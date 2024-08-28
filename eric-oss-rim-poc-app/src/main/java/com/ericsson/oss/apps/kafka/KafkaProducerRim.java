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

import static com.ericsson.oss.apps.utils.PmConstants.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.concurrent.ListenableFuture;

import com.ericsson.oss.apps.data.collection.pmrop.counters.MoPmCounter;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.utils.TimeConverter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class KafkaProducerRim.
 * Receives the PM ROP data and uses the Kafka producer to send the data in avro format as Generic Record to the counter parser topic.
 */
@Slf4j
@Getter
@Component
public class KafkaProducerRim {

    @Value("${spring.kafka.topics.input.name}")
    private String counterParserTopicName;
    @Value("${spring.kafka.consumer.group-id}")
    protected String group;

    @Autowired
    KafkaTemplate<String, GenericRecord> kafkaOutputTemplate;

    @Value(value = "${spring.kafka.topics.output.partitions}")
    private int partitions;

    private final AtomicInteger numberRecordsSent = new AtomicInteger();

    /**
     * Creates the producer messages in generic record format.
     *
     * @param pmRopNRCellDU
     *     the PM ROP NRCellDU object with the counter information read from CSV file.
     * @param nrCellDuSchema
     *     the NRCELLDU schema
     *
     * @return the optional generic record built from the inputted counter information.
     */
    public Optional<GenericRecord> createProducerMessagesGenericRecord(PmRopNRCellDU pmRopNRCellDU, Schema nrCellDuSchema) {
        Map<String, MoPmCounter> mOPMCounterMap = new HashMap<>();

        mOPMCounterMap.put("pmMacRBSymUsedPdschTypeA",getMoPMCounter("pmMacRBSymUsedPdschTypeA", COUNTER_TYPE_SINGLE, pmRopNRCellDU.getPmMacRBSymUsedPdschTypeA()));
        mOPMCounterMap.put("pmMacRBSymUsedPdcchTypeA",getMoPMCounter("pmMacRBSymUsedPdcchTypeA", COUNTER_TYPE_SINGLE, pmRopNRCellDU.getPmMacRBSymUsedPdcchTypeA()));
        mOPMCounterMap.put("pmMacRBSymUsedPdcchTypeB",getMoPMCounter("pmMacRBSymUsedPdcchTypeB", COUNTER_TYPE_SINGLE, pmRopNRCellDU.getPmMacRBSymUsedPdcchTypeB()));
        mOPMCounterMap.put("pmMacRBSymUsedPdschTypeABroadcasting",getMoPMCounter("pmMacRBSymUsedPdschTypeABroadcasting", COUNTER_TYPE_SINGLE, pmRopNRCellDU.getPmMacRBSymUsedPdschTypeABroadcasting()));
        mOPMCounterMap.put("pmMacRBSymCsiRs", getMoPMCounter("pmMacRBSymCsiRs", COUNTER_TYPE_SINGLE, pmRopNRCellDU.getPmMacRBSymCsiRs()));
        mOPMCounterMap.put("pmMacRBSymAvailDl", getMoPMCounter("pmMacRBSymAvailDl", COUNTER_TYPE_SINGLE, pmRopNRCellDU.getPmMacRBSymAvailDl()));

        //pmMacVolUl defined as uint64; https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html
        //The Avro Schema for NRCELLDU used by the parsers has these defined as 'long'
        //The PmRopNRCellDU have the following counters defined as DOUBLE ???
        //So all Double values read in from a CSV file will be ROUNDED to LONG
        mOPMCounterMap.put("pmMacVolUl", getMoPMCounter("pmMacVolUl", COUNTER_TYPE_SINGLE, (Math.round(pmRopNRCellDU.getPmMacVolUl()))));
        mOPMCounterMap.put("pmMacVolDl", getMoPMCounter("pmMacVolDl", COUNTER_TYPE_SINGLE, (Math.round(pmRopNRCellDU.getPmMacVolDl()))));
        mOPMCounterMap.put("pmMacVolUlResUe",getMoPMCounter("pmMacVolUlResUe", COUNTER_TYPE_SINGLE, (Math.round(pmRopNRCellDU.getPmMacVolUlResUe()))));
        mOPMCounterMap.put("pmMacTimeUlResUe",getMoPMCounter("pmMacTimeUlResUe", COUNTER_TYPE_SINGLE, (Math.round(pmRopNRCellDU.getPmMacTimeUlResUe()))));

        mOPMCounterMap.put("pmRadioMaxDeltaIpNDistr", getMoPMCounters("pmRadioMaxDeltaIpNDistr", COUNTER_TYPE_PDF, isValidLongArray(pmRopNRCellDU.getPmRadioMaxDeltaIpNDistr()) ? pmRopNRCellDU.getPmRadioMaxDeltaIpNDistr() : new ArrayList<>()));
        mOPMCounterMap.put("pmRadioSymbolDeltaIpnDistr", getMoPMCounters("pmRadioSymbolDeltaIpnDistr", COUNTER_TYPE_PDF, isValidLongArray(pmRopNRCellDU.getPmRadioSymbolDeltaIpnDistr()) ? pmRopNRCellDU.getPmRadioSymbolDeltaIpnDistr() : new ArrayList<>()));
        mOPMCounterMap.put("avgDeltaIpNPreCalculated",getMoPMCounter("avgDeltaIpNPreCalculated", COUNTER_TYPE_SINGLE, kafkaDoubleToLong(pmRopNRCellDU.getAvgDeltaIpNPreCalculated())));

        try {
            GenericRecord pmCountersRecord = generatePmCounters(nrCellDuSchema, mOPMCounterMap ).orElseThrow();

            GenericRecord pmCtrsGenericRecords = new GenericRecordBuilder(nrCellDuSchema)
                .set(NODE_FDN, pmRopNRCellDU.getMoRopId().getFdn())
                .set(ELEMENT_TYPE, PmRopNrCellDuConstantValues.ELEMENT_TYPE)
                .set(MO_FDN, PmRopNrCellDuConstantValues.MO_FDN)
                .set(ROP_BEGIN_TIME, TimeConverter.of().convertEpochToTimestamp(pmRopNRCellDU.getMoRopId().getRopTime() - ROP_PERIOD_MS))
                .set(ROP_END_TIME, TimeConverter.of().convertEpochToTimestamp(pmRopNRCellDU.getMoRopId().getRopTime()))
                .set(SUSPECT, PmRopNrCellDuConstantValues.SUSPECT)
                .set(PM_COUNTERS, pmCountersRecord)
                .build();

            printValuesFromGenericRecord(pmCtrsGenericRecords);

            return Optional.of(pmCtrsGenericRecords);
        } catch (Exception e) {
            log.error("KafkaProducerRim: ERROR creating Producer Messages for NRCELLDU Generic Record, {} :", e.getMessage(), e);
            return Optional.empty();
        }

    }

