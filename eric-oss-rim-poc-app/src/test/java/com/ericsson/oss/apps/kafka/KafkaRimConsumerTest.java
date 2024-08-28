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

import static com.ericsson.oss.apps.utils.PmConstants.NODE_FDN;
import static com.ericsson.oss.apps.utils.PmConstants.ELEMENT_TYPE;
import static com.ericsson.oss.apps.utils.PmConstants.SCHEMA_ID;
import static com.ericsson.oss.apps.utils.PmConstants.MO_TYPE;
import static com.ericsson.oss.apps.utils.PmConstants.NRECLL_DU_SCHEMA;
import static com.ericsson.oss.apps.utils.PmConstants.SUSPECT;
import static com.ericsson.oss.apps.utils.PmConstants.ROP_END_TIME;
import static com.ericsson.oss.apps.utils.PmConstants.MODIFIED_NRECLLDU_SCHEMA;

import static com.ericsson.oss.apps.utils.TestUtils.EP_XnU_GNBCUUP_1_SCHEMA;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.data.collection.KafkaRopScheduler;
import com.ericsson.oss.apps.data.collection.AppDataConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.ericsson.oss.apps.data.collection.pmrop.PmRopLoader;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import com.ericsson.oss.apps.utils.TestUtils;
import com.ericsson.oss.apps.utils.Utils;

