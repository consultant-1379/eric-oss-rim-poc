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
package com.ericsson.oss.apps.data.collection.pmrop;

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.data.collection.CsvLoader;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.AppDataConfig;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRop;
import com.ericsson.oss.apps.kafka.KafkaProducerRim;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * The Generic Class PmRopLoader.
 */
@Slf4j
public class PmRopLoader<T extends PmRop> extends CsvLoader<T> {

    private static final int PREFIX_LENGTH = "PmRop".length();

    private final Counter numPmRopRecordsReceived;

    public PmRopLoader(
            Class<T> clazz,
            JpaRepository<T, MoRopId> repository,
            BdrClient bdrClient,
            BdrConfiguration bdrConfiguration,
            Counter numPmRopRecordsReceived,
            FileTracker fileTracker,
            AppDataConfig appDataConfig
    ) {
        super(clazz, repository, bdrClient, bdrConfiguration, fileTracker, appDataConfig);
        this.numPmRopRecordsReceived = numPmRopRecordsReceived;
    }

    public PmRopLoader(
            Class<T> clazz,
            KafkaProducerRim kafkaProducerRim,
            BdrClient bdrClient,
            BdrConfiguration bdrConfiguration,
            Counter numPmRopRecordsReceived,
            FileTracker fileTracker,
            AppDataConfig appDataConfig
    ) {
        super(clazz, bdrClient, bdrConfiguration, fileTracker, kafkaProducerRim, appDataConfig);
        this.numPmRopRecordsReceived = numPmRopRecordsReceived;
    }

    /**
     * Load pm rop.
     *
     * @param ropTimestamp the rop timestamp
     * @param customerId   the customer id
     */
    @Timed
    public void loadPmRop(long ropTimestamp, String customerId) {
        String objectPath = getObjectPath(ropTimestamp, customerId);
        log.info("Processing ROP with CustomerId '{}' and TimeStamp '{}' from object path '{}' ", customerId, ropTimestamp, objectPath);
        final long numRecordsProcessed = loadCsvData(objectPath, pmRopData -> {
            pmRopData.getMoRopId().setRopTime(ropTimestamp);
            return Optional.of(pmRopData);
        });
        numPmRopRecordsReceived.increment(numRecordsProcessed);
    }

    String getObjectPath(long ropTimestamp, String customerId) {
        String ropPath = appDataConfig.getAppDataPmRopPath().endsWith("/")
            ? appDataConfig.getAppDataPmRopPath().substring(0, appDataConfig.getAppDataPmRopPath().length() - 1)
            : appDataConfig.getAppDataPmRopPath();

        return String.format("%s/pm-%s-%s-%d.csv.gz", ropPath, clazz.getSimpleName().substring(PREFIX_LENGTH), customerId, ropTimestamp);
    }
}