    /**
     * Send generic record to kafka producder.
     *
     * @param pmCtrsGenericRecordList
     *     the pm ctrs generic record list
     */
    public void sendKafkaGenericRecord(List<GenericRecord> pmCtrsGenericRecordList) {
        pmCtrsGenericRecordList.stream()
            .flatMap(pmCtrsGenericRecord -> getKafkaProducerMessage(pmCtrsGenericRecord).stream())
            .forEach(this::doSend);
    }

    /**
     * Send a list of messages (each containing a generic record) to the kafa producer.
     *
     * @param pmCtrsMessageList
     *     list of messages (each containing a generic record)
     */
    public void sendKafkaMessage(List<Message<GenericRecord>> pmCtrsMessageList) {
        pmCtrsMessageList.forEach(this::doSend);
    }

    /**
     * Send a message (containing a generic record) to the kafa producer.
     *
     * @param message
     *     message (containing a generic record)
     */
    public void doSend(Message<GenericRecord> message) {
        final ListenableFuture<SendResult<String, GenericRecord>> lf = kafkaOutputTemplate.send(message);
        checkSendSuccessful(lf, message);
    }

    /**
     * Gets the kafka message. Add the kafka Headers to the input Generic Record.
     *
     * @param pmCtrsGenericRecord
     *     the pm counter in a generic record
     *
     * @return Optional of the kafka message
     */
    public Optional<Message<GenericRecord>> getKafkaProducerMessage(GenericRecord pmCtrsGenericRecord) {
        String nodeName = pmCtrsGenericRecord.get(NODE_FDN).toString();
        int partition = calculatePartition(nodeName);
        String schemaName = pmCtrsGenericRecord.getSchema().getName();
        String schemaNameWithoutVersion = schemaName.substring(0, schemaName.lastIndexOf('_'));
        int schemaIdValue = PmRopNrCellDuConstantValues.SCHEMA_ID;
        final Message<GenericRecord> message = MessageBuilder.withPayload(pmCtrsGenericRecord)
            .setHeader(KafkaHeaders.MESSAGE_KEY, nodeName)
            .setHeader(KafkaHeaders.TOPIC, counterParserTopicName)
            .setHeader(KafkaHeaders.PARTITION_ID, partition)
            .setHeader(NODE_FDN, pmCtrsGenericRecord.get(NODE_FDN))
            .setHeader(ELEMENT_TYPE, pmCtrsGenericRecord.get(ELEMENT_TYPE))
            .setHeader(MO_TYPE, schemaNameWithoutVersion)
            .setHeader(SCHEMA_ID, schemaIdValue)
            .build();
        return Optional.of(message);
    }