import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.micrometer.core.instrument.Counter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@SpringBootTest(classes = { CoreApplication.class, KafkaRopScheduler.class }, properties = { "spring.kafka.mode.enabled=true", "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}" })
@EmbeddedKafka(partitions = 3, topics = {"eric-oss-3gpp-pm-xml-ran-parser-nr"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("production")
class KafkaRimConsumerTest {
    private static final List<String> MESSAGE_HEADER_KEYS = Arrays.asList(NODE_FDN, ELEMENT_TYPE, MO_TYPE, SCHEMA_ID);
    private static final String BUCKET = "rim";

    private static final Path ROP_PATH = Path.of("pm", "rop");
    private static final String CSV_ROP = "pm-NRCellDU-200238-1659568500000.csv.gz";

    private static final String CUSTOMER_ID_200238 = "200238";
    private static final long CSV_ROP_TS_1659568500000L = 1659568500000L;

    private static final int PRODUCER_SEND_WAIT_MS = 20000;
    private static final int CONSUMER_SEND_WAIT_MS = 20000;

    private final Logger logger = (Logger) LoggerFactory.getLogger(CounterParserTopicListenerRim.class);

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private CounterParserTopicListenerRim counterParserTopicListenerRim;

    @Autowired
    protected KafkaProducerRim kafkaProducer;

    @Autowired
    private KafkaAvroDeserializer kad;

    @Value("${spring.kafka.topics.input.name}")
    private String counterParserTopicName;
    @Value("${spring.kafka.bootstrap-servers}")
    private String brokerAddresses;

    @Value("${spring.kafka.consumer.group-id}")
    protected String group;

    @Value("${spring.kafka.schema-registry.url}")
    private String schemaRegistryUrl;

    @Autowired
    private FileTracker fileTracker;

    @Mock
    private BdrConfiguration bdrConfiguration;
    @Mock
    private BdrClient bdrClient;

    @Mock(lenient = true)
    private Counter counter;

    @Mock
    private PmRopNrCellDuRepo pmRopNrCellDuRepo;

    @Mock
    private AppDataConfig appDataConfig;

    @Captor
    ArgumentCaptor<List<PmRopNRCellDU>> pmRopNRCellDUArgumentCaptor;

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "pmRopNrCellDuRepo", pmRopNrCellDuRepo);
        brokerAddresses = embeddedKafkaBroker.getBrokersAsString();
        CounterParserTopicListenerRim.setPatternValidateFdn(Pattern.compile("fdn.+"));
        logger.setLevel(Level.TRACE);
    }

    @AfterEach
    public void cleanup() {
        logger.setLevel(Level.INFO);
    }

    @SuppressWarnings("unchecked")
    @Test
    @Order(1)
    void loadCsvPmTestRopSuccess_test() throws Exception {
        logger.setLevel(Level.INFO);
        CounterParserTopicListenerRim.setPatternValidateFdn(Pattern.compile("SubNetwork.+MeContext.+ManagedElement.+GNBDUFunction.+NRCellDU.+"));
        Mockito.when(appDataConfig.getAppDataPmRopPath()).thenReturn("pm/rop/");
        loadCsvPmTestRopPerfromTest(false);
        Mockito.reset(pmRopNrCellDuRepo);
        Mockito.reset(bdrConfiguration);
        Mockito.reset(bdrClient);
        pmRopNRCellDUArgumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
        loadCsvPmTestRopPerfromTest(true);
    }

    /*
     * Schema supplied by Parsers will not have avgDeltaIpNPreCalculated included
     * But this schema has been modified (here) to add avgDeltaIpNPreCalculated
     * So produce Generic Record with schema that HAS avgDeltaIpNPreCalculated
     * Consumer with schema that does have avgDeltaIpNPreCalculated.
     * KafkaProducerRim set the avgDeltaIpNPreCalculated to 0.0, the default value in PmRopNrCEllDU is Double.NaN
     * So expecting 0.0 .
     */
    @Test
    @Order(2)
    void counterListenerConsumerSuccessWithModifiedSchema() throws Exception {
        logger.setLevel(Level.INFO);
        Mockito.when(appDataConfig.isUsePreCalculatedAvgDeltaIpN()).thenReturn(false);
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "appDataConfig", appDataConfig);
        List<Optional<GenericRecord>> genericRecordList = createProducerMessages(10);

        counterParserTopicListenerRim.getLogs();

        long summedOffsetsBefore = getTotalNumberRecordsAcknowledged();
        CounterValues cvBefore = getCounterParserMetricValues();
        consumeGeneriRecord(genericRecordList, cvBefore);

        counterParserTopicListenerRim.getLogs();
        testOffsetsOK(summedOffsetsBefore, cvBefore);
        assertMetrics(cvBefore, genericRecordList.size(), genericRecordList.size(), 0, 0);
        verifyTestRop(genericRecordList.size());
    }

    /*
     * Schema supplied by Parsers will not have avgDeltaIpNPreCalculated included
     * So produce Generic Record with schema that does not have avgDeltaIpNPreCalculated
     * Consumer with schema that does have avgDeltaIpNPreCalculated.
     * KafkaProducerRim set the avgDeltaIpNPreCalculated to 0.0, the default value in PmRopNrCEllDU is Double.NaN
     * So expecting Double.NaN.
     */

    @Test
    @Order(2)
    void counterListenerConsumerSuccessWithUnmodifiedSchema() throws Exception {
        Mockito.when(appDataConfig.isUsePreCalculatedAvgDeltaIpN()).thenReturn(false);
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "appDataConfig", appDataConfig);
        List<Optional<GenericRecord>> genericRecordList = TestUtils.of().createGenericRecords(kafkaProducer, 1, NRECLL_DU_SCHEMA);

        counterParserTopicListenerRim.getLogs();

        //TODO: Maybe.. make sure these are stable before processing with test. See note on Test Stability
        long summedOffsetsBefore = getTotalNumberRecordsAcknowledged();
        CounterValues cvBefore = getCounterParserMetricValues();
        consumeGeneriRecord(genericRecordList, cvBefore);

        counterParserTopicListenerRim.getLogs();
        testOffsetsOK(summedOffsetsBefore, cvBefore);
        assertMetrics(cvBefore, genericRecordList.size(), genericRecordList.size(), 0, 0);

        PmRopNRCellDU actualPmRopNRCellDU = getActualRop("fdn-0", genericRecordList.size());
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setAvgDeltaIpNPreCalculated(Double.NaN);
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));
    }

    @Test
    @Order(3)
    void counterListenerConsumerGenericRecordFail() throws Exception {
        Mockito.when(appDataConfig.isUsePreCalculatedAvgDeltaIpN()).thenReturn(false);
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "appDataConfig", appDataConfig);
        List<Optional<GenericRecord>> genericRecordList = createProducerMessages(12);
        int numBadRecords = 4;
        int numEmptyRecords = 1;
        int numInvalidMoType = 0;
        setInvalidRoptime(genericRecordList, 2);
        setSuspectFlag(genericRecordList, 3);
        setRopTimeZero(genericRecordList, 4);
        setRopTimeBadFormat(genericRecordList, 5);
        setGenericRecordEmpty(genericRecordList, 6);

        counterParserTopicListenerRim.getLogs();

        long summedOffsetsBefore = getTotalNumberRecordsAcknowledged();
        CounterValues cvBefore = getCounterParserMetricValues();
        sendToInputTopic(genericRecordList, numEmptyRecords);
        waitforConsumer(genericRecordList.size() - numEmptyRecords, 20000, cvBefore);

        counterParserTopicListenerRim.getLogs();
        testOffsetsOK(summedOffsetsBefore, cvBefore);

        assertMetrics(cvBefore, genericRecordList.size() - numEmptyRecords, genericRecordList.size() - (numBadRecords + numEmptyRecords),
            numBadRecords, numInvalidMoType);
        verifyTestRop(genericRecordList.size() - (numBadRecords + numEmptyRecords));
    }

    @Test
    @Order(3)
    void counterListenerConsumerHeaderFail() throws Exception {
        Mockito.when(appDataConfig.isUsePreCalculatedAvgDeltaIpN()).thenReturn(false);
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "appDataConfig", appDataConfig);
        List<Optional<GenericRecord>> genericRecordList = createProducerMessages(12);
        int numBadRecords = 4;
        int numInvalidMoType = 2;
        Map<String, BadHeaders> bhmap = new HashMap<>();

        bhmap.put("fdn-8", new BadHeaders("fdn-8", "moType", true));
        bhmap.put("fdn-9", new BadHeaders("fdn-9", "moType", true));
        bhmap.put("fdn-10", new BadHeaders("fdn-10", "moType", false));
        bhmap.put("fdn-11", new BadHeaders("fdn-11", "moType", false));

        List<Message<GenericRecord>> messages = buildMessages(genericRecordList, bhmap);

        long summedOffsetsBefore = getTotalNumberRecordsAcknowledged();
        CounterValues cvBefore = getCounterParserMetricValues();
        consumeMessage(messages, cvBefore);

        counterParserTopicListenerRim.getLogs();
        testOffsetsOK(summedOffsetsBefore, cvBefore);

        assertMetrics(cvBefore, genericRecordList.size(), genericRecordList.size() - numBadRecords, numBadRecords, numInvalidMoType);
        verifyTestRop(genericRecordList.size() - numBadRecords);
    }

    @Test
    @Order(4)
    void counterListenerConsumerThrowsExceptionFail() {
        PmRopNrCellDuRepo pmRopNrCellDuRepoMock = Mockito.mock(PmRopNrCellDuRepo.class);
        Mockito.when(pmRopNrCellDuRepoMock.saveAll(Mockito.any())).thenThrow(new RimHandlerException("Test Batch Exception"));
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "pmRopNrCellDuRepo", pmRopNrCellDuRepoMock);
        // Check max.poll.records. Make sure we have at least more than one batch
        final List<Optional<GenericRecord>> genericRecordList = createProducerMessages(5);

        counterParserTopicListenerRim.getLogs();

        CounterValues cvBefore = getCounterParserMetricValues();
        sendToInputTopic(genericRecordList);
        await().atMost(Duration.of(20000, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(1000)).untilAsserted(() -> {
            counterParserTopicListenerRim.getLogs();
            CounterValues cvNow = getCounterParserMetricValues();
            double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
            double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
            double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
            assertEquals(numRecordReceived, (genericRecordList.size()));
            assertThat(numRecordProcessed).isZero();
            assertThat(numRecordDropped).isEqualTo(genericRecordList.size());
        });
    }

    @Test
    @Order(5)
    void counterListenerConsumerManualTestSuccess() throws Exception {
        List<ConsumerRecord<String, byte[]>> consumerRecords = createConsumerRecords(1);

        CounterValues cvBefore = getCounterParserMetricValues();
        counterParserTopicListenerRim.listen(consumerRecords);
        CounterValues cvNow = getCounterParserMetricValues();
        double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
        double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
        double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
        assertThat(numRecordReceived).isEqualTo(consumerRecords.size());
        assertThat(numRecordProcessed).isEqualTo(consumerRecords.size());
        assertThat(numRecordDropped).isZero();
    }

    @Test
    @Order(6)
    void counterListenerConsumerManualNull() throws Exception {
        List<ConsumerRecord<String, byte[]>> consumerRecords = new ArrayList<>();
        final List<GenericRecord> genericRecordList = createProducerMessages(3).stream()
            .filter(Optional::isPresent)
            .map(gro -> gro.get())
                .collect(Collectors.toList());

        GenericRecord gr0 = genericRecordList.get(0);
        kafkaProducer.getKafkaProducerMessage(gr0).ifPresent(message -> {
            ConsumerRecord<String, byte[]> cr0 = new ConsumerRecord<>((String) message.getHeaders().get(KafkaHeaders.TOPIC),
                (int) message.getHeaders().get(KafkaHeaders.PARTITION_ID),
                0L,
                ConsumerRecord.NO_TIMESTAMP,
                TimestampType.NO_TIMESTAMP_TYPE,
                -1,
                -1,
                (String) message.getHeaders().get(KafkaHeaders.MESSAGE_KEY),
                TestUtils.of().serializeGenericRecordJsonEncoder(message.getPayload()),
                new RecordHeaders(),
                Optional.empty());

            consumerRecords.add(cr0);
        });

        GenericRecord gr1 = genericRecordList.get(1);
        kafkaProducer.getKafkaProducerMessage(gr1).ifPresent(message -> {
            ConsumerRecord<String, byte[]> cr1 = new ConsumerRecord<>((String) message.getHeaders().get(KafkaHeaders.TOPIC),
                (int) message.getHeaders().get(KafkaHeaders.PARTITION_ID),
                0L,
                ConsumerRecord.NO_TIMESTAMP,
                TimestampType.NO_TIMESTAMP_TYPE,
                -1,
                -1,
                (String) message.getHeaders().get(KafkaHeaders.MESSAGE_KEY),
                null,
                kafkaProducer.getRecordHeaders(message.getHeaders()),
                Optional.empty());

            consumerRecords.add(cr1);
        });

        consumerRecords.add(null);

        GenericRecord gr2 = genericRecordList.get(2);
        kafkaProducer.getKafkaProducerMessage(gr2).ifPresent(message -> {
            Headers recordHeaders = kafkaProducer.getRecordHeaders(message.getHeaders());
            recordHeaders.remove(NODE_FDN);

            ConsumerRecord<String, byte[]> cr2 = new ConsumerRecord<>((String) message.getHeaders().get(KafkaHeaders.TOPIC),
                (int) message.getHeaders().get(KafkaHeaders.PARTITION_ID),
                0L,
                ConsumerRecord.NO_TIMESTAMP,
                TimestampType.NO_TIMESTAMP_TYPE,
                -1,
                -1,
                (String) message.getHeaders().get(KafkaHeaders.MESSAGE_KEY),
                TestUtils.of().serializeGenericRecordJsonEncoder(message.getPayload()),
                recordHeaders,
                Optional.empty());

            consumerRecords.add(cr2);
        });

        CounterValues cvBefore = getCounterParserMetricValues();
        counterParserTopicListenerRim.listen(consumerRecords);
        CounterValues cvNow = getCounterParserMetricValues();
        double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
        double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
        double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
        assertThat(numRecordReceived).isEqualTo(consumerRecords.size());
        assertThat(numRecordDropped).isEqualTo(consumerRecords.size());
        assertThat(numRecordProcessed).isZero();
    }

    @SuppressWarnings("unchecked")
    @Test
    @Order(7)
    void counterListenerConsumerHeaderThrowsExceptionFail() {
        List<ConsumerRecord<String, byte[]>> consumerRecords = createConsumerRecords(2);
        ConsumerRecord<String, byte[]> cr = Mockito.mock(ConsumerRecord.class);
        Mockito.when(cr.headers()).thenThrow(new RimHandlerException("Test Record Reject Exception"));
        consumerRecords.add(cr);

        CounterValues cvBefore = getCounterParserMetricValues();
        counterParserTopicListenerRim.listen(consumerRecords);
        CounterValues cvNow = getCounterParserMetricValues();
        double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
        double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
        double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
        assertThat(numRecordReceived).isEqualTo(consumerRecords.size());
        assertThat(numRecordDropped).isEqualTo(1);
        assertThat(numRecordProcessed).isEqualTo(2);
    }

    // throw exception in isValidNrCellDURecordHeader()
    @SuppressWarnings("unchecked")
    @Test
    @Order(8)
    void counterListenerConsumerIsValidNrCellDURecordHeaderThrowsExceptionFail() {
        List<ConsumerRecord<String, byte[]>> consumerRecords = createConsumerRecords(2);
        ConsumerRecord<String, byte[]> cr = Mockito.mock(ConsumerRecord.class);
        Mockito.when(cr.value()).thenThrow(new RimHandlerException("Test Record Reject Exception"));
        consumerRecords.add(cr);

        CounterValues cvBefore = getCounterParserMetricValues();
        counterParserTopicListenerRim.listen(consumerRecords);
        CounterValues cvNow = getCounterParserMetricValues();
        double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
        double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
        double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
        assertThat(numRecordReceived).isEqualTo(consumerRecords.size());
        assertThat(numRecordDropped).isEqualTo(1);
        assertThat(numRecordProcessed).isEqualTo(2);
    }

    // throw exception from isValidFdn()
    @Test
    @Order(9)
    void counterListenerConsumerIsValidFdnThrowsExceptionFail() {
        logger.setLevel(Level.INFO);
        List<ConsumerRecord<String, byte[]>> consumerRecords = createConsumerRecords(1);
        KafkaAvroDeserializer kadMock = Mockito.mock(KafkaAvroDeserializer.class);
        NRCellDU_GNBDU_1 nrCellDuPmCtrsMock = Mockito.mock(NRCellDU_GNBDU_1.class);
        Mockito.when(nrCellDuPmCtrsMock.getMoFdn()).thenThrow(new RimHandlerException("Test Record Reject Exception"));
        Mockito.when(kadMock.deserialize(Mockito.anyString(), Mockito.any())).thenReturn(nrCellDuPmCtrsMock);

        ReflectionTestUtils.setField(counterParserTopicListenerRim, "kad", kadMock);

        CounterValues cvBefore = getCounterParserMetricValues();
        counterParserTopicListenerRim.listen(consumerRecords);
        CounterValues cvNow = getCounterParserMetricValues();
        double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
        double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
        double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
        assertThat(numRecordReceived).isEqualTo(consumerRecords.size());
        assertThat(numRecordDropped).isEqualTo(1);
        assertThat(numRecordProcessed).isZero();
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "kad", kad);
    }

    // throw exception from ProcessPmCounterAvro(), i.e. KafkaAvroDeserializer throws exception
    @Test
    @Order(9)
    void counterListenerConsumerDeserializerThrowsExceptionFail() {
        logger.setLevel(Level.INFO);
        List<ConsumerRecord<String, byte[]>> consumerRecords = createConsumerRecords(1);
        KafkaAvroDeserializer kadMock = Mockito.mock(KafkaAvroDeserializer.class);
        Mockito.when(kadMock.deserialize(Mockito.anyString(), Mockito.any())).thenThrow(new RimHandlerException("Test Record Reject Exception"));

        ReflectionTestUtils.setField(counterParserTopicListenerRim, "kad", kadMock);

        CounterValues cvBefore = getCounterParserMetricValues();
        counterParserTopicListenerRim.listen(consumerRecords);
        CounterValues cvNow = getCounterParserMetricValues();
        double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
        double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
        double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
        assertThat(numRecordReceived).isEqualTo(consumerRecords.size());
        assertThat(numRecordDropped).isEqualTo(1);
        assertThat(numRecordProcessed).isZero();
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "kad", kad);
    }

    @Test
    @Order(9)
    void counterListenerConsumerDeserializerReturnsNull() {
        logger.setLevel(Level.INFO);
        List<ConsumerRecord<String, byte[]>> consumerRecords = createConsumerRecords(1);
        KafkaAvroDeserializer kadMock = Mockito.mock(KafkaAvroDeserializer.class);
        Mockito.when(kadMock.deserialize(Mockito.anyString(), Mockito.any())).thenReturn(null);

        ReflectionTestUtils.setField(counterParserTopicListenerRim, "kad", kadMock);

        CounterValues cvBefore = getCounterParserMetricValues();
        counterParserTopicListenerRim.listen(consumerRecords);
        CounterValues cvNow = getCounterParserMetricValues();
        double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
        double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
        double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
        assertThat(numRecordReceived).isEqualTo(consumerRecords.size());
        assertThat(numRecordDropped).isEqualTo(1);
        assertThat(numRecordProcessed).isZero();
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "kad", kad);
    }
    @Test
    @Order(10)
    void counterListenerConsumerAverageAndDeltaIpNNull() throws Exception {
        testDelaIpnAndAveragesForNullOrNan(null);
    }

    @Test
    @Order(11)
    void counterListenerConsumerDeltaIpNNullAveragesNan() throws Exception {
        testDelaIpnAndAveragesForNullOrNan(Double.NaN);
    }

    @Test
    @Order(12)
    void counterListenerConsumerWithOtherSchema() throws Exception {
        logger.setLevel(Level.INFO);
        KafkaProducerRim kafkaProducerRim = new KafkaProducerRim();
        List<String> schemaNames = Arrays.asList(EP_XnU_GNBCUUP_1_SCHEMA, MODIFIED_NRECLLDU_SCHEMA);
        final List<Optional<GenericRecord>> genericRecordList = new ArrayList<>();

        IntStream.range(0, schemaNames.size()).forEach(index -> {
            PmRopNRCellDU pmRopNRCellDU = TestUtils.of().createPmRopNrCellDU(0);
            Optional<GenericRecord> gr = kafkaProducerRim
                .createProducerMessagesGenericRecord(pmRopNRCellDU, Utils.of().getSchema(schemaNames.get(index)).get());
            printGenericRecord(gr, schemaNames.get(index));
            genericRecordList.add(gr);
        });
        logger.setLevel(Level.TRACE);

        long summedOffsetsBefore = getTotalNumberRecordsAcknowledged();
        CounterValues cvBefore = getCounterParserMetricValues();
        consumeGeneriRecord(genericRecordList, cvBefore);

        counterParserTopicListenerRim.getLogs();
        testOffsetsOK(summedOffsetsBefore, cvBefore);

        assertMetrics(cvBefore, genericRecordList.size(), genericRecordList.size() - 1, 1, 1);
        PmRopNRCellDU actualPmRopNRCellDU = getActualRop("fdn-0", genericRecordList.size() - 1);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setAvgDeltaIpNPreCalculated(Double.NaN);
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));
    }

    private void printGenericRecord(Optional<GenericRecord> grOpt, String type) {
        grOpt.ifPresent(gr -> {
            log.info("-------------------------------------------------------");
            StringBuilder sb = new StringBuilder();
            gr.getSchema()
                .getFields()
                .forEach(field -> sb.append(type).append(" - ").append(field.name()).append(":").append(gr.get(field.name())).append("\n"));
            log.info("\n{}", sb.toString());

            log.info("-------------------------------------------------------");
            sb.setLength(0);
        });
    }
    private void testDelaIpnAndAveragesForNullOrNan(Double valueToSet) throws Exception {
        PmRopNRCellDU pmRopNRCellDU = TestUtils.of().createPmRopNrCellDU(1);
        pmRopNRCellDU.setAvgDeltaIpNPreCalculated(valueToSet);
        pmRopNRCellDU.setPmMacTimeUlResUe(-1.0);  // cause avgUlUeTp to return NaN
        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(null);
        Optional<GenericRecord> gr = kafkaProducer.createProducerMessagesGenericRecord(pmRopNRCellDU,
            Utils.of().getSchema(MODIFIED_NRECLLDU_SCHEMA).get());
        final List<Optional<GenericRecord>> genericRecordList = new ArrayList<>();
        genericRecordList.add(gr);

        long summedOffsetsBefore = getTotalNumberRecordsAcknowledged();
        CounterValues cvBefore = getCounterParserMetricValues();
        consumeGeneriRecord(genericRecordList, cvBefore);

        counterParserTopicListenerRim.getLogs();
        testOffsetsOK(summedOffsetsBefore, cvBefore);

        assertMetrics(cvBefore, genericRecordList.size(), genericRecordList.size(), 0, 0);
        PmRopNRCellDU actualPmRopNRCellDU = getActualRop("fdn-1", 1);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(1);
        expectedPmRopNrCellDu.setPmRadioMaxDeltaIpNDistr(new ArrayList<>());
        expectedPmRopNrCellDu.setAvgDeltaIpNPreCalculated((double) Long.MIN_VALUE);
        expectedPmRopNrCellDu.getMoRopId().setFdn("fdn-1");
        expectedPmRopNrCellDu.setPmMacTimeUlResUe(-1.0);
        assertEquals(Double.NaN, actualPmRopNRCellDU.getAvgDeltaIpN());
        assertEquals(Double.NaN, actualPmRopNRCellDU.getAvgUlUeTp());
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));
    }

    private List<ConsumerRecord<String, byte[]>> createConsumerRecords(int numberToCreate) {
        final List<GenericRecord> genericRecordList = createProducerMessages(numberToCreate).stream()
            .filter(Optional::isPresent)
            .map(gro -> gro.get())
                .collect(Collectors.toList());
        return TestUtils.of().getConsumerRecords(kafkaProducer, genericRecordList, schemaRegistryUrl);
    }

    private void setInvalidRoptime(final List<Optional<GenericRecord>> genericRecordList, int recordIndex) {
        Optional<GenericRecord> gro = genericRecordList.get(recordIndex);
        gro.ifPresent(gr -> {
            gr.put(ROP_END_TIME, "");
        });
    }

    private void setSuspectFlag(final List<Optional<GenericRecord>> genericRecordList, int recordIndex) {
        Optional<GenericRecord> gro = genericRecordList.get(recordIndex);
        gro.ifPresent(gr -> {
            gr.put(SUSPECT, true);
        });
    }

    private void setRopTimeZero(final List<Optional<GenericRecord>> genericRecordList, int recordIndex) {
        Optional<GenericRecord> gro = genericRecordList.get(recordIndex);
        gro.ifPresent(gr -> {
            gr.put(ROP_END_TIME, "0");
        });
    }

    private void setRopTimeBadFormat(final List<Optional<GenericRecord>> genericRecordList, int recordIndex) {
        Optional<GenericRecord> gro = genericRecordList.get(recordIndex);
        gro.ifPresent(gr -> {
            gr.put(ROP_END_TIME, "0000-00-00T03:45:00Z");
        });
    }

    private void setGenericRecordEmpty(final List<Optional<GenericRecord>> genericRecordList, int recordIndex) {
        genericRecordList.remove(recordIndex);
        genericRecordList.add(Optional.empty());
    }

    private List<Message<GenericRecord>> buildMessages(List<Optional<GenericRecord>> genericRecordList, Map<String, BadHeaders> bhmap) {
        Set<String> badHeaderNodes = bhmap.keySet();

        List<Message<GenericRecord>> messages = new ArrayList<>();
        genericRecordList.forEach(optGenericRecord -> {
            GenericRecord nrCellDuPmCtrsGr = optGenericRecord.get();
            if (badHeaderNodes.contains(nrCellDuPmCtrsGr.get(NODE_FDN))) {
                messages.add(buildBadMessage(nrCellDuPmCtrsGr, bhmap.get(nrCellDuPmCtrsGr.get(NODE_FDN))));
            } else {
                messages.add(buildGoodMessage(nrCellDuPmCtrsGr));
            }
        });
        return messages;
    }

    private Message<GenericRecord> buildGoodMessage(GenericRecord nrCellDuPmCtrsGr) {
        int partition = 1;
        String schemaName = nrCellDuPmCtrsGr.getSchema().getName();
        String schemaNameWithoutVersion = schemaName.substring(0, schemaName.lastIndexOf('_'));
        int schemaIdValue = 1;

        return MessageBuilder.withPayload(nrCellDuPmCtrsGr)
            .setHeader(KafkaHeaders.MESSAGE_KEY, NODE_FDN)
            .setHeader(KafkaHeaders.TOPIC, counterParserTopicName)
            .setHeader(KafkaHeaders.PARTITION_ID, partition)
            .setHeader(NODE_FDN, nrCellDuPmCtrsGr.get(NODE_FDN))
            .setHeader(ELEMENT_TYPE, nrCellDuPmCtrsGr.get(ELEMENT_TYPE))
            .setHeader(MO_TYPE, schemaNameWithoutVersion)
            .setHeader(SCHEMA_ID, schemaIdValue)
            .build();

    }

    private Message<GenericRecord> buildBadMessage(GenericRecord nrCellDuPmCtrsGr, BadHeaders bh) {
        int partition = 1;
        String schemaName = nrCellDuPmCtrsGr.getSchema().getName();
        String schemaNameWithoutVersion = schemaName.substring(0, schemaName.lastIndexOf('_'));
        int schemaIdValue = 1;

        Map<String, Object> kv = new HashMap<>();
        kv.put(NODE_FDN, nrCellDuPmCtrsGr.get(NODE_FDN));
        kv.put(ELEMENT_TYPE, nrCellDuPmCtrsGr.get(ELEMENT_TYPE));
        kv.put(MO_TYPE, schemaNameWithoutVersion);
        kv.put(SCHEMA_ID, schemaIdValue);

        final MessageBuilder<GenericRecord> messageBuilder = MessageBuilder.withPayload(nrCellDuPmCtrsGr)
            .setHeader(KafkaHeaders.MESSAGE_KEY, nrCellDuPmCtrsGr.get(NODE_FDN))
            .setHeader(KafkaHeaders.TOPIC, counterParserTopicName)
            .setHeader(KafkaHeaders.PARTITION_ID, partition);

        MESSAGE_HEADER_KEYS.forEach(headerKey -> {
            if (bh.getHeaderKeyToUpdate().equals(headerKey)) {
                if (bh.addBadValue) {
                    messageBuilder.setHeader(headerKey, "");
                }
            } else {
                messageBuilder.setHeader(headerKey, kv.get(headerKey));
            }
        });
        return messageBuilder.build();

    }
    private void testRop(PmRopLoader<PmRopNRCellDU> pmRopLoader, String customerId, long ts, CounterValues cvBefore, int expectedNumRecords)
        throws InterruptedException {
        Mockito.when(bdrConfiguration.getBucket()).thenReturn(BUCKET);
        Mockito.when(bdrClient.getObjectInputStream(Mockito.anyString(), Mockito.anyString())).thenAnswer(i -> {
            File file = TestUtils.getfile((String) i.getArguments()[1]);
            return new FileInputStream(file);
        });
        pmRopLoader.loadPmRop(ts, customerId);
        waitforConsumer(expectedNumRecords, 30000, cvBefore);
    }

    private void testOffsetsOK(long summedOffsetsBefore, CounterValues cvBefore) throws Exception {
        CounterValues cvNow = getCounterParserMetricValues();
        double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
        double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
        long expectedOffset = summedOffsetsBefore + (long) numRecordProcessed + (long) numRecordDropped;

        await().atMost(Duration.of(CONSUMER_SEND_WAIT_MS, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            long summedOffsetsNow = getTotalNumberRecordsAcknowledged();
            assertEquals(expectedOffset, summedOffsetsNow);
        });
    }
    private long getTotalNumberRecordsAcknowledged() throws Exception {
        Map<String, Map<Integer, Long>> allTopicsAllMetaMap = getTopicDetails();
        Map<Integer, Long> nrTopicMap = allTopicsAllMetaMap.get(counterParserTopicName);
        return nrTopicMap.values().stream().mapToLong(Long::longValue).sum();
    }

    // Put an await around the metrics received after test, if these are as expected, then next test depending on the metrics will be operating on stable metrics
    // as no consumer thread will be 'late' with the metrics.
    private void assertMetrics(CounterValues cvBefore, int expNumberReceived, int expNumberProcessed, int expNumberDropped,
                               int excectedNumberInValidMoType) {
        await().atMost(Duration.of(CONSUMER_SEND_WAIT_MS, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(1000)).untilAsserted(() -> {
            CounterValues cvNow = getCounterParserMetricValues();
            double numBatchesReceived = cvNow.numberBatchesReceived - cvBefore.numberBatchesReceived;
            double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
            double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
            double numRecordReceived = cvNow.numberRecordsReceived - cvBefore.numberRecordsReceived;
            double numRecordsInvalidMoType = cvNow.numRecordsInvalidMoType - cvBefore.numRecordsInvalidMoType;
            assertThat(numRecordReceived).isEqualTo(expNumberReceived);
            assertThat(numRecordProcessed).isEqualTo(expNumberProcessed);
            assertThat(numRecordDropped).isEqualTo(expNumberDropped);
            assertThat(numRecordsInvalidMoType).isEqualTo(excectedNumberInValidMoType);
            verify(pmRopNrCellDuRepo, times((int) numBatchesReceived)).saveAll(pmRopNRCellDUArgumentCaptor.capture());
        });
    }

    private void verifyTestRop(int expNumberProcessed) {
        PmRopNRCellDU actualPmRopNRCellDU = getActualRop("fdn-0", expNumberProcessed);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        assertThat(actualPmRopNRCellDU).usingRecursiveComparison().isEqualTo(expectedPmRopNrCellDu);
    }

    private PmRopNRCellDU getActualRop(String fdn, int expNumberProcessed) {
        List<PmRopNRCellDU> pmRopNRCellDUList = pmRopNRCellDUArgumentCaptor.getAllValues()
            .stream()
            .flatMap(List::stream)
                .collect(Collectors.toList());
        assertThat(pmRopNRCellDUList).hasSize(expNumberProcessed);
        List<PmRopNRCellDU> pmRopNRCellDUListFdn0 = pmRopNRCellDUList.stream()
            .filter(pmRopNRCellDU -> pmRopNRCellDU.getMoRopId().getFdn().equals(fdn))
                .collect(Collectors.toList());
        assertThat(pmRopNRCellDUListFdn0).hasSize(1);
        PmRopNRCellDU actualPmRopNRCellDU = pmRopNRCellDUListFdn0.get(0);
        return actualPmRopNRCellDU;
    }

    private List<Optional<GenericRecord>> createProducerMessages(int numberRecordsToCreate) {
        return TestUtils.of().createGenericRecords(kafkaProducer, numberRecordsToCreate);
    }

    private void consumeGeneriRecord(final List<Optional<GenericRecord>> genericRecordList, CounterValues cvBefore) {
        sendToInputTopic(genericRecordList);
        waitforConsumer(genericRecordList.size(), 20000, cvBefore);
    }

    private void consumeMessage(final List<Message<GenericRecord>> messageList, CounterValues cvBefore) {
        resetNumberOfRecordsSent();
        kafkaProducer.sendKafkaMessage(messageList);
        await().atMost(Duration.of(PRODUCER_SEND_WAIT_MS, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(1000)).untilAsserted(() -> {
            assertEquals(messageList.size(), kafkaProducer.getNumberRecordsSent().get());
        });
        waitforConsumer(messageList.size(), 20000, cvBefore);
    }

    private void waitforConsumer(final int expectedNumberRecords, int timeToWaitMs, CounterValues cvBefore) {
        await().atMost(Duration.of(timeToWaitMs, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(1000)).untilAsserted(() -> {
            CounterValues cvNow = getCounterParserMetricValues();
            double numRecordProcessed = cvNow.numberRecordsProcessed - cvBefore.numberRecordsProcessed;
            double numRecordDropped = cvNow.numberRecordsDropped - cvBefore.numberRecordsDropped;
            assertEquals(expectedNumberRecords, ((int) (numRecordProcessed + numRecordDropped)));
        });
    }

    private void sendToInputTopic(final List<Optional<GenericRecord>> genericRecordList) {
        sendToInputTopic(genericRecordList, 0);
    }

    private void sendToInputTopic(final List<Optional<GenericRecord>> genericRecordList, int numEmptyRecords) {
        resetNumberOfRecordsSent();
        kafkaProducer.sendKafkaGenericRecord(genericRecordList.stream().flatMap(Optional::stream).collect(Collectors.toList()));
        await().atMost(Duration.of(PRODUCER_SEND_WAIT_MS, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(1000)).untilAsserted(() -> {
            assertEquals(genericRecordList.size() - numEmptyRecords, kafkaProducer.getNumberRecordsSent().get());
        });
    }

    // Test Stability: Parallel test execution may be affecting metric parameters;
    // If there continues to be problems with kafka testing and intermittent failures, then look at updating maven-surefire version.
    // it really old and has parallel execution set to method and forkMode = pertest, but threads could be re-used...
    // REF: https://maven.apache.org/surefire/maven-surefire-plugin/examples/class-loading.html
    //
    // Latest surefire is 3.0.0-M9
    // Let make sure only the correct number of records are sent. ensure 'numberRecordsSent' is reset before starting to send.
    private void resetNumberOfRecordsSent() {
        await().atMost(Duration.of(PRODUCER_SEND_WAIT_MS, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(1000)).untilAsserted(() -> {
            kafkaProducer.getNumberRecordsSent().getAndSet(0);
            assertEquals(0, kafkaProducer.getNumberRecordsSent().get());
        });
    }


    private Map<String, Map<Integer, Long>> getTopicDetails() throws Exception {
        final Set<String> topics = embeddedKafkaBroker.getTopics();
        final int numberPartritions = embeddedKafkaBroker.getPartitionsPerTopic();
        final Map<String, Map<Integer, Long>> topicDetails = new HashMap<>();
        for (final String myTopicName : topics) {
            final Map<Integer, Long> partitionDetails = new HashMap<>();
            for (int pNo = 0; pNo < numberPartritions; pNo++) {
                final OffsetAndMetadata kakfaOffset = KafkaTestUtils.getCurrentOffset(brokerAddresses, group, myTopicName, pNo);
                if (kakfaOffset != null) {
                    partitionDetails.put(pNo, kakfaOffset.offset());
                    log.debug("kafakUtils topic {}, partition {}, offset = {}, metaData = {} ", myTopicName, pNo, kakfaOffset.offset(),
                        kakfaOffset.toString());
                }
            }
            topicDetails.put(myTopicName, partitionDetails);
        }
        return topicDetails;
    }

    private CounterValues getCounterParserMetricValues() {
        return new CounterValues(counterParserTopicListenerRim.getKafkaNumberBatchesReceived().count(),
            counterParserTopicListenerRim.getKafkaNumberRecordsReceived().count(),
            counterParserTopicListenerRim.getKafkaNumberRecordsProcessed().count(),
            counterParserTopicListenerRim.getKafkaNumberRecordsDropped().count(),
            counterParserTopicListenerRim.getKafkaNumberRecordsInvalidMoType().count());
    }

    private void loadCsvPmTestRopPerfromTest(boolean usePreCalculatedInCsv) throws Exception, InterruptedException {
        PmRopLoader<PmRopNRCellDU> pmRopLoader = new PmRopLoader<>(PmRopNRCellDU.class,
            kafkaProducer,
            bdrClient,
            bdrConfiguration,
            counter,
            fileTracker,
            appDataConfig);

        //Precalculated
        Mockito.when(appDataConfig.isUsePreCalculatedAvgDeltaIpN()).thenReturn(usePreCalculatedInCsv);
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "appDataConfig", appDataConfig);

        String filename = ROP_PATH.resolve(CSV_ROP).toString();
        log.info("loadCsvPmTestRopSuccess : Looking for ROP {}", filename);
        File file = TestUtils.getfile(filename);
        log.info("loadCsvPmTestRopSuccess : found ROP {}", file.getAbsoluteFile());

        int numRecordsInRop = TestUtils.numRecordsInZipFile(file.getAbsolutePath());
        long summedOffsetsBefore = getTotalNumberRecordsAcknowledged();
        CounterValues cvBefore = getCounterParserMetricValues();
        testRop(pmRopLoader, CUSTOMER_ID_200238, CSV_ROP_TS_1659568500000L, cvBefore, numRecordsInRop);
        counterParserTopicListenerRim.getLogs();
        testOffsetsOK(summedOffsetsBefore, cvBefore);
        assertMetrics(cvBefore, numRecordsInRop, numRecordsInRop, 0, 0);
        // Check that consumer received the correct contents (Verify line).
        String fdn = "SubNetwork=OMCENM01,MeContext=G10056,ManagedElement=G10056,GNBDUFunction=1,NRCellDU=Q10056A";
        PmRopNRCellDU actualPmRopNRCellDU = getActualRop(fdn, numRecordsInRop);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().getPmRopNrCellDUFirstLineFromPmNRCellDU_200238_1659568500000();
        if (usePreCalculatedInCsv) {
            expectedPmRopNrCellDu.setAvgDeltaIpN(expectedPmRopNrCellDu.getAvgDeltaIpNPreCalculated());
            expectedPmRopNrCellDu.setUsePreCalculatedInCsv(true);
        }
        assertThat(actualPmRopNRCellDU).usingRecursiveComparison().withComparatorForType(Double::compare ,Double.class).isEqualTo(expectedPmRopNrCellDu);
        assertEquals(3099.3338181818, actualPmRopNRCellDU.getAvgUlUeTp(), 0.0000001);
        if (usePreCalculatedInCsv) {
            assertEquals(0.1, actualPmRopNRCellDU.getAvgDeltaIpN(), 0.0000001);
        } else {
            assertEquals(1.00000000003673E-05, actualPmRopNRCellDU.getAvgDeltaIpN(), 0.0000001);
        }
    }

    @ToString
    @AllArgsConstructor
    class CounterValues {
        private double numberBatchesReceived;
        private double numberRecordsReceived;
        private double numberRecordsProcessed;
        private double numberRecordsDropped;
        private double numRecordsInvalidMoType;
    }

    @ToString
    @Getter
    @AllArgsConstructor
    class BadHeaders {
        private String nodeName;
        private String headerKeyToUpdate;
        private boolean addBadValue;
    }
}

