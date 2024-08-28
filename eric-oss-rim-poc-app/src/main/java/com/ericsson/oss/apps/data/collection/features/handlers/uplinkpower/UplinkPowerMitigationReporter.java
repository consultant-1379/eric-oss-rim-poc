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
package com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower;

import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.report.ReportSaver;
import com.ericsson.oss.apps.data.collection.features.report.uplinkpower.UplinkPowerReportRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UplinkPowerMitigationReporter {

    public static final String UPLINK_POWER_MITIGATION = "uplink_power_mitigation";
    private final ReportSaver reportSaver;

    public void handle(FeatureContext context) {
        List<UplinkPowerReportRecord> victimCellReportRecordList = createChangeReport(context);
        if (!victimCellReportRecordList.isEmpty()) {
            reportSaver.createReport(victimCellReportRecordList, context.getRopTimeStamp(), UPLINK_POWER_MITIGATION);
        }
    }

    private List<UplinkPowerReportRecord> createChangeReport(FeatureContext context) {
        Map<String, FtRopNRCellDU> ftRopNRCellDUMap = context.getFdnToFtRopNRCellDU();
        return context.getUplinkPowerReportingStatusList().stream().map(uplinkPowerReportingStatus -> {
            UplinkPowerReportRecord uplinkPowerReportRecord = new UplinkPowerReportRecord(uplinkPowerReportingStatus);
            FtRopNRCellDU ftRopNRCellDU = ftRopNRCellDUMap.getOrDefault(uplinkPowerReportingStatus.getCellFdn(), new FtRopNRCellDU());
            BeanUtils.copyProperties(ftRopNRCellDU, uplinkPowerReportRecord.getCellReportRecord());
            return uplinkPowerReportRecord;
        }).collect(Collectors.toUnmodifiableList());
    }

}
