/*******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.apps.data.collection;

import static com.ericsson.oss.apps.utils.PmConstants.MODIFIED_NRECLLDU_SCHEMA;

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.kafka.KafkaProducerRim;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import com.ericsson.oss.apps.utils.Utils;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * The Class CsvLoader. Base PM Loader class for ROPS and Baseline data.
 */
@Slf4j
public abstract class CsvLoader<T> {

    protected final Class<T> clazz;
    protected final JpaRepository<T, ?> repository;
    protected final BdrClient bdrClient;
    protected final BdrConfiguration bdrConfiguration;
    protected final FileTracker fileTracker;
    protected final AppDataConfig appDataConfig;
    private final List<GenericRecord> producerRecordList = new ArrayList<>();
    private final KafkaProducerRim kafkaProducer;
    private Optional<Schema> schemaOpt = Optional.empty();

    public CsvLoader(Class<T> clazz, BdrClient bdrClient, BdrConfiguration bdrConfiguration, FileTracker fileTracker, KafkaProducerRim kafkaProducer, AppDataConfig appDataConfig) {
        this.clazz = clazz;
        this.bdrClient = bdrClient;
        this.bdrConfiguration = bdrConfiguration;
        this.fileTracker = fileTracker;
        this.repository = null;
        this.kafkaProducer = kafkaProducer;
        this.appDataConfig = appDataConfig;
    }

    public CsvLoader(Class<T> clazz, JpaRepository<T, ?> repository, BdrClient bdrClient, BdrConfiguration bdrConfiguration, FileTracker fileTracker) {
        this.clazz = clazz;
        this.bdrClient = bdrClient;
        this.bdrConfiguration = bdrConfiguration;
        this.fileTracker = fileTracker;
        this.repository = repository;
        this.kafkaProducer = null;
        this.appDataConfig = null;
    }

    public CsvLoader(Class<T> clazz, JpaRepository<T, ?> repository, BdrClient bdrClient, BdrConfiguration bdrConfiguration, FileTracker fileTracker, AppDataConfig appDataConfig) {
        this.clazz = clazz;
        this.bdrClient = bdrClient;
        this.bdrConfiguration = bdrConfiguration;
        this.fileTracker = fileTracker;
        this.repository = repository;
        this.kafkaProducer = null;
        this.appDataConfig = appDataConfig;
    }

    protected long loadCsvData(String objectPath) {
        return loadCsvData(objectPath, Optional::of);
    }

    protected long loadCsvData(String objectPath, Function<T, Optional<T>> entityFinalizer) {
        log.info("Loading CSV data for path {}", objectPath);
        try (GZIPInputStream inputStream = new GZIPInputStream(bdrClient.getObjectInputStream(bdrConfiguration.getBucket(), objectPath));
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        ) {
            final long numRecordsProcessed = readFromCsv(reader, entityFinalizer);
            log.info("Read {} records from {}", numRecordsProcessed, objectPath);
            if (kafkaProducer != null && clazz.getName().contains("PmRopNRCellDU")) {
                log.info("Sending {} PmRopNRCellDU records to kafka", producerRecordList.size());
                kafkaProducer.sendKafkaGenericRecord(producerRecordList);
            }
            return numRecordsProcessed;
        } catch (SdkClientException sdkClientException) {
            log.info("Error connecting to object store, cannot load '{}'", objectPath, sdkClientException);
        } catch (IOException ioException) {
            log.info("Error reading CSV data, cannot load '{}'", objectPath, ioException);
        } catch (RuntimeException runtimeException) {
            log.info("Error parsing CSV data, cannot load '{}'", objectPath, runtimeException);
        }
        // problem loading file, remove entry from etag map. Force 'fresh' load next time, regardless of etag.
        fileTracker.removeEtagfromMap(objectPath);
        return 0;
    }

    /**
     * Read records from CSV.
     *
     * @param reader          the reader of the CSV file.
     * @param entityFinalizer the finalizer which gives option to modify the readied entity.
     * @return the length of the loaded records.
     */
    private long readFromCsv(final Reader reader, Function<T, Optional<T>> entityFinalizer) {
        long numRecords = 0;
        producerRecordList.clear();

        CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                .withType(clazz)
                .withIgnoreLeadingWhiteSpace(true)
                .withThrowExceptions(false)
                .build();
        if (kafkaProducer != null) {
            schemaOpt = Utils.of().getSchema(MODIFIED_NRECLLDU_SCHEMA);
        }
        for (var entity : csvToBean) {
            if (loadEntity(entity, entityFinalizer)) {
                numRecords++;
            } else {
                log.debug("Error Processing Parameters in Record # {}: {}", numRecords, entity);
            }
        }
        csvToBean.getCapturedExceptions().stream()
                .map(Throwable::getMessage).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .forEach((errorMessage, count) -> log.error("Error while parsing CSV data '{}' times {}", errorMessage, count));
        return numRecords;
    }

    /**
     * Load single record.
     *
     * @param entity          the record entity.
     * @param entityFinalizer the finalizer which gives option to modify the readied entity.
     * @return boolean representing that the loading was successful or not.
     */
    private boolean loadEntity(T entity, Function<T, Optional<T>> entityFinalizer) {
        try {
            var acceptedEntity = entityFinalizer.apply(entity);
            log.trace("Loading {}: {}", clazz, entity);
            if (acceptedEntity.isPresent() && acceptedEntity.get() instanceof PmRopNRCellDU) {
                PmRopNRCellDU pmRopNRCellDU = (PmRopNRCellDU) acceptedEntity.get();
                // PM Counters processed thru Kafka, will have avgDeltaIpN & avgUeUlTp set on consumer side.
                if (kafkaProducer != null) {
                    log.trace("Processing ROP (Kafka) with MoId {} ", pmRopNRCellDU.getMoRopId());
                    schemaOpt.stream()
                        .flatMap(schema -> kafkaProducer.createProducerMessagesGenericRecord(pmRopNRCellDU, schema).stream())
                        .forEach(producerRecordList::add);
                    return true;
                }
                // Must set avgDeltaIpN & avgUeUlTp here before saving to repo.
                if (appDataConfig.isUsePreCalculatedAvgDeltaIpN()) {
                    pmRopNRCellDU.setAvgDeltaIpN(pmRopNRCellDU.getAvgDeltaIpNPreCalculated());
                    pmRopNRCellDU.setUsePreCalculatedInCsv(true);
                }
                log.trace("Processing ROP (CSV) with MoId {}, ", pmRopNRCellDU.getMoRopId());
                ((PmRopNrCellDuRepo) repository).save(pmRopNRCellDU);
                return true;
            }

            acceptedEntity.ifPresent(repository::save);
            return true;
        } catch (final Exception exception) {
            log.error("Error occurred during loading", exception);
        }
        return false;
    }
}