    /**
     * Converts Spring kafka message header into Kafka Producer Avro record headers
     *
     * @param messageHeaders
     *     the Spring kafka message headers
     *
     * @return the Avro record headers
     */
    public Headers getRecordHeaders(MessageHeaders messageHeaders) {
        Headers headers = new RecordHeaders();
        headers.add(new RecordHeader(KafkaHeaders.MESSAGE_KEY, getMessageHeader(messageHeaders, KafkaHeaders.MESSAGE_KEY)));
        headers.add(new RecordHeader(KafkaHeaders.TOPIC, getMessageHeader(messageHeaders, KafkaHeaders.TOPIC)));
        headers.add(new RecordHeader(KafkaHeaders.PARTITION_ID, getMessageHeader(messageHeaders, KafkaHeaders.PARTITION_ID)));
        headers.add(new RecordHeader(NODE_FDN, getMessageHeader(messageHeaders, NODE_FDN)));
        headers.add(new RecordHeader(ELEMENT_TYPE, getMessageHeader(messageHeaders, ELEMENT_TYPE)));
        headers.add(new RecordHeader(MO_TYPE, getMessageHeader(messageHeaders, MO_TYPE)));
        headers.add(new RecordHeader(SCHEMA_ID, getMessageHeader(messageHeaders, SCHEMA_ID)));
        return headers;
    }

    private byte[] getMessageHeader(MessageHeaders messageHeaders, String key) {
        if (key == null || key.isBlank() || messageHeaders == null || messageHeaders.get(key) == null) {
            return new byte[] {};
        }
        //Sonar, yeah its already checked above.
        Object obj = messageHeaders.get(key);
        if (obj != null) {
            return obj.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            return new byte[] {};
        }
    }

    private void checkSendSuccessful(final ListenableFuture<SendResult<String, GenericRecord>> lf, Message<GenericRecord> message) {
        if (!lf.isCancelled()) {
            log.trace("KafkaProducerRim: Sent message {} with headers {} to topic {} on partition {}", message.getHeaders().get("nodeFdn"),
                message.getHeaders(),
                message.getHeaders().get(KafkaHeaders.TOPIC), message.getHeaders().get(KafkaHeaders.PARTITION_ID));
            numberRecordsSent.getAndIncrement();
        } else {
            log.error("KafkaProducerRim: Sending messages to topic was cancelled before it was completed");
        }
    }

    private void printValuesFromGenericRecord(GenericRecord pmCtrsGenericRecords) {
        if (!log.isTraceEnabled()) {
            return;
        }
        log.trace("-------------------------------------------------------");
        StringBuilder sb = new StringBuilder();
        pmCtrsGenericRecords.getSchema()
            .getFields()
            .forEach(field -> sb.append("NRCELLDU - ")
                .append(field.name())
                .append(":")
                .append(pmCtrsGenericRecords.get(field.name()))
                .append("\n"));
        log.trace("\n{}", sb.toString());

        log.trace("-------------------------------------------------------");
        sb.setLength(0);

        GenericRecord pmCountersGR = (GenericRecord) pmCtrsGenericRecords.get("pmCounters");
        Field[] pmCountersFields = NR.RAN.PM_COUNTERS.pmCounters.class.getDeclaredFields();
        Arrays.stream(pmCountersFields).filter(f -> f.getName().startsWith("pm")).forEach(f -> buildString(pmCountersGR, f, sb));
        log.trace("\n{}", sb);
    }

