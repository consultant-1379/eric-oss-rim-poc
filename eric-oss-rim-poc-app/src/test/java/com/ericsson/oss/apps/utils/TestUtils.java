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
package com.ericsson.oss.apps.utils;

import static com.ericsson.oss.apps.utils.PmConstants.MODIFIED_NRECLLDU_SCHEMA;
import static com.ericsson.oss.apps.utils.PmConstants.SCHEMA_SUBJECT_RIM;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.record.TimestampType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.kafka.KafkaProducerRim;

import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * The Class TestUtils.
 */
@Slf4j
public class TestUtils {
    public static final String EP_XnU_GNBCUUP_1_SCHEMA = "avro/schema/EP_XnU_GNBCUUP_1.avsc";
    /**
     * Of.
     *
     * @return the test utils
     */
    public static TestUtils of() {
        return new TestUtils();
    }

    /**
     * Num records in file.
     *
     * @param filename
     *     the filename
     *
     * @return the int representing the number of records in the file, assuming one line == one record.
     */
    public static int numRecordsInFile(String filename) {
        int numRecords = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            while (reader.readLine() != null) {
                numRecords++;
            }
        } catch (final IOException e) {
            log.error("Failed to count the number of records in ROP {}", filename);
        }

        // subtract one for header one for final <CR>
        return numRecords - 1;
    }

    public static int numRecordsInZipFile(String filename) {
        int numRecords = 0;

        try (FileInputStream fileInputStream = new FileInputStream(filename);
             GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, "UTF-8"))) {
            while (reader.readLine() != null) {
                numRecords++;
            }
        } catch (final IOException e) {
            log.error("Failed to count the number of records in ROP {}", filename);
        }
        // subtract one for header one for final <CR>
        return numRecords - 1;
    }

    /**
     * Gets the  specified resource file.
     *
     * @param resourceFile
     *     the resource file
     *
     * @return the file
     */
    public static File getfile(final String resourceFile) {
        try {
            return new ClassPathResource(resourceFile).getFile();
        } catch (IOException ioException) {
            throw NoSuchKeyException.builder().message(String.format("Object not found: %s", resourceFile)).build();
        }
    }

    /**
     * Creates the producer messages.
     * Not making this static, in case producer is threaded in future.
     * Wrapper for createGenericRecords with schemaFile = MODIFIED_NRECLLDU_SCHEMA
     *
     * @param kafkaProducer
     *     the kafka producer
     * @param numberRecordsToCreate
     *     the number records to create
     *
     * @return A list of 'created' Generic Records for the Kafka Producer.
     */
    public List<Optional<GenericRecord>> createGenericRecords(KafkaProducerRim kafkaProducer, int numberRecordsToCreate) {
        return createGenericRecords(kafkaProducer, numberRecordsToCreate, MODIFIED_NRECLLDU_SCHEMA);
    }

    /**
     * Creates the producer messages.
     * Not making this static, in case producer is threaded in future.
     *
     * @param kafkaProducer
     *     the kafka producer
     * @param numberRecordsToCreate
     *     the number records to create
     * @param schemaFile
     *     the schema file
     *
     * @return A list of 'created' Generic Records for the Kafka Producer.
     */
    public List<Optional<GenericRecord>> createGenericRecords(KafkaProducerRim kafkaProducer, int numberRecordsToCreate, String schemaFile) {
        List<Optional<GenericRecord>> genericRecordList = new ArrayList<>();
        Utils.of().getSchema(schemaFile).ifPresent(schema -> {
            IntStream.range(0, numberRecordsToCreate).forEach(index -> {
                createNewPmRopNRCellDU(kafkaProducer, genericRecordList, index, schema);
            });
        });
        return genericRecordList;
    }

    /**
     * Creates a PmRopNrCellDU Object with specific values for each parameter..
     *
     * @param i
     *     an index used to create distinct PmRadioMaxDeltaIpNDistr values when successive PmRopNrCellDU are created.
     *
     * @return the created PmRopNrCellDU object
     */
    public PmRopNRCellDU createPmRopNrCellDU(int i) {
        PmRopNRCellDU pmRopNRCellDU = new PmRopNRCellDU();
        pmRopNRCellDU.setPmMacRBSymAvailDl(12L);
        pmRopNRCellDU.setPmMacRBSymCsiRs(1L);
        pmRopNRCellDU.setPmMacRBSymUsedPdcchTypeA(2L);
        pmRopNRCellDU.setPmMacRBSymUsedPdcchTypeB(3L);
        pmRopNRCellDU.setPmMacRBSymUsedPdschTypeA(4L);
        pmRopNRCellDU.setPmMacRBSymUsedPdschTypeABroadcasting(5L);
        pmRopNRCellDU.setPmMacVolDl(6.0);
        pmRopNRCellDU.setPmMacVolUl(7.0);
        List<Long> pmPmRadioMaxDeltaIpNDistr = Arrays.stream(new long[] { 0L, 1L, 2L, 3L, i }).boxed().collect(Collectors.toList());
        List<Long> pmRadioSymbolDeltaIpnDistr = Arrays.stream(new long[] {1L, 3L, 5L, 7L, 9L, 0L, 1L, 2L, i }).boxed().collect(Collectors.toList());
        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(new ArrayList<>(pmPmRadioMaxDeltaIpNDistr));
        pmRopNRCellDU.setPmRadioSymbolDeltaIpnDistr(new ArrayList<>(pmRadioSymbolDeltaIpnDistr));
        // avgDeltaIpN && avgUlUeTp are calculated in pmRopNRCellDU.
        // avgUlUe is calculated based on pmMacVolUlResUe && pmMacTimeUlResUe
        // avgDeltaIpN is calculated based on pmRadioMaxDeltaIpNDistr
        pmRopNRCellDU.setPmMacVolUlResUe(10.0);
        pmRopNRCellDU.setPmMacTimeUlResUe(11.0);

        pmRopNRCellDU.setAvgDeltaIpNPreCalculated(0.0);

        MoRopId moRopId = new MoRopId();
        moRopId.setFdn("fdn-" + i);
        //2021-02-24T03:45:00+00:00
        moRopId.setRopTime(1614138300000L);
        pmRopNRCellDU.setMoRopId(moRopId);
        return pmRopNRCellDU;
    }

    /**
     * fdn,neid,granularity,software,pmRadioMaxDeltaIpNDistr,pmMacVolDl,pmMacVolUl,pmPuschSchedActivity,pmPdschAvailTime,pmPdschSchedActivity,pmPuschAvailTime,pmMacRBSymUsedPdschTypeA,
     * pmMacRBSymUsedPdcchTypeA,pmMacRBSymUsedPdcchTypeB,pmMacRBSymUsedPdschTypeABroadcasting,pmMacRBSymCsiRs,pmMacVolUlResUe,pmMacTimeUlResUe,pmMacRBSymAvailDl,avg_Ul_Ue,avg_delta_linear,
     * delta_sum,avg_delta,avg_sw2_ul_ue_throughput,avg_sw8_ul_ue_throughput,avg_sw8_avg_delta,date,timestamp
     *
     * "SubNetwork=OMCENM01,MeContext=G10056,ManagedElement=G10056,GNBDUFunction=1,NRCellDU=Q10056A","SubNetwork=OMCENM01,MeContext=G10056",900,CXP9024418_22
     * R11H09,[900 0 0 0 0],
     * 753864223,12353671,232832,5760000,470084,1440000,91126422,3928134,0,3786318,45360000,2130792,44000,3538080000,3099.333818181818,900.002072328970,900.0,1.0000001112234239e-05,
     * 1908.8528124721804,1770.980438381773,0.0006367885588107668,2022-08-03,2022-08-03 23:15:00
     *
     *
     * @return PmRopNrCellDU First Line From ROP FILE PmNRCellDU_200238_1659568500000.csv
     */
    public PmRopNRCellDU getPmRopNrCellDUFirstLineFromPmNRCellDU_200238_1659568500000() {
        PmRopNRCellDU pmRopNRCellDU = new PmRopNRCellDU();
        pmRopNRCellDU.setPmMacRBSymAvailDl(3538080000L);
        pmRopNRCellDU.setPmMacRBSymCsiRs(45360000L);
        pmRopNRCellDU.setPmMacRBSymUsedPdcchTypeA(3928134L);
        pmRopNRCellDU.setPmMacRBSymUsedPdcchTypeB(0L);
        pmRopNRCellDU.setPmMacRBSymUsedPdschTypeA(91126422L);
        pmRopNRCellDU.setPmMacRBSymUsedPdschTypeABroadcasting(3786318L);
        pmRopNRCellDU.setPmMacVolDl(753864223.0);
        pmRopNRCellDU.setPmMacVolUl(12353671.0);
        List<Long> pmCounterValues = Arrays.stream(new long[] { 900L, 0L, 0L, 0L, 0L }).boxed().collect(Collectors.toList());
        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(new ArrayList<>(pmCounterValues));
        pmRopNRCellDU.setPmMacVolUlResUe(2130792.0);
        pmRopNRCellDU.setPmMacTimeUlResUe(44000.0);

        // Note: agDeltaIpNPreCalculated changed this in the ROP File from the original value of 0.00001 for testing purposes
        // so that it gives different value to calculated avgDeltaIpN
        pmRopNRCellDU.setAvgDeltaIpNPreCalculated(0.1);

        MoRopId moRopId = new MoRopId();
        moRopId.setFdn("SubNetwork=OMCENM01,MeContext=G10056,ManagedElement=G10056,GNBDUFunction=1,NRCellDU=Q10056A");
        //2021-02-24T03:45:00+00:00
        moRopId.setRopTime(1659568500000L);
        pmRopNRCellDU.setMoRopId(moRopId);
        return pmRopNRCellDU;
    }

    /**
     * Checks if two PmRopNrCellDU equal.
     * Need this as Double.NaN != Double.NaN so assertThat(actualPmRopNRCellDU).usingRecursiveComparison().isEqualTo(expectedPmRopNrCellDu), will not be equal.
     *
     * @param pmRopNRCellDU
     *     the PmRopNrCellDU
     * @param otherPmRopNRCellDU
     *     the other PmRopNrCellDU
     *
     * @return true, if is the two PmRopNrCellDU are equal.
     */
    public boolean isPmRopNrCellDUEqual(PmRopNRCellDU pmRopNRCellDU , PmRopNRCellDU otherPmRopNRCellDU ) {
        if (pmRopNRCellDU == otherPmRopNRCellDU) {
            return true;
        }
        if (otherPmRopNRCellDU == null || pmRopNRCellDU.getClass() != otherPmRopNRCellDU.getClass()) {
            return false;
        }

        return pmRopNRCellDU.getMoRopId().equals(otherPmRopNRCellDU.getMoRopId())
            && pmRopNRCellDU.getPmMacVolUl().equals(otherPmRopNRCellDU.getPmMacVolUl())
            && pmRopNRCellDU.getPmMacVolDl().equals(otherPmRopNRCellDU.getPmMacVolDl())
            && pmRopNRCellDU.getPmMacVolUlResUe().equals(otherPmRopNRCellDU.getPmMacVolUlResUe())
            && pmRopNRCellDU.getPmMacTimeUlResUe().equals(otherPmRopNRCellDU.getPmMacTimeUlResUe())
            && pmRopNRCellDU.getAvgDeltaIpN().equals(otherPmRopNRCellDU.getAvgDeltaIpN())
            && pmRopNRCellDU.getAvgUlUeTp().equals(otherPmRopNRCellDU.getAvgUlUeTp())
            && pmRopNRCellDU.getPmMacRBSymUsedPdschTypeA().equals(otherPmRopNRCellDU.getPmMacRBSymUsedPdschTypeA())
            && pmRopNRCellDU.getPmMacRBSymUsedPdcchTypeA().equals(otherPmRopNRCellDU.getPmMacRBSymUsedPdcchTypeA())
            && pmRopNRCellDU.getPmMacRBSymUsedPdcchTypeB().equals(otherPmRopNRCellDU.getPmMacRBSymUsedPdcchTypeB())
            && pmRopNRCellDU.getPmMacRBSymUsedPdschTypeABroadcasting().equals(otherPmRopNRCellDU.getPmMacRBSymUsedPdschTypeABroadcasting())
            && pmRopNRCellDU.getPmMacRBSymCsiRs().equals(otherPmRopNRCellDU.getPmMacRBSymCsiRs())
            && pmRopNRCellDU.getPmMacRBSymAvailDl().equals(otherPmRopNRCellDU.getPmMacRBSymAvailDl())
            && pmRopNRCellDU.getPmRadioMaxDeltaIpNDistr().equals(otherPmRopNRCellDU.getPmRadioMaxDeltaIpNDistr())
            && pmRopNRCellDU.getTotalBinSumSymbolDeltaIpn().equals(otherPmRopNRCellDU.getTotalBinSumSymbolDeltaIpn())
            && pmRopNRCellDU.getPositiveBinSumSymbolDeltaIpn().equals(otherPmRopNRCellDU.getPositiveBinSumSymbolDeltaIpn())
            && pmRopNRCellDU.getPmRadioSymbolDeltaIpnDistr().equals(otherPmRopNRCellDU.getPmRadioSymbolDeltaIpnDistr())
            && pmRopNRCellDU.getAvgSymbolDeltaIpn().equals(otherPmRopNRCellDU.getAvgSymbolDeltaIpn());
    }

    /**
     * Serialize generic record using Json encoder.
     *
     * @param pmCtrsGenericRecord
     *     the pm counters generic record to serialize into bytes
     *
     * @return the byte[]
     */
    public byte[] serializeGenericRecordJsonEncoder(GenericRecord pmCtrsGenericRecord) {
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(pmCtrsGenericRecord.getSchema());
        byte[] data = new byte[0];
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder jsonEncoder = null;
        try {
            jsonEncoder = EncoderFactory.get().jsonEncoder(pmCtrsGenericRecord.getSchema(), stream);
            datumWriter.write(pmCtrsGenericRecord, jsonEncoder);
            jsonEncoder.flush();
            data = stream.toByteArray();
        } catch (IOException e) {
            log.error("Serialization error (Json Encoder):" + e.getMessage());
        }
        return data;
    }

    /**
     * Deserialize generic record using Json Decoder.
     *
     * @param data
     *     the data ( in bytes)
     *
     * @return the NRCellDU_1 Object
     */
    public NRCellDU_GNBDU_1 deserializeGenericRecordJsonDecoder(byte[] data) {
        DatumReader<NRCellDU_GNBDU_1> reader = new SpecificDatumReader<>(NRCellDU_GNBDU_1.class);
        Decoder decoder = null;
        try {
            decoder = DecoderFactory.get().jsonDecoder(NRCellDU_GNBDU_1.getClassSchema(), new String(data));
            return reader.read(null, decoder);
        } catch (IOException e) {
            log.error("Deserialization error (Json Decoder):" + e.getMessage());
        }
        return null;
    }

    /**
     * Convert Generic Record containing PM Counters to NRCellDU_GNBDU_1 Object using JSON Endoder & Decoder
     *
     * @param genericRecord
     *     the generic record
     *
     * @return the NRCellDU_GNBDU_1
     */
    public NRCellDU_GNBDU_1 convertViaJson(GenericRecord genericRecord) {
        byte[] data = TestUtils.of().serializeGenericRecordJsonEncoder(genericRecord);
        return TestUtils.of().deserializeGenericRecordJsonDecoder(data);
    }

    /**
     * Serialize generic record using Avro encoder.
     * Note: Make sure to close the encoder.
     *
     * @param genericRecord
     *     the generic record to convert
     * @param schemaRegistryUrl
     *     the schema registry url
     * @param topicName
     *     the topic name, this is a unique identifier for schema registry, need not be an existing kafka topic, but must be unique for that schema
     *     name.
     * @param schemaFilename
     *     the schema filename, used to convert the generic record.
     *
     * @return the byte[]
     */
    @SuppressWarnings("deprecation")
    public byte[] serializeGenericRecordAvroEncoder(GenericRecord genericRecord, final String schemaRegistryUrl, String topicName,
                                                    String schemaFilename) {
        try (KafkaAvroSerializer kas = new KafkaAvroSerializer()) {
            Map<String, Object> config = new HashMap<>();
            config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
            config.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
            kas.configure(config, false);
            //TODO: Replace depreciated method.
            kas.register(topicName, Utils.of().getSchema(MODIFIED_NRECLLDU_SCHEMA).orElseThrow());
            byte[] serializedBytes = kas.serialize(topicName, genericRecord);
            log.trace("Serialized Bytes = {}", getBytesAsString(serializedBytes));
            return serializedBytes;
        } catch (Exception e) {
            log.error("Serialization error:" + e.getMessage());
        }
        return new byte[] {};
    }

    /**
     * Deserialize bytes to NRCellDU_GNBDU_1 using Avro deccoder.
     * Note: Make sure to close the decoder.
     *
     * @param bytes
     *     the bytes
     * @param schemaRegistryUrl
     *     the schema registry url
     * @param topicName
     *     the topic name, this is a unique identifier for schema registry, need not be an existing kafka topic, but must be unique for that schema
     *     name.
     * @param schemaFilename
     *     the schema filename
     *
     * @return the deserialized Object
     */
    public Object deserializeGenericRecordAvroEncoder(byte[] bytes, final String schemaRegistryUrl, String topicName,
                                                                String schemaFilename) {
        try {
            KafkaAvroDeserializer kad = Utils.of().getKafkaAvroDeserializer(schemaRegistryUrl, topicName, schemaFilename);
            return kad.deserialize(topicName, bytes);
        } catch (Exception e) {
            log.error("Deserialization error:" + e.getMessage());
        }
        return new NRCellDU_GNBDU_1();
    }

    /**
     * Convert Generic Record containing PM Counters to NRCellDU_GNBDU_1 Object using Avro Endoder & Decoder
     *
     * @param genericRecord
     *     the generic record
     *
     * @return the NRCellDU_GNBDU_1
     */
    public Object convertViaAvro(GenericRecord genericRecord, final String schemaRegistryUrl, String topicName, String schemaFilename) {
        byte[] data = TestUtils.of().serializeGenericRecordAvroEncoder(genericRecord, schemaRegistryUrl, topicName, schemaFilename);
        return TestUtils.of().deserializeGenericRecordAvroEncoder(data, schemaRegistryUrl, topicName, schemaFilename);
    }

    /**
     * Convert a byte array to a printable 'hex' list of bytes.
     *
     * @param bytes
     *     the raw bytes
     *
     * @return String with the bytes formatted.
     */
    public String getBytesAsString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Creates a list of consumer records from the given lis of GenericRecords.
     *
     * @param kafkaProducer
     *     the kafka producer to use for conversion of the records.
     * @param genericRecordList
     *     the list of generic records to convert to consumer records.
     * @param schemaRegistryUrl
     *     the schema registry url to use... can use '"mock://testurl' local conversion.
     *
     * @return the list of consumer records
     */
    public List<ConsumerRecord<String, byte[]>> getConsumerRecords(final KafkaProducerRim kafkaProducer, final List<GenericRecord> genericRecordList, final String schemaRegistryUrl){
        List<ConsumerRecord<String, byte[]>> consumerRecords = new ArrayList<>();
        genericRecordList.forEach(gr -> {
            byte[] serializedBytes = TestUtils.of()
                .serializeGenericRecordAvroEncoder(gr, schemaRegistryUrl, SCHEMA_SUBJECT_RIM + MODIFIED_NRECLLDU_SCHEMA, MODIFIED_NRECLLDU_SCHEMA);

            kafkaProducer.getKafkaProducerMessage(gr).ifPresent(message -> {
                ConsumerRecord<String, byte[]> cr = new ConsumerRecord<>((String) message.getHeaders().get(KafkaHeaders.TOPIC),
                    (int) message.getHeaders().get(KafkaHeaders.PARTITION_ID),
                    0L,
                    ConsumerRecord.NO_TIMESTAMP,
                    TimestampType.NO_TIMESTAMP_TYPE,
                    -1,
                    -1,
                    (String) message.getHeaders().get(KafkaHeaders.MESSAGE_KEY),
                    serializedBytes,
                    kafkaProducer.getRecordHeaders(message.getHeaders()),
                    Optional.empty());

                consumerRecords.add(cr);
            });
        });
        return consumerRecords;
    }

    /**
     * Create one NRCellDU_GNBDU_1 with defined values.
     *
     */
    public NRCellDU_GNBDU_1 getOnePmAvroCountersRecord() {
        KafkaProducerRim kp = new KafkaProducerRim();
        ReflectionTestUtils.setField(kp, "partitions", 3);
        ReflectionTestUtils.setField(kp, "counterParserTopicName", "test-consumer-topic");

        PmRopNRCellDU pmRopNRCellDU = TestUtils.of().createPmRopNrCellDU(0);
        Optional<GenericRecord> gr = kp.createProducerMessagesGenericRecord(pmRopNRCellDU, Utils.of().getSchema(MODIFIED_NRECLLDU_SCHEMA).get());
        return convertViaJson(gr.get());
    }

    private void createNewPmRopNRCellDU(KafkaProducerRim kafkaProducer, final List<Optional<GenericRecord>> genericRecordList, int i, Schema schema) {
        PmRopNRCellDU pmRopNRCellDU = createPmRopNrCellDU(i);

        Optional<GenericRecord> gr = kafkaProducer.createProducerMessagesGenericRecord(pmRopNRCellDU, schema);
        genericRecordList.add(gr);
    }

    public boolean areListsEqual(List list1, List list2) {
        if (list1 == null && list2 == null) {
            return true;
        }
        if (list1 == null || list2 == null || list1.size() != list2.size()) {
            return false;
        }
        Collections.sort(list1);
        Collections.sort(list2);
        return list1.equals(list2);
    }

}
