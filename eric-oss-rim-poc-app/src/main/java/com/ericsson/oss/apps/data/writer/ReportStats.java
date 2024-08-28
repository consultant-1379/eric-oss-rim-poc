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
package com.ericsson.oss.apps.data.writer;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The Class OutputWriterStats.
 * This will keep counts of number of bytes and number lines written to Obhect Store fore each file.
 */
@Getter
@Setter
@ToString
public class ReportStats {
    List<Integer> numberLinesWritten = new ArrayList<>();
    List<Integer> numberBytesWritten = new ArrayList<>();
    List<Integer> numberCompressedBytesWritten = new ArrayList<>();
    List<String> reportFilename = new ArrayList<>();
    private int numberReports = 0;

    public int getTotalNumberBytesWritten() {
        return numberBytesWritten.stream().reduce(0, Integer::sum);
    }

    public int getTotalNumberLinesWritten() {
        return numberLinesWritten.stream().reduce(0, Integer::sum);
    }

    public int getTotalNumberCompressedBytesWritten() {
        return numberCompressedBytesWritten.stream().reduce(0, Integer::sum);
    }

    /**
     * Increment number reports.
     */
    public void incrementNumberReports() {
        numberReports++;
    }
}