    private void buildString(GenericRecord pmCountersGR, Field f, StringBuilder sb) {
        GenericRecord pmGr = (GenericRecord) pmCountersGR.get(f.getName());
        if (pmGr != null) {
            boolean b = (boolean) pmGr.get(IS_VALUE_PRESENT);
            if (b) {
                MoPmCounterWithSchemaType moPmSt = getMoPMCounterWithSchemaTypeFromGenericRecord(pmGr);
                sb.append(String.format("%-50s", moPmSt.getMoPmCounter().getCounterName()))
                    .append(",")
                    .append("Counter Value Type Expected: ")
                    .append(moPmSt.getMoPmCounter().getCounterType())
                    .append(",")
                    .append("Counter Value Type in Schema: ")
                    .append(moPmSt.getCounterValueSchemaType())
                    .append(", Item:")
                    .append(moPmSt.getCounterValueItemType())
                    .append(",")
                    .append(moPmSt.getMoPmCounter().getPmCounterValues())
                    .append("\n");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private MoPmCounterWithSchemaType getMoPMCounterWithSchemaTypeFromGenericRecord(GenericRecord pmGr) {
        Schema pmCounterSubSchema = pmGr.getSchema();
        String pmCounterName = pmCounterSubSchema.getName();
        String pmCounterType = (String) pmGr.get(COUNTER_TYPE);
        Schema.Type counterValueSchemaTypeInInputAvro = pmCounterSubSchema.getField(COUNTER_VALUE).schema().getType();
        List<Long> pmCounterValueList = new ArrayList<>();
        Schema.Type counterValueItemTypeInInputAvro = null;
        if (counterValueSchemaTypeInInputAvro == Schema.Type.LONG) {
            long pmCounterValue = (long) pmGr.get(COUNTER_VALUE);
            pmCounterValueList.add(pmCounterValue);
        } else if (counterValueSchemaTypeInInputAvro == Schema.Type.ARRAY) {
            counterValueItemTypeInInputAvro = pmCounterSubSchema.getField(COUNTER_VALUE).schema().getElementType().getType();
            if (counterValueItemTypeInInputAvro == Schema.Type.LONG) {
                pmCounterValueList.addAll((List<Long>) pmGr.get(COUNTER_VALUE));
            } else {
                log.info("KafkaProducerRim : Error Creating Counter {}, Unknown/Unhandled Type {}, with counter value {}", pmCounterName,
                    pmCounterType, pmGr.get(COUNTER_VALUE));
            }
        } else {
            log.info("KafkaProducerRim : Error Creating Counter {}, Unknown/Unhandled Type {}, with counter value {}", pmCounterName, pmCounterType,
                pmGr.get(COUNTER_VALUE));
        }
        MoPmCounter moPmCtr = MoPmCounter.builder().counterName(pmCounterName).counterType(pmCounterType).pmCounterValues(pmCounterValueList).build();
        return new MoPmCounterWithSchemaType(moPmCtr, counterValueSchemaTypeInInputAvro, counterValueItemTypeInInputAvro);
    }

    private Optional<GenericRecord> generatePmCounters(Schema nrCellDuSchema, Map<String, MoPmCounter> mOPMCounterMap) {
        Schema.Field pmCountersField = nrCellDuSchema.getField(PM_COUNTERS);
        if (!ObjectUtils.isEmpty(pmCountersField)) {
            // pmCounters schema
            Schema pmCountersSchema = pmCountersField.schema();
            GenericRecord pmCountersRecord = new GenericData.Record(pmCountersSchema);

            List<Schema.Field> schemaWithListOfPmCounters = pmCountersSchema.getFields();
            schemaWithListOfPmCounters.forEach(pmCounterField -> generatePmCounter(mOPMCounterMap, pmCountersRecord, pmCounterField));
            return Optional.of(pmCountersRecord);
        }
        return Optional.empty();
    }

    private void generatePmCounter(Map<String, MoPmCounter> mOPMCounterMap, GenericRecord pmCountersRecord,
                                   org.apache.avro.Schema.Field pmCounterField) {
        // inner pmCounter schema
        Schema pmCounterSubSchema = pmCounterField.schema();
        String pmCounterName = pmCounterSubSchema.getName();
        log.trace("KafkaProducerRim : PM Counter Name: {}", pmCounterName);

        Schema.Type type = pmCounterSubSchema.getField(COUNTER_VALUE).schema().getType();
        GenericRecord pmCounterSubRecord = null;
        if (mOPMCounterMap.containsKey(pmCounterName) && mOPMCounterMap.get(pmCounterName) != null  && mOPMCounterMap.get(pmCounterName).isValuePresent()) {
            MoPmCounter mo = mOPMCounterMap.get(pmCounterName);
            pmCounterSubRecord = getPmCounterValues(pmCounterSubSchema, mo, type);
        } else {
            pmCounterSubRecord = getDefaultPmCounterValues(pmCounterSubSchema, type);

        }
        pmCountersRecord.put(pmCounterSubSchema.getName(), pmCounterSubRecord);
    }

    private GenericRecord getPmCounterValues(Schema pmCounterSubSchema, MoPmCounter mo, Schema.Type type) {
        GenericRecord pmCounterSubRecord = new GenericData.Record(pmCounterSubSchema);
        if (type == Schema.Type.LONG) {
            pmCounterSubRecord.put(COUNTER_VALUE, mo.getPmCounterValue());
        } else {
            pmCounterSubRecord.put(COUNTER_VALUE, mo.getPmCounterValues());
        }
        pmCounterSubRecord.put(COUNTER_TYPE, mo.getCounterType());
        pmCounterSubRecord.put(IS_VALUE_PRESENT, true);
        return pmCounterSubRecord;
    }

    private GenericRecord getDefaultPmCounterValues(Schema pmCounterSubSchema, Schema.Type type) {
        GenericRecord pmCounterSubRecord = new GenericData.Record(pmCounterSubSchema);
        if (type == Schema.Type.LONG) {
            pmCounterSubRecord.put(COUNTER_VALUE, 0);
        } else {
            pmCounterSubRecord.put(COUNTER_VALUE, new ArrayList<>());
        }
        pmCounterSubRecord.put(COUNTER_TYPE, "null");
        pmCounterSubRecord.put(IS_VALUE_PRESENT, false);
        return pmCounterSubRecord;
    }

    private Long kafkaDoubleToLong(Double value) {
        return (value != null && !Double.isNaN(value)) ? (Math.round(value * DOUBLE_TO_LONG_CONSTANT)) : Long.MIN_VALUE;
    }

    private boolean isValidLongArray(List<Long> longArray) {
        return longArray != null && !longArray.isEmpty()
                && !longArray.contains(null);
    }

    private MoPmCounter getMoPMCounter(String counterName, String counterType, Long pmCounterValue) {
        if(pmCounterValue == null) {
            return MoPmCounter.builder().counterName(counterName).counterType(counterType).pmCounterValue(Long.MIN_VALUE).isValuePresent(false).build();
        }
        return MoPmCounter.builder().counterName(counterName).counterType(counterType).pmCounterValue(pmCounterValue).isValuePresent(true).build();
    }

    private MoPmCounter getMoPMCounters(String counterName, String counterType, List<Long> pmCounterValues) {
        if (pmCounterValues == null || (pmCounterValues.size() == 1 && pmCounterValues.contains(null))) {
            return MoPmCounter.builder().counterName(counterName).counterType(counterType).pmCounterValues(Arrays.asList(0L)).isValuePresent(false).build();
        }
        return MoPmCounter.builder().counterName(counterName).counterType(counterType).pmCounterValues(pmCounterValues).isValuePresent(true).build();
    }

    private int calculatePartition(final String nodeName) {
        int hashCode = nodeName.hashCode();

        if (hashCode < 0) {
            hashCode *= -1;
        }

        return hashCode % partitions;
    }

    /**
     * The Class PmRopNrCellDuConstants.
     */
    class PmRopNrCellDuConstantValues {
        private PmRopNrCellDuConstantValues() {
            // No Args constructor: Sonar
        }

        private static final int SCHEMA_ID = 1;
        private static final String ELEMENT_TYPE = "XML";
        private static final String MO_FDN = "noValue";
        private static final boolean SUSPECT = false;
    }

    /**
     * The Class MoPmCounterWithSchemaType.
     */
    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    class MoPmCounterWithSchemaType {
        private MoPmCounter moPmCounter;
        private Type counterValueSchemaType;
        private Type counterValueItemType;
    }
}
