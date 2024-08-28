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

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class CellReportRecord {

    @CsvBindByName()
    private Long connectedComponentId;
    @CsvBindByName
    private double dlRBSymUtil = Double.NaN;
    @CsvBindByName
    private Double victimScore;
    @CsvBindByName
    private double avgSw2UlUeThroughput = Double.NaN;
    @CsvBindByName
    private double avgSw8UlUeThroughput = Double.NaN;
    @CsvBindByName
    private double avgSw8AvgDeltaIpN = Double.NaN;
    @CsvBindByName
    private double ueTpBaseline;

}
