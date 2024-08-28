/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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
package com.ericsson.oss.apps.kafka;

import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;
import com.ericsson.oss.apps.data.collection.AppDataConfig;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import com.ericsson.oss.apps.utils.PmRopNrCellDuCreator;
import com.ericsson.oss.apps.utils.TimeConverter;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.micrometer.core.instrument.Counter;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.ericsson.oss.apps.utils.PmConstants.*;

/**
 * The Class CounterParserTopicListenerRim.
 * Listens on the counter parser topic for Consumer Records in Avro Format and deserializes these.
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(prefix = "spring.kafka.mode", name = "enabled", havingValue = "true")
public class CounterParserTopicListenerRim {

    private static final String NRCELLDU_GNBDU = "NRCellDU_GNBDU";
    private static Pattern patternValidateFdn = Pattern.compile("SubNetwork.+MeContext.+ManagedElement.+GNBDUFunction.+NRCellDU.+");

    @Value("${spring.kafka.topics.input.name}")
    private String counterParserTopicName;

    @Value("${spring.kafka.consumer.discard-rops-older-than-minutes}")
    private long discardRopsOlderThanMinutes;
    private long discardRopsOlderThanMs;

    private final Counter kafkaNumberBatchesReceived;
    private final Counter kafkaNumberRecordsReceived;
    private final Counter kafkaNumberRecordsProcessed;
    private final Counter kafkaNumberRecordsDropped;
    private final Counter kafkaNumberRecordsInvalidMoType;
    private final PmRopNrCellDuRepo pmRopNrCellDuRepo;
    private final AppDataConfig appDataConfig;
    private final KafkaAvroDeserializer kad;

    /**
     * Listener for the {@link CounterParserTopicListenerRim#counterParserTopicName} topic.
     *
     * @param consumerRecords the consumer records
     */
    @KafkaListener(id = "${spring.kafka.listenerId}", idIsGroup = false, containerFactory = "concurrentKafkaListenerContainerFactory", topics = "${spring.kafka.topics.input.name}", concurrency = "${spring.kafka.consumer.concurrency:1}", autoStartup = "${spring.kafka.auto.start:false}")
    public void listen(final List<ConsumerRecord<String, byte[]>> consumerRecords) {
        // Local metrics to track exceptions thrown and DROP the batch if exception thrown rejected.
        MetricValues mv = new MetricValues();

        try {
            discardRopsOlderThanMs = discardRopsOlderThanMinutes * 60 * 1000;
            mv.numRecordsReceived.getAndSet(consumerRecords.size());
            kafkaNumberRecordsReceived.increment(consumerRecords.size());
            kafkaNumberBatchesReceived.increment();
            List<ConsumerRecord<String, byte[]>> validConsumerRecords = getConsumerRecordsWithValidHeader(consumerRecords, mv);

            if (log.isTraceEnabled()) {
                logStats(consumerRecords.size(), validConsumerRecords.size());
            }
            // Any exceptions thrown in following lambda, will mean pmRopNRCellDUList is empty and so no records processed.
            // So exceptions thrown and not caught, will mean ALL RECORD in batch are rejected.
            List<PmRopNRCellDU> pmRopNRCellDUList = validConsumerRecords.stream()
                    .flatMap(nrCellDuPmCtrRecord -> processPmAvroRecord(nrCellDuPmCtrRecord, mv).stream())
                    .collect(Collectors.toList());
            acceptBatch(mv, pmRopNRCellDUList);
        } catch (final Exception exception) {
            rejectEntireBatch(mv, exception);
        }
    }

    /**
     * Gets the logs.
     */
    public void getLogs() {
        log.info(
                "Counter Listener: KAFKA_METRICS: Total of {}/{}/{} ({}) consumerRecords (Received/Processed/Dropped(of which have 'invalidMoType')) and {} Batches Received from topic: {}", kafkaNumberRecordsReceived
                        .count(), kafkaNumberRecordsProcessed.count(), kafkaNumberRecordsDropped
                        .count(), kafkaNumberRecordsInvalidMoType.count(),
                kafkaNumberBatchesReceived.count(), counterParserTopicName);
    }

    /**
     * Process the validated ConsumerRecord Record with NRCELLDU PM Counters.
     *
     * @param consumerRecord Consumer record containing NrCellDu PM Counter Record.
     */
    Optional<PmRopNRCellDU> processPmAvroRecord(ConsumerRecord<String, byte[]> consumerRecord, MetricValues mv) {
        Optional<String> optNodeFdn = getNodeFdnFromHeader(consumerRecord.headers());
        try {
            log.trace("Kafka Avro Deserialization of consumerRecord with fdn : {}", getNodeFdnFromHeader(consumerRecord.headers()));
            NRCellDU_GNBDU_1 nrCellDuPmCtrRecord = (NRCellDU_GNBDU_1) kad.deserialize(SCHEMA_SUBJECT_RIM + MODIFIED_NRECLLDU_SCHEMA, consumerRecord.value());
            if (nrCellDuPmCtrRecord == null) {
                log.error("Counter Listener: Dropping record: Kafka Avro Deserialization Failed, Cannot deserialize consumerRecord with fdn : {}", optNodeFdn);
                mv.getNumRecordsDropped().getAndIncrement();
                return Optional.empty();
            }
            showTraceLogInfo(consumerRecord, nrCellDuPmCtrRecord);
            if (!isValidFdn(consumerRecord, nrCellDuPmCtrRecord)) {
                mv.getNumRecordsDropped().getAndIncrement();
                return Optional.empty();
            }

            Optional<Long> ropEndTimeEpochOpt = TimeConverter.of().convertTimestampToEpoch(nrCellDuPmCtrRecord, discardRopsOlderThanMs);

            if (ropEndTimeEpochOpt.isEmpty()) {
                log.error("Counter Listener: Dropping record, Record with fdn {} has invalid 'ropEndTime'", nrCellDuPmCtrRecord.getMoFdn()
                        .toString());
                mv.numRecordsDropped.getAndIncrement();
                return Optional.empty();
            }

            if (nrCellDuPmCtrRecord.getSuspect()) {
                mv.numRecordsDropped.getAndIncrement();
                log.error("Counter Listener: Dropping record, Record with fdn {} has 'suspect' flag set", nrCellDuPmCtrRecord.getMoFdn());
                return Optional.empty();
            }

            PmRopNRCellDU pmRopNRCellDU = PmRopNrCellDuCreator.of()
                    .createNewPmRopNRCellDU(nrCellDuPmCtrRecord, ropEndTimeEpochOpt.get(), appDataConfig.isUsePreCalculatedAvgDeltaIpN());
            log.trace("Counter Listener: pmRopNRCellDU = {} ", pmRopNRCellDU);
            return Optional.of(pmRopNRCellDU);
        } catch (final Exception exception) {
            rejectRecord(optNodeFdn.toString(), mv, exception);
            return Optional.empty();
        }
    }

    public static void setPatternValidateFdn(Pattern newaPtternValidateFdn) {
        patternValidateFdn = newaPtternValidateFdn;
    }

    private synchronized void acceptBatch(MetricValues mv, List<PmRopNRCellDU> pmRopNRCellDUList) {
        logStatsAtEnd(mv);
        pmRopNrCellDuRepo.saveAll(pmRopNRCellDUList);
        mv.numRecordsProcessed.getAndSet(pmRopNRCellDUList.size());
        kafkaNumberRecordsDropped.increment(mv.numRecordsDropped.get());
        kafkaNumberRecordsProcessed.increment(mv.numRecordsProcessed.get());
        kafkaNumberRecordsInvalidMoType.increment(mv.numRecordsInvalidMoType.get());
    }


    private synchronized void rejectEntireBatch(MetricValues mv, final Exception exception) {
        logStatsAtEnd(mv);
        kafkaNumberRecordsDropped.increment(mv.numRecordsReceived.get());
        kafkaNumberRecordsInvalidMoType.increment(mv.numRecordsInvalidMoType.get());
        log.error("Counter Listener: Processing error: Failure Deserializing Record: Dropping ALL {} records in batch; {}",
                mv.numRecordsReceived.get(), exception.getMessage(), exception);
    }

    private synchronized String getPmCounterAvroInfo(final ConsumerRecord<String, byte[]> consumerRecord, NRCellDU_GNBDU_1 nrCellDuPmCtr) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("Counter Listener: -----------------------------------").append("\n");
        Headers headers = consumerRecord.headers();
        if (nrCellDuPmCtr == null || headers == null) {
            sb.append("Counter Listener: Invalid Consumer record format; null header or Record found; consumerRecord =  ")
                    .append(consumerRecord)
                    .append("\n");
            sb.append("-----------------------------------").append("\n").append("\n");
            return sb.toString();
        }
        sb.append("Counter Listener: Processing Schema:").append(nrCellDuPmCtr.getSchema().getName()).append("\n");

        headers.spliterator()
                .forEachRemaining(header -> sb.append("Header- ")
                        .append(header.key())
                        .append(":")
                        .append(new String(header.value(), StandardCharsets.UTF_8))
                        .append("\n"));

        nrCellDuPmCtr.getSchema()
                .getFields()
                .forEach(field -> sb.append("PmCounter- ").append(field.name()).append(":").append(nrCellDuPmCtr.get(field.name())).append("\n"));
        sb.append("Counter Listener: -----------------------------------").append("\n");
        return sb.toString();
    }

    private synchronized void logStats(int numberConsumerRecords, int numberValidNrCellDuConsumerRecords) {
        log.trace("Counter Listener: ALL BATCHES: Received {} consumerRecords and {} Batches from topic: {}", kafkaNumberRecordsReceived.count(),
                kafkaNumberBatchesReceived.count(), counterParserTopicName);
        log.trace("Counter Listener: THIS BATCH : Received {}/{} (Total/Valid) consumerRecords from topic '{}' on consumer thread '{}' ",
                numberConsumerRecords, numberValidNrCellDuConsumerRecords, counterParserTopicName, Thread.currentThread().getName());
    }

    private void rejectRecord(String fdn, MetricValues mv, final Exception exception) {
        mv.numRecordsDropped.getAndIncrement();
        log.error("Counter Listener: Processing error: Failure Deserializing Record: Unknown Reason, Dropping record with fdn {} from batch batch; Exception", fdn, exception);
    }

    private void logStatsAtEnd(MetricValues mv) {
        if (log.isTraceEnabled()) {
            String stats = String.format("%d/%d/%d(%d)", mv.numRecordsReceived.get(), mv.numRecordsProcessed.get(), mv.numRecordsDropped.get(), mv.numRecordsInvalidMoType.get());
            log.trace("Counter Listener: All records consumed (THIS BATCH: {} #received/processed/dropped (of which have 'invalidMoType') on this batch for consumer '{}' \n", stats,
                    Thread.currentThread().getName());
        }
    }

    private List<ConsumerRecord<String, byte[]>> getConsumerRecordsWithValidHeader(List<ConsumerRecord<String, byte[]>> consumerRecords,
                                                                                   MetricValues mv) {
        return consumerRecords.stream()
                .filter(consumerRecord -> isValidNrCellDURecordHeader(consumerRecord, mv))
                .collect(Collectors.toUnmodifiableList());
    }

    private boolean isValidNrCellDURecordHeader(ConsumerRecord<String, byte[]> consumerRecord, MetricValues mv) {
        String fdnValue = "noValue";
        try {
            if (consumerRecord == null) {
                log.error("Counter Listener: Dropping record, Consumer Record is null.");
                mv.numRecordsDropped.getAndIncrement();
                return false;
            }
            if (consumerRecord.value() == null) {
                log.error("Counter Listener: Dropping record, Consumer Record 'NRCellDu PM Counter Record' is null.");
                mv.numRecordsDropped.getAndIncrement();
                return false;
            }
            if (!consumerRecord.headers().iterator().hasNext()) {
                log.error("Counter Listener: Dropping record, Consumer Record 'header' is empty.");
                mv.numRecordsDropped.getAndIncrement();
                return false;
            }
            Headers headers = consumerRecord.headers();
            Optional<Header> moTypeHeader = getHeaderElement(headers, MO_TYPE);
            if (moTypeHeader.isEmpty()) {
                log.error("Counter Listener: Dropping record, Either no or multiple moType found in Consumer Record Header, expected one only");
                mv.numRecordsDropped.getAndIncrement();
                return false;
            }
            String moTypeKey = moTypeHeader.get().key();
            String moTypeValue = new String(moTypeHeader.get().value(), StandardCharsets.UTF_8);
            if (!moTypeValue.equals(NRCELLDU_GNBDU)) {
                //Filter all non NRCELLDU, there will be a lot of them.
                log.trace("Counter Listener: Dropping record, Invalid MoType found in Consumer Record. expected '{}', Found '{}'",
                        NRCELLDU_GNBDU, moTypeValue);
                mv.numRecordsInvalidMoType.getAndIncrement();
                mv.numRecordsDropped.getAndIncrement();
                return false;
            }

            Optional<Header> fdnHeader = getHeaderElement(headers, NODE_FDN);
            if (fdnHeader.isEmpty()) {
                log.error("Counter Listener: Dropping record, Either no or multiple nodeFDN found in Consumer Record Header, expected one only");
                mv.numRecordsDropped.getAndIncrement();
                return false;
            }

            String fdnKey = fdnHeader.get().key();
            fdnValue = new String(fdnHeader.get().value(), StandardCharsets.UTF_8);
            if (fdnValue.isEmpty()) {
                log.error("Counter Listener: Dropping record, Invalid nodeFDN found in Consumer Record. nodeFDN is empty");
                mv.numRecordsDropped.getAndIncrement();
                return false;
            }

            log.trace("Counter Listener: Consuming record Header with {} : {} and  {} : {}", fdnKey, fdnValue, moTypeKey, moTypeValue);
            return true;
        } catch (final Exception exception) {
            rejectRecord(fdnValue, mv, exception);
            return false;
        }

    }

    //TODO:Simplify this fdn validation once parsers updated with valid full fdn format
    private boolean isValidFdn(ConsumerRecord<String, byte[]> consumerRecord, NRCellDU_GNBDU_1 nrCellDUpmCtrs) {
        Optional<String> optNodeFdnInHeader = getNodeFdnFromHeader(consumerRecord.headers());
        try {
            String nodeFdnInRecord = nrCellDUpmCtrs.getNodeFDN().toString();
            String moFdn = nrCellDUpmCtrs.getMoFdn().toString();
            if (optNodeFdnInHeader.map(nodeFdnInHeader -> nodeFdnInHeader.equals(nodeFdnInRecord)).orElse(false)) {
                String nodeFdnInHeader = optNodeFdnInHeader.orElse(Optional.empty().toString());
                if (patternValidateFdn.matcher(nodeFdnInHeader).matches()) {
                    log.trace("Processing Record: Validated fdn ( w/o moFdn):  nodeFdn = {} ", nodeFdnInHeader);
                    return true;
                }
                FdnHandler fdnHandler = new FdnHandler(nodeFdnInHeader, moFdn);
                if (fdnHandler.isValid()) {
                    log.trace("Processing Record: Validated fdn (using FdnHandler):  fdnHandler = {} ", fdnHandler);
                    nrCellDUpmCtrs.setNodeFDN(fdnHandler.getFullFdn());
                    consumerRecord.headers().remove(NODE_FDN);
                    consumerRecord.headers().add(new RecordHeader(NODE_FDN, fdnHandler.getFullFdn().getBytes(StandardCharsets.UTF_8)));
                    return true;
                }
                log.error("FdnHandler: Cannot Validate fdn (using FdnHandler): fdnHandler : {}", fdnHandler);
            }
            log.trace("Processing error: DROPPING RECORD: Cannot construct 'fullFdn'; Invalid or mis matching information in nodeFdn or moFdn : "
                    + "\n moFdn = '{}', \n nodeFDN in header = '{}', \n nodeFdn in Record = '{}', \n nrCellDUpmCtrs = {}", moFdn, optNodeFdnInHeader, nodeFdnInRecord, nrCellDUpmCtrs);
            log.error("Processing error: DROPPING RECORD: Cannot  validate 'nodeFdn' and/or 'moFdn', cannot construct 'full' fdn. moFdn = '{}', nodeFdnInHeader = '{}', nodeFdnInRecord = '{}'", moFdn, optNodeFdnInHeader, nodeFdnInRecord);
        } catch (final Exception exception) {
            log.error("Processing error: DROPPING RECORD: Failure Deserializing Record: Cannot validate 'nodeFdn' and/or 'moFdn', cannot construct 'full' fdn, Dropping record with fdn '{}';", optNodeFdnInHeader, exception);
        }
        return false;
    }

    private Optional<Header> getHeaderElement(Headers headers, String headerKey) {
        Iterable<Header> header = headers.headers(headerKey);
        long numberOfElements = StreamSupport.stream(header.spliterator(), false).count();
        if (numberOfElements != 1) {
            log.error("Counter Listener: Invalid Consumer Record Header, Invalid Number of '{}' found in header, expected one, got {}, Header = {}",
                    headerKey, numberOfElements, headers);
            return Optional.empty();
        }

        return Optional.of(header.iterator().next());
    }

    private void showTraceLogInfo(final ConsumerRecord<String, byte[]> consumerRecord, NRCellDU_GNBDU_1 nrCellDUpmCtr) {
        if (log.isTraceEnabled()) {
            log.trace("Counter Listener: Consuming record received from topic {} : {}", counterParserTopicName, consumerRecord);
            log.trace("Counter Listener: {}", getPmCounterAvroInfo(consumerRecord, nrCellDUpmCtr));
        }
    }

    private Optional<String> getNodeFdnFromHeader(Headers headers) {
        Optional<Header> fdnHeader = getHeaderElement(headers, NODE_FDN);
        return fdnHeader.map(x -> new String(x.value(), StandardCharsets.UTF_8));
    }

    @ToString
    @Getter
    @NoArgsConstructor
    static class MetricValues {
        private final AtomicInteger numRecordsReceived = new AtomicInteger(0);
        private final AtomicInteger numRecordsProcessed = new AtomicInteger(0);
        private final AtomicInteger numRecordsDropped = new AtomicInteger(0);
        private final AtomicInteger numRecordsInvalidMoType = new AtomicInteger(0);

    }

    @ToString
    @Getter
    private static class FdnHandler {
        private final String nodeFdn;
        private final String moFdn;
        private String managedElement;
        private String fullFdn;
        private final boolean isValid;

        public FdnHandler(String nodeFdn, String moFdn) {
            this.nodeFdn = nodeFdn;
            this.moFdn = moFdn;
            this.isValid = validate();
        }

        private boolean validate() {
            if (moFdn == null || nodeFdn == null || moFdn.equals("noValue") || !moFdn.contains("ManagedElement")
                    || !nodeFdn.contains("ManagedElement") || !moFdn.contains(",") || !nodeFdn.contains(",")) {
                return false;
            }
            String meMoFdn = getMeMoFdnManagedElement(moFdn);
            String meNodeFdn = getManagedElement(nodeFdn);
            if (!meMoFdn.equals(meNodeFdn)) {
                managedElement = "ManagedElement in moFdn = " + meMoFdn + "|| ManagedElement in nodeFdn = " + meNodeFdn;
                return false;
            }
            managedElement = meNodeFdn;
            if (!moFdn.startsWith(managedElement)) {
                return false;
            }
            if (!nodeFdn.endsWith(managedElement)) {
                return false;
            }
            fullFdn = nodeFdn + moFdn.substring(meMoFdn.length());
            if (fullFdn.contains("ENodeBFunction=1")) {
                fullFdn = fullFdn.replaceAll("ENodeBFunction=1", "GNBDUFunction=1");
            }
            return patternValidateFdn.matcher(fullFdn).matches();
        }

        private String getMeMoFdnManagedElement(String s) {
            int start = s.indexOf("ManagedElement");
            return s.substring(start, moFdn.indexOf(',', start));
        }

        private String getManagedElement(String s) {
            int start = s.indexOf("ManagedElement");
            return s.substring(start);
        }
    }
}
