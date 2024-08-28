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

import com.ericsson.oss.apps.data.collection.MoRopId;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvRecurse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class RIDetectionReportObject {
    @CsvBindByName
    final Long connectedComponentId;
    @CsvBindByName
    final double pmMacVolUl;
    @CsvBindByName
    final double pmMacVolDl;
    @CsvBindByName
    final double pmMacVolUlResUe;
    @CsvBindByName
    final double pmMacTimeUlResUe;
    @CsvRecurse
    final MoRopId moRopId;
    @CsvBindByName
    final double avgSw2UlUeThroughput;
    @CsvBindByName
    final double avgSw8UlUeThroughput;
    @CsvBindByName
    final double avgSw8AvgDeltaIpN;
    @CsvBindByName
    final double dlRBSymUtil;
}
