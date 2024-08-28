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

import static com.ericsson.oss.apps.utils.PmConstants.SUSPECT;
import static com.ericsson.oss.apps.utils.PmConstants.ROP_END_TIME;
import static com.ericsson.oss.apps.utils.PmConstants.MODIFIED_NRECLLDU_SCHEMA;
import static com.ericsson.oss.apps.utils.PmConstants.SCHEMA_SUBJECT_RIM;
import static com.ericsson.oss.apps.utils.PmConstants.MO_FDN;
import static com.ericsson.oss.apps.utils.PmConstants.NODE_FDN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.oss.apps.data.collection.AppDataConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import com.ericsson.oss.apps.utils.PmConstants;
import com.ericsson.oss.apps.utils.PmRopNrCellDuCreator;
import com.ericsson.oss.apps.utils.TestUtils;
import com.ericsson.oss.apps.utils.TimeConverter;
import com.ericsson.oss.apps.utils.Utils;

import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableAutoConfiguration
@ContextConfiguration(classes = PmRopNrCellDuRepo.class)
@DataJpaTest
class TestConsumer {
    public static final String APP = "app";
    public static final String ERIC_OSS_RIM_POC_APP = "eric.oss.rim.poc.app";
    private static final String VALID_FDN_REGEX_FOR_TEST = "fdn.+";
    private final Logger logger = (Logger) LoggerFactory.getLogger(CounterParserTopicListenerRim.class);
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private String schemaRegistryMockUrl = "mock://testurl";


    @BeforeEach
    public void init() throws RimHandlerException {
        logger.setLevel(Level.TRACE);
    }

    @AfterEach
    public void cleanup() {
        logger.setLevel(Level.INFO);
    }

    @Mock
    PmRopNrCellDuRepo pmRopNrCellDuRepo;

