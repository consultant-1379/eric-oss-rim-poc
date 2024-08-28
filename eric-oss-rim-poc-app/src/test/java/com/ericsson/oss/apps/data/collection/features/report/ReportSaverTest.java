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
package com.ericsson.oss.apps.data.collection.features.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.config.ReportingConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.writer.ReportStats;
import com.ericsson.oss.apps.utils.TestUtils;
import com.google.common.util.concurrent.AtomicDouble;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ReportSaverTest {
    @InjectMocks
    private ReportSaver reportSaver;
    @Mock
    private BdrClient bdrClient;
    @Mock
    private BdrConfiguration bdrConfiguration;
    @Mock
    private ReportingConfig reportingConfig;

    @Captor
    private ArgumentCaptor<byte[]> baisArgumentCaptor;

    private static final String EXPECTED_LINE = "\"NaN\",\"NaN\",\"%.1f\",\"0.0\",\"fdn1\",\"fdn2\",\"NaN\",\"0.0\",\"0.0\",\"123456789\",\"0.0\"";
    private static final String HEADER = "\"AGGRESSORSCORE\",\"AZIMUTHAFFINITY\",\"DISTANCE\",\"DUCTSTRENGTH\",\"FDN1\",\"FDN2\",\"FREQUENCYOVERLAP\",\"GUARDDISTANCE\",\"GUARDOVERDISTANCE\",\"ROPTIME\",\"TDDOVERLAP\"";
    static List<FtRopNRCellDU> ftRopNRCellDuList = new ArrayList<>();
    static List<FtRopNRCellDUPair> ftRopNRCellDUPairList = new ArrayList<>();
    static Long ROP_TIME = 123456789L;
    static FeatureContext featureContext = new FeatureContext(ROP_TIME);
    static int index = 0;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(bdrConfiguration.getBucket()).thenReturn("rim");
        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(5000);
        ftRopNRCellDuList.add(new FtRopNRCellDU(new MoRopId("fdn1", ROP_TIME)) {
            private static final long serialVersionUID = 8675031521560423091L;
        });
        ftRopNRCellDUPairList.add(new FtRopNRCellDUPair() {
            private static final long serialVersionUID = -2770572145178530702L;

            {
                setFdn1("fdn1");
                setFdn2("fdn2");
                setRopTime(ROP_TIME);
            }
        });
        featureContext.setFtRopNRCellDUPairs(ftRopNRCellDUPairList);
    }

    @Test
    void ftRopNRCellDuPairReportTest() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter streamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        reportSaver.getStatefulBeanToCsv(streamWriter).write(featureContext.getFtRopNRCellDUPairs());
        streamWriter.flush();
        assertThat(stream.toString(StandardCharsets.UTF_8)).contains(ftRopNRCellDUPairList.get(0).getFdn1())
                .contains(ftRopNRCellDUPairList.get(0).getFdn2())
                .contains("AZIMUTHAFFINITY")
                .contains("FDN1")
                .contains("FDN2")
                .contains("GUARDDISTANCE");
    }

    @Test
    void ftRopNRCellDuReportTest() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter streamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        reportSaver.getStatefulBeanToCsv(streamWriter).write(ftRopNRCellDuList);
        streamWriter.flush();
        assertThat(stream.toString(StandardCharsets.UTF_8)).contains(ftRopNRCellDuList.get(0).getMoRopId().getFdn())
                .contains("CONNECTEDCOMPONENTID")
                .contains("AVGSW2ULUETHROUGHPUT")
                .contains("FDN")
                .contains("VICTIMSCORE");
    }

    @Test
    void createObjectTest() {
        reportSaver.createReport(featureContext.getFtRopNRCellDUPairs(), ROP_TIME, "FtRopNRCellDUPair");
        verify(bdrClient, times(1)).uploadInputStreamObject(anyString(), anyString(), any(byte[].class));
    }

    @Test
    void createSingleReportTest() {
        getFtRopNRCellDuPairList(1000);
        ReportStats owStats = reportSaver.createReport(ftRopNRCellDUPairList, ROP_TIME, "FtRopNRCellDUPair");
        assertEquals(76043, owStats.getTotalNumberBytesWritten());
        assertEquals(1001, owStats.getTotalNumberLinesWritten());
        assertEquals("FtRopNRCellDUPair_Report_" + ROP_TIME + ".csv.gz", owStats.getReportFilename().get(0));
        assertEquals(1, owStats.getNumberBytesWritten().size());
        assertEquals(1, owStats.getNumberLinesWritten().size());
    }

    @Test
    void createMultipleReportsTest() {
        int maxNumberCellPairs = 1000;
        int maxLinesPerReport = 100;
        String reportName = "FtRopNRCellDUPair";

        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(maxLinesPerReport);

        getFtRopNRCellDuPairList(maxNumberCellPairs);
        int expectedNumberReports = (int) Math.ceil(((double) maxNumberCellPairs / (double) maxLinesPerReport));
        ReportStats expectedOwStats = getExpectedOutputWriterStats(maxNumberCellPairs, maxLinesPerReport, expectedNumberReports, reportName);

        ReportStats owStats = reportSaver.createReport(ftRopNRCellDUPairList, ROP_TIME, reportName);

        assertEquals(expectedOwStats.getTotalNumberBytesWritten(), owStats.getTotalNumberBytesWritten());
        assertEquals(4260, owStats.getTotalNumberCompressedBytesWritten());
        assertEquals(expectedOwStats.getTotalNumberLinesWritten(), owStats.getTotalNumberLinesWritten());

        assertEquals(expectedNumberReports, owStats.getNumberBytesWritten().size());
        assertEquals(expectedNumberReports, owStats.getNumberLinesWritten().size());
        assertEquals(expectedNumberReports, owStats.getReportFilename().size());

        assertTrue(TestUtils.of().areListsEqual(expectedOwStats.getReportFilename(), owStats.getReportFilename()));
        assertTrue(TestUtils.of().areListsEqual(expectedOwStats.getNumberBytesWritten(), owStats.getNumberBytesWritten()));
        assertTrue(TestUtils.of().areListsEqual(expectedOwStats.getNumberLinesWritten(), owStats.getNumberLinesWritten()));

        Mockito.verify(bdrClient, times(expectedNumberReports)).uploadInputStreamObject(anyString(), anyString(), baisArgumentCaptor.capture());
        List<byte[]> baisList = baisArgumentCaptor.getAllValues();
        AtomicDouble indexDistance = new AtomicDouble(0);

        List<String> actualLinesList = new ArrayList<>();
        List<String> expectedLinesList = new ArrayList<>();
        baisList.forEach(bais -> {
            byte[] uncompressed = unzipBytes(bais);
            String fileContents = new String(uncompressed, StandardCharsets.UTF_8);
            String[] lines = fileContents.split("\\r?\\n");

            for (String actualLine : lines) {
                if (!actualLine.contains(HEADER)) {
                    String expectedLine = String.format(EXPECTED_LINE, indexDistance.getAndAdd(1.0));
                    actualLinesList.add(actualLine);
                    expectedLinesList.add(expectedLine);
                }
            }
        });
        assertTrue(TestUtils.of().areListsEqual(actualLinesList, expectedLinesList));
    }

    //Negative tests
    @Test
    void createSingleReportRecordListNullTest() {
        ReportStats owStats = reportSaver.createReport(null, ROP_TIME, "FtRopNRCellDUPair");
        assertEquals(0, owStats.getTotalNumberBytesWritten());
        assertEquals(0, owStats.getTotalNumberLinesWritten());
        assertEquals(Collections.emptyList(), owStats.getReportFilename());
        assertEquals(0, owStats.getNumberBytesWritten().size());
        assertEquals(0, owStats.getNumberLinesWritten().size());
    }

    @Test
    void createSingleReportRecordListEmptyTest() {
        ReportStats owStats = reportSaver.createReport(Collections.emptyList(), ROP_TIME, "FtRopNRCellDUPair");
        assertEquals(0, owStats.getTotalNumberBytesWritten());
        assertEquals(0, owStats.getTotalNumberLinesWritten());
        assertEquals(Collections.emptyList(), owStats.getReportFilename());
        assertEquals(0, owStats.getNumberBytesWritten().size());
        assertEquals(0, owStats.getNumberLinesWritten().size());
    }

    @Test
    void isSingleReportRequiredRequestAllInOneFileTest() {
        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(0);
        boolean singleReportRequired = reportSaver.isSingleReportRequired(1000);
        assertTrue(singleReportRequired);
    }

    @Test
    void isSingleReportRequiredMultipleRecordsFalseTest() {
        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(1000);
        boolean singleReportRequired = reportSaver.isSingleReportRequired(1000000);
        assertFalse(singleReportRequired);
    }

    @Test
    void isSingleReportRequiredMultipleRecordsTrueTest() {
        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(1000);
        boolean singleReportRequired = reportSaver.isSingleReportRequired(10);
        assertTrue(singleReportRequired);
    }

    private void getFtRopNRCellDuPairList(int maxNumberCellPairs) {
        ftRopNRCellDUPairList.clear();
        index = 0;
        IntStream.range(0, maxNumberCellPairs).forEach(i -> {
            FtRopNRCellDUPair ftRopNRCellDUPair = new FtRopNRCellDUPair();
            ftRopNRCellDUPair.setFdn1("fdn1");
            ftRopNRCellDUPair.setFdn2("fdn2");
            ftRopNRCellDUPair.setRopTime(ROP_TIME);
            ftRopNRCellDUPair.setDistance(index++);
            ftRopNRCellDUPairList.add(ftRopNRCellDUPair);
        });
    }

    private ReportStats getExpectedOutputWriterStats(int maxNumberCellPairs, int maxLinesPerReport, int expectedNumberReports,
                                                           String reportName) {
        // size  = header length + FtRopNRCellDUPair Length + CR last line
        int reportSizes1 = 152 +(10*74)+(90*75) + 1; //index in setDistance is three or four chars (Ex. 1.0 or 10.0)
        int reportSizes2_10 = 152 + (100*76) +1;  //index in setDistance is five chars (Ex. 110.0)

        ReportStats owStats = new ReportStats();
        IntStream.rangeClosed(1, expectedNumberReports).forEach(i -> {
            owStats.getReportFilename().add(reportName + "_Report_" + ROP_TIME + "_" + i + ".csv.gz");

            if (i == 1) {
                owStats.getNumberBytesWritten().add(reportSizes1);
            } else {
                owStats.getNumberBytesWritten().add(reportSizes2_10);
            }

            if (i != expectedNumberReports) {
                owStats.getNumberLinesWritten().add(maxLinesPerReport + 1); //include header
            }
            else {
                owStats.getNumberLinesWritten().add((maxNumberCellPairs + expectedNumberReports) - owStats.getTotalNumberLinesWritten());
            }
        });

        return owStats;
    }

    private byte[] unzipBytes(byte[] gzipBytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(gzipBytes);
        GZIPInputStream gzipBais;
        try {
            gzipBais = new GZIPInputStream(bais);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int res = 0;
            byte[] buf = new byte[1024];
            while (res >= 0) {
                res = gzipBais.read(buf, 0, buf.length);
                if (res > 0) {
                    baos.write(buf, 0, res);
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Unable to gunzip bytes ", e);
        }
        return new byte[] {};
    }

}
