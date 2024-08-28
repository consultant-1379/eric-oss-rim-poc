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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.config.ReportingConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.data.writer.ReportStats;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ReportSaverExceptionsTest {

    @Spy
    @InjectMocks
    private ReportSaver reportSaver;
    @Mock
    private BdrClient bdrClient;
    @Mock
    private BdrConfiguration bdrConfiguration;
    @Mock
    private ReportingConfig reportingConfig;


    static List<FtRopNRCellDUPair> ftRopNRCellDUPairList = new ArrayList<>();
    static Long ROP_TIME = 123456789L;
    static int index = 0;


    @Test
    void generateReportTest() {
        int maxNumberCellPairs = 10;
        String reportName = "FtRopNRCellDUPair";
        getFtRopNRCellDuPairList(maxNumberCellPairs);
        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(ftRopNRCellDUPairList.size());
        ReportStats reportStats = new ReportStats();
        boolean reportGenerated = reportSaver.generateReport(ftRopNRCellDUPairList, ROP_TIME, reportStats, reportName);
        assertTrue(reportGenerated);
    }

    @SuppressWarnings("unchecked")
    @Test
    void generateReportThrowsCsvRequiredFieldEmptyExceptionTest() throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IOException {
        int maxNumberCellPairs = 10;
        String reportName = "FtRopNRCellDUPair";
        getFtRopNRCellDuPairList(maxNumberCellPairs);
        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(ftRopNRCellDUPairList.size());
        ReportStats reportStats = new ReportStats();
        Mockito.doThrow(new CsvRequiredFieldEmptyException())
            .when(reportSaver)
            .writeToOutputStreamWriter(any(List.class), any(GZIPOutputStream.class));
        boolean reportGenerated = reportSaver.generateReport(ftRopNRCellDUPairList, ROP_TIME, reportStats, reportName);
        assertFalse(reportGenerated);
    }

    @SuppressWarnings("unchecked")
    @Test
    void generateReportThrowsCsvDataTypeMismatchExceptionTest() throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IOException {
        int maxNumberCellPairs = 10;
        String reportName = "FtRopNRCellDUPair";
        getFtRopNRCellDuPairList(maxNumberCellPairs);
        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(ftRopNRCellDUPairList.size());
        ReportStats reportStats = new ReportStats();
        Mockito.doThrow(new CsvDataTypeMismatchException())
            .when(reportSaver)
            .writeToOutputStreamWriter(any(List.class), any(GZIPOutputStream.class));
        boolean reportGenerated = reportSaver.generateReport(ftRopNRCellDUPairList, ROP_TIME, reportStats, reportName);
        assertFalse(reportGenerated);
    }

    @SuppressWarnings("unchecked")
    @Test
    void generateReportThrowsIOExceptionTest() throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IOException {
        int maxNumberCellPairs = 10;
        String reportName = "FtRopNRCellDUPair";
        getFtRopNRCellDuPairList(maxNumberCellPairs);
        Mockito.lenient().when(reportingConfig.getMaxNumberRecordsPerOutputFile()).thenReturn(ftRopNRCellDUPairList.size());
        ReportStats reportStats = new ReportStats();
        Mockito.doThrow(new IOException())
            .when(reportSaver)
            .writeToOutputStreamWriter(any(List.class), any(GZIPOutputStream.class));
        boolean reportGenerated = reportSaver.generateReport(ftRopNRCellDUPairList, ROP_TIME, reportStats, reportName);
        assertFalse(reportGenerated);
    }
    private void getFtRopNRCellDuPairList(int maxNumberCellPairs) {
        ftRopNRCellDUPairList.clear();
        index = 0;
        IntStream.range(0, maxNumberCellPairs).forEach(i -> {
            FtRopNRCellDUPair ftRopNRCellDUPair = new FtRopNRCellDUPair();
            ftRopNRCellDUPair.setFdn1("fdn1");
            ftRopNRCellDUPair.setFdn2("fdn2");
            ftRopNRCellDUPair.setRopTime(ROP_TIME);
            ftRopNRCellDUPair.setDistance(Double.valueOf(index++));
            ftRopNRCellDUPairList.add(ftRopNRCellDUPair);
        });
    }
}