    @Test
    public void createNewPmRopNRCellDUSuccessTest() {
        NRCellDU_GNBDU_1 pmAvroCounter = TestUtils.of().getOnePmAvroCountersRecord();
        log.info(pmAvroCounter.toString());

        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmAvroCounter.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmAvroCounter, ropEndTimeEpoch);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        assertThat(actualPmRopNRCellDU).usingRecursiveComparison().isEqualTo(expectedPmRopNrCellDu);
    }

    @Test
    public void createNewPmRopNRCellDUPreCalculatedSuccessTest() {
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch, true);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setUsePreCalculatedInCsv(true);
        expectedPmRopNrCellDu.setAvgDeltaIpN(expectedPmRopNrCellDu.getAvgDeltaIpNPreCalculated());
        assertThat(actualPmRopNRCellDU).usingRecursiveComparison().isEqualTo(expectedPmRopNrCellDu);
    }

    @Test
    public void createNewPmRopNRCellDUCounterValueNullTest() {
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.getPmCounters().getPmRadioMaxDeltaIpNDistr().setCounterValue(null);
        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setPmRadioMaxDeltaIpNDistr(new ArrayList<>());
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));
    }

    @Test
    public void createNewPmRopNRCellDUInvalidCounterTypeTest() {
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.getPmCounters().getPmMacRBSymUsedPdschTypeA().setCounterType("BLAH");
        pmCounterAvro.getPmCounters().getPmMacTimeUlResUe().setCounterType("BLAH");
        pmCounterAvro.getPmCounters().getPmRadioMaxDeltaIpNDistr().setCounterType("BLAH");

        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setPmMacRBSymUsedPdschTypeA(Long.MIN_VALUE);
        expectedPmRopNrCellDu.setPmMacTimeUlResUe(Double.NaN);
        expectedPmRopNrCellDu.setPmRadioMaxDeltaIpNDistr(new ArrayList<>());
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));

    }

    @Test
    public void createNewPmRopNRCellDUCountersValuesEmptyTest() {
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.getPmCounters().getPmMacRBSymCsiRs().setCounterValue(Long.MIN_VALUE);
        //PmMacTimeUlResUe is a uint64. Defined in PmRopNrCellDu as a double.
        pmCounterAvro.getPmCounters().getPmMacTimeUlResUe().setCounterValue(Long.MIN_VALUE);
        pmCounterAvro.getPmCounters().getPmRadioMaxDeltaIpNDistr().setCounterValue(new ArrayList<>());
        pmCounterAvro.getPmCounters().getPmRadioSymbolDeltaIpnDistr().setCounterValue(new ArrayList<>());
        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setPmMacRBSymCsiRs(Long.MIN_VALUE);
        expectedPmRopNrCellDu.setAvgDeltaIpN(Double.NaN);
        expectedPmRopNrCellDu.setPmMacTimeUlResUe(Double.NaN);
        expectedPmRopNrCellDu.setPmRadioMaxDeltaIpNDistr(new ArrayList<>());
        expectedPmRopNrCellDu.setPmRadioSymbolDeltaIpnDistr(new ArrayList<>());
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));
    }

    @Test
    public void createNewPmRopNRCellDUCountersTypeNullTest() {
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.getPmCounters().getPmMacRBSymUsedPdschTypeA().setCounterType(null);
        pmCounterAvro.getPmCounters().getPmMacTimeUlResUe().setCounterType(null);
        pmCounterAvro.getPmCounters().getPmRadioMaxDeltaIpNDistr().setCounterType(null);

        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setPmMacRBSymUsedPdschTypeA(Long.MIN_VALUE);
        expectedPmRopNrCellDu.setPmMacTimeUlResUe(Double.NaN);
        expectedPmRopNrCellDu.setPmRadioMaxDeltaIpNDistr(new ArrayList<>());
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));
    }

    @Test
    public void createNewPmRopNRCellDUIsValuePresentTest() {
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.getPmCounters().getPmMacRBSymUsedPdschTypeA().setIsValuePresent(false);
        pmCounterAvro.getPmCounters().getPmMacTimeUlResUe().setIsValuePresent(false);
        pmCounterAvro.getPmCounters().getPmRadioMaxDeltaIpNDistr().setIsValuePresent(false);

        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setPmMacRBSymUsedPdschTypeA(Long.MIN_VALUE);
        expectedPmRopNrCellDu.setPmMacTimeUlResUe(Double.NaN);
        expectedPmRopNrCellDu.setPmRadioMaxDeltaIpNDistr(new ArrayList<>());
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));
    }

    @Test
    public void createNewPmRopNRCellDUThrowExceptionTest() {
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.setPmCounters(null);
        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch);
        // All PM Counter fields should be null, just check two fields.
        assertThat(actualPmRopNRCellDU.getPmMacVolUl()).isNull();
        assertThat(actualPmRopNRCellDU.getPmMacRBSymAvailDl()).isNull();
    }

    @Test
    public void createNewPmRopNRCellDUThrowExceptionProcessingCounterTest() {
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.getPmCounters().setPmMacRBSymUsedPdschTypeA(null);
        pmCounterAvro.getPmCounters().setPmMacTimeUlResUe(null);
        pmCounterAvro.getPmCounters().setPmRadioMaxDeltaIpNDistr(null);

        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch);
        PmRopNRCellDU expectedPmRopNrCellDu = TestUtils.of().createPmRopNrCellDU(0);
        expectedPmRopNrCellDu.setPmMacRBSymUsedPdschTypeA(Long.MIN_VALUE);
        expectedPmRopNrCellDu.setPmMacTimeUlResUe(Double.NaN);
        expectedPmRopNrCellDu.setPmRadioMaxDeltaIpNDistr(new ArrayList<>());
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, expectedPmRopNrCellDu));
    }

    @Test
    public void processPmAvroRecordSuccessTest() throws RimHandlerException {
        ConsumerRecord<String, byte[]> cr = getOneConsumerRecord();
        CounterParserTopicListenerRim counterParserTopicListenerRim = setupCounterParserTopicListenerRim(PmConstants.OLD_ROP_100_YEARS);
        CounterParserTopicListenerRim.MetricValues mv = new CounterParserTopicListenerRim.MetricValues();
        Optional<PmRopNRCellDU> pmRopNRCellDUActual = counterParserTopicListenerRim.processPmAvroRecord(cr, mv);
        assertThat(pmRopNRCellDUActual).isPresent();
        assertThat(mv.getNumRecordsDropped().get()).isEqualTo(0);
        counterParserTopicListenerRim.getKad().close();
    }

    @Test
    public void processPmAvroRecordSuspectFlagTest() throws RimHandlerException {
        GenericRecord gr = getOneGenericRecord();
        gr.put(SUSPECT, true);
        ConsumerRecord<String, byte[]> cr = getOneConsumerRecord(gr);
        testAndAssertFailProcessAvroRecord(cr);
    }

    @Test
    public void processPmAvroRecordRopEndTimeEmptyTest() throws RimHandlerException {
        GenericRecord gr = getOneGenericRecord();
        gr.put(ROP_END_TIME, "");
        ConsumerRecord<String, byte[]> cr = getOneConsumerRecord(gr);
        testAndAssertFailProcessAvroRecord(cr);
    }

    @Test
    public void processPmAvroRecordRopEndTimeNegativeTest() throws RimHandlerException {
        GenericRecord gr = getOneGenericRecord();
        gr.put(ROP_END_TIME, "1821-02-24T03:45:00Z");
        ConsumerRecord<String, byte[]> cr = getOneConsumerRecord(gr);
        testAndAssertFailProcessAvroRecord(cr);
    }


    //Test workaround for <Add Jira>
    @Test
    public void processPmAvroRecordValidFdnTest() throws RimHandlerException {
        String validFdnRegex = "SubNetwork.+MeContext.+ManagedElement.+GNBDUFunction.+NRCellDU.+";
        String parserNodeFdn = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR128gNodeBRadio00019,ManagedElement=NR128gNodeBRadio00019";
        String parserMoFdn = "ManagedElement=NR128gNodeBRadio00019,ENodeBFunction=1,NRCellDU=NR128gNodeBRadio00019-1";
        String fullFdn = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR128gNodeBRadio00019,ManagedElement=NR128gNodeBRadio00019,GNBDUFunction=1,NRCellDU=NR128gNodeBRadio00019-1";
        testValidFdn(parserNodeFdn, parserNodeFdn, parserMoFdn, fullFdn, validFdnRegex);
        testValidFdn(fullFdn, fullFdn, parserMoFdn, fullFdn, validFdnRegex);
        testValidFdn(fullFdn, fullFdn, "noValue", fullFdn, validFdnRegex);
        testInValidFdn(fullFdn, "noNodeFdn", parserMoFdn, validFdnRegex);
        testInValidFdn("noNodeFdn", "noNodeFdn", parserMoFdn, validFdnRegex);

        String parserNodeFdnNoMe = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR128gNodeBRadio00019,ManagedEEEEEEEEEEEEEEElement=NR128gNodeBRadio00019";
        String parserMoFdnNoMe = "ManagedEEEEEEEEEEEEEEElement=NR128gNodeBRadio00019,ENodeBFunction=1,NRCellDU=NR128gNodeBRadio00019-1";
        String parserNodeFdnDiffMe = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR128gNodeBRadio00019,ManagedElement=NR128gNodeBRadio00020";
        testInValidFdn(parserNodeFdnNoMe, parserNodeFdnNoMe, parserMoFdn, validFdnRegex);
        testInValidFdn(parserNodeFdn, parserNodeFdn, parserMoFdnNoMe, validFdnRegex);
        testInValidFdn(parserNodeFdnDiffMe, parserNodeFdnDiffMe, parserMoFdn, validFdnRegex);

        String parserNodeFdnNaE = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR128gNodeBRadio00019,ManagedElement=NR128gNodeBRadio00019,NOT_AT_END";
        String parserMoFdnNaS = "NOT_AT_START,ManagedElement=NR128gNodeBRadio00019,ENodeBFunction=1,NRCellDU=NR128gNodeBRadio00019-1";
        testInValidFdn(parserNodeFdnNaE, parserNodeFdnNaE, parserMoFdn, validFdnRegex);
        testInValidFdn(parserNodeFdn, parserNodeFdn, parserMoFdnNaS, validFdnRegex);

        testInValidFdn(parserNodeFdn, parserNodeFdn, "noValue", validFdnRegex);
    }

    private void testInValidFdn(String headerNodeFdn, String recordNodeFdn, String moFdn, String validFdnRegex) throws RimHandlerException {
        ConsumerRecord<String, byte[]> cr = createConsumerRecordWithGivenFdn(headerNodeFdn, recordNodeFdn, moFdn);
        testAndAssertFailProcessAvroRecord(cr, PmConstants.OLD_ROP_100_YEARS, validFdnRegex);
    }

    private void testValidFdn(String headerNodeFdn, String recordNodeFdn, String moFdn, String fullFdn, String validFdnRegex)
        throws RimHandlerException {
        ConsumerRecord<String, byte[]> cr = createConsumerRecordWithGivenFdn(headerNodeFdn, recordNodeFdn, moFdn);
        testAndAssertPassProcessAvroRecord(cr, fullFdn, validFdnRegex);
    }

    private ConsumerRecord<String, byte[]> createConsumerRecordWithGivenFdn(String headerNodeFdn, String recordNodeFdn, String moFdn) {
        GenericRecord gr = getOneGenericRecord();
        gr.put(MO_FDN, moFdn);
        gr.put(NODE_FDN, recordNodeFdn);
        return replaceNodeFdnInConsumerRecordHeader(gr, headerNodeFdn);
    }
    private ConsumerRecord<String, byte[]> replaceNodeFdnInConsumerRecordHeader(GenericRecord gr, String requiredNodeFdnInHeader) {
        ConsumerRecord<String, byte[]> cr = getOneConsumerRecord(gr);
        cr.headers().remove(NODE_FDN);
        cr.headers().add(new RecordHeader(NODE_FDN, requiredNodeFdnInHeader.toString().getBytes(StandardCharsets.UTF_8)));

        Header fdnHeader = cr.headers().headers(NODE_FDN).iterator().next();
        String actualNodeFdnInHeader = new String(fdnHeader.value(), StandardCharsets.UTF_8);
        assertThat(actualNodeFdnInHeader).isEqualTo(requiredNodeFdnInHeader);
        return cr;
    }

    private void testAndAssertPassProcessAvroRecord(ConsumerRecord<String, byte[]> cr, String expectedFullFdn, String validFdnRegex)
        throws RimHandlerException {
        CounterParserTopicListenerRim counterParserTopicListenerRim = setupCounterParserTopicListenerRim(PmConstants.OLD_ROP_100_YEARS, validFdnRegex);
        CounterParserTopicListenerRim.MetricValues mv = new CounterParserTopicListenerRim.MetricValues();
        Optional<PmRopNRCellDU> pmRopNRCellDUActual = counterParserTopicListenerRim.processPmAvroRecord(cr, mv);
        assertThat(pmRopNRCellDUActual).isPresent();
        assertThat(mv.getNumRecordsDropped().get()).isEqualTo(0);
        assertThat(pmRopNRCellDUActual.get().getMoRopId().getFdn()).isEqualTo(expectedFullFdn);

        counterParserTopicListenerRim.getKad().close();
    }

    private void testAndAssertFailProcessAvroRecord(ConsumerRecord<String, byte[]> cr) throws RimHandlerException {
        testAndAssertFailProcessAvroRecord(cr, PmConstants.OLD_ROP_100_YEARS, VALID_FDN_REGEX_FOR_TEST);
    }

    private void testAndAssertFailProcessAvroRecord(ConsumerRecord<String, byte[]> cr, long discardRopsOlderThanMs, String validFdnRegex)
        throws RimHandlerException {
        CounterParserTopicListenerRim counterParserTopicListenerRim = setupCounterParserTopicListenerRim(discardRopsOlderThanMs, validFdnRegex);
        CounterParserTopicListenerRim.MetricValues mv = new CounterParserTopicListenerRim.MetricValues();
        counterParserTopicListenerRim.processPmAvroRecord(cr, mv);
        counterParserTopicListenerRim.getLogs();
        assertThat(mv.getNumRecordsDropped().get()).isEqualTo(1);
        counterParserTopicListenerRim.getKad().close();
    }

    private CounterParserTopicListenerRim setupCounterParserTopicListenerRim(long discardRopsOlderThanMs) throws RimHandlerException {
        return setupCounterParserTopicListenerRim(discardRopsOlderThanMs, VALID_FDN_REGEX_FOR_TEST);
    }

    private CounterParserTopicListenerRim setupCounterParserTopicListenerRim(long discardRopsOlderThanMs, String validFdnRegex)
        throws RimHandlerException {
        Counter kafkaNumberBatchesReceived = meterRegistry.counter("kafka.number.batches.received", APP, ERIC_OSS_RIM_POC_APP);
        Counter kafkaNumberRecordsReceived = meterRegistry.counter("kafka.number.records.received", APP, ERIC_OSS_RIM_POC_APP);
        Counter kafkaNumberRecordsProcessed = meterRegistry.counter("kafka.number.records.processed", APP, ERIC_OSS_RIM_POC_APP);
        Counter kafkaNumberRecordsDropped = meterRegistry.counter("kafka.number.records.dropped", APP, ERIC_OSS_RIM_POC_APP);
        Counter kafkaNumberRecordsInvalidMoType = meterRegistry.counter("kafka.number.records.invalid.mo.type", APP, ERIC_OSS_RIM_POC_APP);
        AppDataConfig appDataConfig = new AppDataConfig();
        KafkaAvroDeserializer kad = Utils.of()
            .getKafkaAvroDeserializer(schemaRegistryMockUrl, SCHEMA_SUBJECT_RIM + MODIFIED_NRECLLDU_SCHEMA, MODIFIED_NRECLLDU_SCHEMA);

        CounterParserTopicListenerRim counterParserTopicListenerRim = new CounterParserTopicListenerRim(kafkaNumberBatchesReceived,
            kafkaNumberRecordsReceived,
            kafkaNumberRecordsProcessed,
            kafkaNumberRecordsDropped,
            kafkaNumberRecordsInvalidMoType,
            pmRopNrCellDuRepo,
            appDataConfig,
            kad);
        CounterParserTopicListenerRim.setPatternValidateFdn(Pattern.compile(validFdnRegex));
        ReflectionTestUtils.setField(counterParserTopicListenerRim, "discardRopsOlderThanMs", discardRopsOlderThanMs);
        Mockito.doReturn(null).when(pmRopNrCellDuRepo).save(Mockito.any());
        return counterParserTopicListenerRim;
    }

    /**
     * Create one GenericRecord.
     *
     */
    private GenericRecord getOneGenericRecord() {
        KafkaProducerRim kp = new KafkaProducerRim();
        ReflectionTestUtils.setField(kp, "partitions", 3);
        ReflectionTestUtils.setField(kp, "counterParserTopicName", "test-consumer-topic");
        List<GenericRecord> genericRecordList = TestUtils.of()
            .createGenericRecords(kp, 1)
            .stream()
            .filter(Optional::isPresent)
            .map(gro -> gro.get())
            .collect(Collectors.toList());
        return genericRecordList.get(0);
    }

    /**
     * Create one ConsumerRecord<String, byte[]> with defined values.
     *
     */
    private ConsumerRecord<String, byte[]> getOneConsumerRecord() {
        KafkaProducerRim kp = new KafkaProducerRim();
        ReflectionTestUtils.setField(kp, "partitions", 3);
        ReflectionTestUtils.setField(kp, "counterParserTopicName", "test-consumer-topic");
        List<GenericRecord> genericRecordList = TestUtils.of()
            .createGenericRecords(kp, 1)
            .stream()
            .filter(Optional::isPresent)
            .map(gro -> gro.get())
            .collect(Collectors.toList());
        List<ConsumerRecord<String, byte[]>> crList = TestUtils.of().getConsumerRecords(kp, genericRecordList, schemaRegistryMockUrl);
        return crList.get(0);
    }

    /**
     * Create one ConsumerRecord<String, byte[]> from input Generic Record.
     *
     */
    private ConsumerRecord<String, byte[]> getOneConsumerRecord(GenericRecord gr) {
        KafkaProducerRim kp = new KafkaProducerRim();
        ReflectionTestUtils.setField(kp, "partitions", 3);
        ReflectionTestUtils.setField(kp, "counterParserTopicName", "test-consumer-topic");
        List<GenericRecord> genericRecordList = new ArrayList<>();
        genericRecordList.add(gr);

        List<ConsumerRecord<String, byte[]>> crList = TestUtils.of().getConsumerRecords(kp, genericRecordList, schemaRegistryMockUrl);
        return crList.get(0);
    }

}
