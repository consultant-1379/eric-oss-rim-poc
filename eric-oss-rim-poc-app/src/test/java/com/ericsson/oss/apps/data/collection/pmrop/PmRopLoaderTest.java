/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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
import com.ericsson.oss.apps.data.collection.AppDataConfig;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import com.ericsson.oss.apps.utils.TestUtils;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class PmRopLoaderTest {

    private static final String BUCKET = "rim";

    private static final Path ROP_PATH = Path.of("pm", "rop");
    private static final String CSV_ROP = "pm-NRCellDU-200238-1659568500000.csv.gz";
    private static final String CSV_ROP_WITH_PROBLEM_RECORDS = "pm-NRCellDU-000001-1.csv.gz";
    private static final String CSV_ROP_WITH_RADIO_SYMBOL_DISTR = "pm-NRCellDU-000001-2.csv.gz";

    private static final String CUSTOMER_ID_200238 = "200238";
    private static final long CSV_ROP_TS_1659568500000L = 1659568500000L;

    private static final String CUSTOMER_ID_000001 = "000001";
    private static final long CSV_ROP_TS_00000000000001L = 1L;
    private static final long CSV_ROP_TS_00000000000002L = 2L;


    @Autowired
    private FileTracker fileTracker;

    @Mock
    private BdrConfiguration bdrConfiguration;
    @Mock
    private BdrClient bdrClient;

    @Mock
    private AppDataConfig appDataConfig;

    @Mock(lenient = true)
    private Counter counter;

    @Mock
    private PmRopNrCellDuRepo pmRopNrCellDuRepo;
    private PmRopLoader<PmRopNRCellDU> pmRopLoader;

    @Captor
    ArgumentCaptor<PmRopNRCellDU> pmRopNRCellDUArgumentCaptor;

    @BeforeEach
    void setup() {
        pmRopLoader = new PmRopLoader<>(PmRopNRCellDU.class,
                pmRopNrCellDuRepo,
                bdrClient,
                bdrConfiguration,
                counter,
                fileTracker,
                appDataConfig);
    }

    @Test
    void loadCsvPmTestRopSuccess_test() {
        File file = getFile(CSV_ROP);
        int numRecordsInRop = TestUtils.numRecordsInZipFile(file.getAbsolutePath());
        testRop(CUSTOMER_ID_200238, CSV_ROP_TS_1659568500000L, numRecordsInRop);
        var firstRecord = pmRopNRCellDUArgumentCaptor.getAllValues().get(0);
        verifyParsing(firstRecord, false);
    }

    @Test
    void loadCsvPmTestRopPreCalculatedSuccess_test() {
        File file = getFile(CSV_ROP);
        Mockito.when(appDataConfig.isUsePreCalculatedAvgDeltaIpN()).thenReturn(true);
        ReflectionTestUtils.setField(pmRopLoader, "appDataConfig", appDataConfig);
        int numRecordsInRop = TestUtils.numRecordsInZipFile(file.getAbsolutePath());
        testRop(CUSTOMER_ID_200238, CSV_ROP_TS_1659568500000L, numRecordsInRop);
        var firstRecord = pmRopNRCellDUArgumentCaptor.getAllValues().get(0);
        verifyParsing(firstRecord, true);
    }

    @Test
    void loadCsvPmTestProblemRopSuccess_test() {
        File file = getFile(CSV_ROP_WITH_PROBLEM_RECORDS);

        // 10 records in ROP, 9 can be processed, one will throw Exception.
        int numRecordsInRop = TestUtils.numRecordsInZipFile(file.getAbsolutePath());
        testRop(CUSTOMER_ID_000001, CSV_ROP_TS_00000000000001L, numRecordsInRop - 1);
        List<PmRopNRCellDU> actualRecords = pmRopNRCellDUArgumentCaptor.getAllValues();

        testDeltaIpn(actualRecords);
        testPmMacVolDl(actualRecords);
        testPmMacVolUl(actualRecords);
        testPmMacVolUlResUe(actualRecords);
        testPmMacTimeUlResUe(actualRecords);
    }

    @Test
    void loadCsvWithRadioSymbolDistr() {
        File file = getFile(CSV_ROP_WITH_RADIO_SYMBOL_DISTR);
        int numRecordsInRop = TestUtils.numRecordsInZipFile(file.getAbsolutePath());
        testRop(CUSTOMER_ID_000001, CSV_ROP_TS_00000000000002L, numRecordsInRop);
        List<PmRopNRCellDU> actualRecords = pmRopNRCellDUArgumentCaptor.getAllValues();
        testPmRadioSymbolDeltaIpnDistr(actualRecords);
        // aggregated values are left to null if input is nok
        actualRecords.subList(0, actualRecords.size() - 1).forEach(pmRopNRCellDU -> {
            assertEquals(Double.NaN, pmRopNRCellDU.getAvgSymbolDeltaIpn());
            assertEquals(Double.NaN, pmRopNRCellDU.getTotalBinSumSymbolDeltaIpn());
            assertEquals(Double.NaN, pmRopNRCellDU.getPositiveBinSumSymbolDeltaIpn());
        });
        // check aggregated values in happy path
        PmRopNRCellDU okPmRopNRCellDU = actualRecords.get(actualRecords.size() - 1);
        assertEquals(8.085, okPmRopNRCellDU.getAvgSymbolDeltaIpn(), 0.001);
        assertEquals(45, okPmRopNRCellDU.getTotalBinSumSymbolDeltaIpn());
        assertEquals(35, okPmRopNRCellDU.getPositiveBinSumSymbolDeltaIpn());
    }

    @NotNull
    private File getFile(String csvRopWithProblemRecords) {
        Mockito.when(appDataConfig.getAppDataPmRopPath()).thenReturn("pm/rop/");
        String filename = ROP_PATH.resolve(csvRopWithProblemRecords).toString();
        log.info("loadCsvPmTestRopSuccess : Looking for ROP {}", filename);
        File file = TestUtils.getfile(filename);
        log.info("loadCsvPmTestRopSuccess : found ROP {}", file.getAbsoluteFile());
        return file;
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    void atlernativeRopPath_test() {
        Mockito.when(appDataConfig.getAppDataPmRopPath()).thenReturn("pm/alt_rop/");
        String testPath = pmRopLoader.getObjectPath(CSV_ROP_TS_00000000000001L, CUSTOMER_ID_000001);
        assertThat(testPath).info.equals("pm/alt_rop/" + CSV_ROP_TS_00000000000001L + "/" + CUSTOMER_ID_000001);
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    void atlernativeRopPathNoTrailingSlash_test() {
        Mockito.when(appDataConfig.getAppDataPmRopPath()).thenReturn("pm/alt_rop");
        String testPath = pmRopLoader.getObjectPath(CSV_ROP_TS_00000000000001L, CUSTOMER_ID_000001);
        assertThat(testPath).info.equals("pm/alt_rop/" + CSV_ROP_TS_00000000000001L + "/" + CUSTOMER_ID_000001);
    }

    private void testDeltaIpn(List<PmRopNRCellDU> actualRecords) {
        List<List<Long>> expectedPmRadioMaxDeltaIpNDistrList = new ArrayList<>();
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{900L, 0L, 0L, 0L, 0L}).boxed().collect(Collectors.toList()));
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{}).boxed().collect(Collectors.toList()));
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{900L, 0L, 0L, 0L, 0L}).boxed().collect(Collectors.toList()));
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{0L, 22L, 241L, 524L, 113L}).boxed().collect(Collectors.toList()));
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{795L, 104L, 1L, 0L, 0L}).boxed().collect(Collectors.toList()));
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{0L, 0L, 0L, 0L, 0L}).boxed().collect(Collectors.toList())); // Actually Zero's in ROP
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{}).boxed().collect(Collectors.toList()));
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{}).boxed().collect(Collectors.toList()));
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{}).boxed().collect(Collectors.toList()));
        expectedPmRadioMaxDeltaIpNDistrList.add(Arrays.stream(new long[]{900L, 0L, 0L, 0L, 0L}).boxed().collect(Collectors.toList()));

        List<List<Long>> actualPmRadioMaxDeltaIpNDistrList = new ArrayList<>();
        actualRecords.forEach(actualPmRopNRCellDU -> {
            actualPmRadioMaxDeltaIpNDistrList.add(actualPmRopNRCellDU.getPmRadioMaxDeltaIpNDistr());
        });
        assertThat(actualPmRadioMaxDeltaIpNDistrList).usingRecursiveComparison().isEqualTo(expectedPmRadioMaxDeltaIpNDistrList);
    }

    private void testPmRadioSymbolDeltaIpnDistr(List<PmRopNRCellDU> actualRecords) {
        List<List<Long>> expectedPmRadioSymbolDeltaIpnDistrList = new ArrayList<>();
        expectedPmRadioSymbolDeltaIpnDistrList.add(new ArrayList<>());
        expectedPmRadioSymbolDeltaIpnDistrList.add(new ArrayList<>());
        expectedPmRadioSymbolDeltaIpnDistrList.add(new ArrayList<>());
        expectedPmRadioSymbolDeltaIpnDistrList.add(new ArrayList<>());
        expectedPmRadioSymbolDeltaIpnDistrList.add(Arrays.stream(new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9}).boxed().collect(Collectors.toList()));
        List<List<Long>> actualPmRadioMaxDeltaIpNDistrList = new ArrayList<>();
        actualRecords.forEach(actualPmRopNRCellDU -> {
            actualPmRadioMaxDeltaIpNDistrList.add(actualPmRopNRCellDU.getPmRadioSymbolDeltaIpnDistr());
        });
        assertThat(actualPmRadioMaxDeltaIpNDistrList).usingRecursiveComparison().isEqualTo(expectedPmRadioSymbolDeltaIpnDistrList);
    }

    private void testPmMacVolDl(List<PmRopNRCellDU> actualRecords) {
        List<Double> expectedValueList = DoubleStream
                .of(797771.0, Double.NaN, Double.NaN, 2.309632E7, 1.833113326E9, 0.0, 4.7169432E7, 9111427.0, 7281232.0, 797771.0)
                .boxed()
                .collect(Collectors.toList());
        List<Double> actualValueList = new ArrayList<>();
        actualRecords.forEach(actualPmRopNRCellDU -> {
            actualValueList.add(actualPmRopNRCellDU.getPmMacVolDl());
        });
        actualValueList.removeAll(expectedValueList);
        assertThat(actualValueList.size()).isZero();
    }

    private void testPmMacVolUl(List<PmRopNRCellDU> actualRecords) {
        List<Double> expectedValueList = DoubleStream
                .of(103454.0, Double.NaN, Double.NaN, 857108.0, 2.7479504E7, 0.0, 5513326.0, 363777.0, 101701.0, 103454.0)
                .boxed()
                .collect(Collectors.toList());
        List<Double> actualValueList = new ArrayList<>();
        actualRecords.forEach(actualPmRopNRCellDU -> {
            actualValueList.add(actualPmRopNRCellDU.getPmMacVolUl());
        });
        actualValueList.removeAll(expectedValueList);
        assertThat(actualValueList.size()).isZero();
    }

    private void testPmMacVolUlResUe(List<PmRopNRCellDU> actualRecords) {
        List<Double> expectedValueList = DoubleStream
                .of(2130792.0, Double.NaN, Double.NaN, 2130792.0, 2130792.0, 0.0, 2130792.0, 2130792.0, 2130792.0, 2130792.0)
                .boxed()
                .collect(Collectors.toList());
        List<Double> actualValueList = new ArrayList<>();
        actualRecords.forEach(actualPmRopNRCellDU -> {
            actualValueList.add(actualPmRopNRCellDU.getPmMacVolUlResUe());
        });
        actualValueList.removeAll(expectedValueList);
        assertThat(actualValueList.size()).isZero();
    }

    private void testPmMacTimeUlResUe(List<PmRopNRCellDU> actualRecords) {
        List<Double> expectedValueList = DoubleStream.of(44000.0, Double.NaN, Double.NaN, 44000.0, 44000.0, 0.0, 44000.0, 44000.0, 44000.0, 44000.0)
                .boxed()
                .collect(Collectors.toList());
        List<Double> actualValueList = new ArrayList<>();
        actualRecords.forEach(actualPmRopNRCellDU -> {
            actualValueList.add(actualPmRopNRCellDU.getPmMacTimeUlResUe());
        });
        actualValueList.removeAll(expectedValueList);
        assertThat(actualValueList.size()).isZero();
    }

    private void testRop(String customerId, long ts, int expectedNumRecords) {
        Mockito.when(bdrConfiguration.getBucket()).thenReturn(BUCKET);
        Mockito.when(bdrClient.getObjectInputStream(Mockito.anyString(), Mockito.anyString())).thenAnswer(i -> {
            File file = TestUtils.getfile((String) i.getArguments()[1]);
            return new FileInputStream(file);
        });

        pmRopLoader.loadPmRop(ts, customerId);
        verify(pmRopNrCellDuRepo, times(expectedNumRecords)).save(pmRopNRCellDUArgumentCaptor.capture());
        verify(counter).increment(eq((double) expectedNumRecords));
    }

    private void verifyParsing(PmRopNRCellDU firstRecord, boolean usePreCalculated) {
        assertEquals("SubNetwork=OMCENM01,MeContext=G10056,ManagedElement=G10056,GNBDUFunction=1,NRCellDU=Q10056A", firstRecord.getMoRopId().getFdn());
        assertEquals(91126422, firstRecord.getPmMacRBSymUsedPdschTypeA());
        assertEquals(3928134, firstRecord.getPmMacRBSymUsedPdcchTypeA());
        assertEquals(0, firstRecord.getPmMacRBSymUsedPdcchTypeB());
        assertEquals(3786318, firstRecord.getPmMacRBSymUsedPdschTypeABroadcasting());
        assertEquals(45360000, firstRecord.getPmMacRBSymCsiRs());
        assertEquals(3538080000L, firstRecord.getPmMacRBSymAvailDl());
        assertEquals(753864223, firstRecord.getPmMacVolDl());
        assertEquals(12353671, firstRecord.getPmMacVolUl());
        assertEquals(44000, firstRecord.getPmMacTimeUlResUe());
        assertEquals(2130792, firstRecord.getPmMacVolUlResUe());
        assertEquals(3099.333818181818, firstRecord.getAvgUlUeTp(), 0.000001);
        assertEquals(0.1, firstRecord.getAvgDeltaIpNPreCalculated(), 0.000001);
        if (usePreCalculated) {
            assertEquals(0.1, firstRecord.getAvgDeltaIpN(), 0.000001);
        } else {
            assertEquals(1.00000000003673E-05, firstRecord.getAvgDeltaIpN(), 0.000001);
            assertEquals(900, firstRecord.getTotalBinSumMaxDeltaIpn());
        }
        List<Long> expectedPmRadioMaxDeltaIpNDistr = Arrays.stream(new long[]{900L, 0L, 0L, 0L, 0L}).boxed().collect(Collectors.toList());
        assertEquals(expectedPmRadioMaxDeltaIpNDistr, firstRecord.getPmRadioMaxDeltaIpNDistr());
    }
}

