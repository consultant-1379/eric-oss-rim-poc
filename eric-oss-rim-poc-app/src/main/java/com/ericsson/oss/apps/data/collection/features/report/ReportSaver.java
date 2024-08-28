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

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.config.ReportingConfig;
import com.ericsson.oss.apps.data.writer.OutputStreamWriterWithWriterAccess;
import com.ericsson.oss.apps.data.writer.ReportStats;
import com.ericsson.oss.apps.model.Constants;
import com.google.common.annotations.VisibleForTesting;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * The Class ReportSaver.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportSaver {

    private final BdrClient bdrClient;
    private final BdrConfiguration bdrConfiguration;
    private final ReportingConfig reportingConfig;

    /**
     * Creates the report and upload it to Object Store.
     * If # records > MaxNumberRecordsPerOutputFile, then break up the recordList into chunks each containing
     * MaxNumberRecordsPerOutputFile + 1 ( header). Last one will have the remaining records.
     * The full report name will be indexes if the records are written in multiple files.
     * <p>
     * If all records written to one file, then report name will not be indexed.
     * To write all records to single file, then set MaxNumberRecordsPerOutputFile = -1 or
     * MaxNumberRecordsPerOutputFile > recordList.size().
     *
     * @param <T>        the generic type
     * @param recordList the record list
     * @param ropTime    the rop time
     * @param reportName the report name
     * @return the report statistics.
     */
    public <T> ReportStats createReport(List<T> recordList, long ropTime, String reportName) {
        ReportStats reportStats = new ReportStats();
        if (recordList == null || recordList.isEmpty()) {
            return reportStats;
        }

        boolean singleReportRequired = isSingleReportRequired(recordList.size());

        AtomicInteger reportIdx = new AtomicInteger(0);
        ListUtils.partition(recordList, reportingConfig.getMaxNumberRecordsPerOutputFile()).parallelStream().forEach(subList -> {
            int idx = reportIdx.incrementAndGet();
            String fullReportName = reportName + "_Report_" + ropTime + "_" + idx + ".csv.gz";
            if (singleReportRequired) {
                fullReportName = reportName + "_Report_" + ropTime + ".csv.gz";
            }
            boolean reportGenerated = generateReport(subList, ropTime, reportStats, fullReportName);
            if (!reportGenerated) {
                return;
            }
        });
        logInfoCompleted(recordList, reportStats, reportName);
        return reportStats;
    }

    /**
     * Generate report for each batch of Records in recordSubList.
     *
     * @param <T>              the generic type
     * @param recordSubList    the record list to write out
     * @param ropTime          the rop time
     * @param totalReportStats the confirmed 'total' output stats, used to keep logging information.
     * @param fullReportName   the full report name
     * @return true, if successful
     */
    @SuppressWarnings("unchecked")
    <T> boolean generateReport(List<T> recordSubList, long ropTime, ReportStats totalReportStats, String fullReportName) {
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gzipBaos = new GZIPOutputStream(baos)) {
            OutputStreamWriterWithWriterAccess osw = writeToOutputStreamWriter(recordSubList, gzipBaos);

            Stats inProgressStats = new Stats("Sending", fullReportName, recordSubList.size());
            inProgressStats.numBytesRead = osw.getNumberBytesWritten();
            inProgressStats.numLinesRead = osw.getNumberLinesWritten();
            inProgressStats.numCompressedBytesRead = baos.size();

            writeReport(fullReportName, baos, inProgressStats);

            totalReportStats.getNumberLinesWritten().add(inProgressStats.numLinesRead);
            totalReportStats.getNumberBytesWritten().add(inProgressStats.numBytesRead);
            totalReportStats.getNumberCompressedBytesWritten().add(inProgressStats.numCompressedBytesRead);
            totalReportStats.getReportFilename().add(fullReportName);
            totalReportStats.incrementNumberReports();

            return true;
        } catch (SdkClientException e) {
            log.info("Error connecting to object store, cannot save '{}_Report_{}", fullReportName, ropTime, e);
        } catch (CsvRequiredFieldEmptyException e) {
            log.error("Could not create {} report due to empty field ", fullReportName, e);
        } catch (CsvDataTypeMismatchException e) {
            log.error("Could not create report {} due to data type mismatch ", fullReportName, e);
        } catch (IOException e) {
            log.error("Could not create report {} due to IO writing process ", fullReportName, e);
        }
        return false;
    }

    /**
     * Get the data from the objects and write it to an output stream.
     * <p>
     * Extracted the OutputStreamWriter to force it to close the stream.
     * This is important for multiple output files, to ensure all bytes written to the correct File.
     * and to ensure that the byte written statistics are correct.
     *
     * @param <T>                  the generic type
     * @param recordSubList        the record list to write out
     * @param ropTime              the rop time
     * @param confirmedReportStats the confirmed output stats, used to keep logging information.
     * @param gzipBaos             the zipped stream to write.
     * @return Output Stream Writer With Writer Access stream. Although closed it contains the stats for the final # bytes written
     * @throws IOException
     * @throws CsvDataTypeMismatchException
     * @throws CsvRequiredFieldEmptyException
     */
    @SuppressWarnings("unchecked")
    <T> OutputStreamWriterWithWriterAccess writeToOutputStreamWriter(List<T> recordSubList, OutputStream gzipBaos)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IOException {
        try (OutputStreamWriterWithWriterAccess osw = new OutputStreamWriterWithWriterAccess(gzipBaos, StandardCharsets.UTF_8)) {
            getStatefulBeanToCsv(osw).write(recordSubList);
            osw.flush();
            return osw;
        }
    }

    /**
     * Write the contents of Byte Array Output Stream to Object Store.
     *
     * @param fullReportName the full report name.
     * @param baos           The byte array output stream containing the bytes to output.
     * @param Stats          The in progress statistics.
     */
    private void writeReport(String fullReportName, ByteArrayOutputStream baos, Stats inProgressStats) {

        // highly inefficient, but w/o the the agreement to get the Object sizes before hand (See TODO), toByteArray is the only way to stop nulls getting written to output file
        // If trying to re-use the BAOS buf and avoid toByteArray, Null will be written to output report as baos.getBuf().length > numBytesRead and s3 fills the difference
        // numBytesRead +1 -> baos.getBuf().length with nulls.
        byte[] bytesToUpload = baos.toByteArray();
        inProgressStats.baisBufsize = bytesToUpload.length;
        logInfo(inProgressStats);
        bdrClient.uploadInputStreamObject(bdrConfiguration.getBucket(), "reports/" + fullReportName, bytesToUpload);
    }

    /**
     * Gets the state full bean to csv.
     *
     * @param streamWriter the stream writer
     * @return the state full bean to csv
     */
    @SuppressWarnings("rawtypes")
    @VisibleForTesting
    StatefulBeanToCsv getStatefulBeanToCsv(OutputStreamWriter streamWriter) {
        return new StatefulBeanToCsvBuilder<>(streamWriter)
                .withSeparator(Constants.COMMA.charAt(0))
                .withEscapechar(ICSVWriter.DEFAULT_ESCAPE_CHARACTER)
                .withLineEnd(ICSVWriter.DEFAULT_LINE_END)
                .withOrderedResults(true)
                .build();
    }

    // either U want all records in one file or # records to write is less than the Max # record per file.
    boolean isSingleReportRequired(int recordListSize) {
        return (reportingConfig.getMaxNumberRecordsPerOutputFile() <= 0) || (recordListSize <= reportingConfig.getMaxNumberRecordsPerOutputFile());
    }

    private void logInfo(Stats stats) {
        log.trace("{} {} to the bucket with OutputStremWriter {} -> GZip -> ByteArrayOutputStream size = {} bytes,"
                        + "  BAIS Buf Size = {}, (total) number records to write = {}, number records written (this file only, incl header) {}",
                stats.stage, stats.fullReportName, stats.numBytesRead, stats.numCompressedBytesRead,
                stats.baisBufsize, stats.numRecords, stats.numLinesRead);

    }

    private void logInfoTotal(String stage, int numRecords, ReportStats owStats) {
        log.info("{}, Uploaded {} records in {} reports, with  {} (bytes) -> GZip ->  {} (bytes), number records written (incl header) {}",
                stage, numRecords, owStats.getNumberReports(), owStats.getTotalNumberBytesWritten(), owStats.getTotalNumberCompressedBytesWritten(),
                owStats.getTotalNumberLinesWritten());

    }

    private <T> void logInfoCompleted(List<T> recordList, ReportStats owStats, String reportName) {
        if (owStats.getTotalNumberLinesWritten() > recordList.size()) {
            logInfoTotal("Upload Done for " + reportName, recordList.size(), owStats);
        } else {
            logInfoTotal("Upload Aborted for " + reportName, recordList.size(), owStats);
        }
    }

    /**
     * Just used to hold statistic info while writing output reports.
     */
    @RequiredArgsConstructor
    private class Stats {
        final String stage;
        final String fullReportName;
        final int numRecords;
        int numBytesRead;
        int numLinesRead;
        int numCompressedBytesRead;
        int baisBufsize;
    }

}
