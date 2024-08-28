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
package com.ericsson.oss.apps.data.collection.features.report.uplinkpower;

import com.ericsson.oss.apps.data.collection.features.report.CellReportRecord;
import com.opencsv.bean.CsvRecurse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class UplinkPowerReportRecord {
    @CsvRecurse
    private final UplinkPowerReportingStatus uplinkPowerReportingStatus;
    @CsvRecurse
    CellReportRecord cellReportRecord = new CellReportRecord();
}
