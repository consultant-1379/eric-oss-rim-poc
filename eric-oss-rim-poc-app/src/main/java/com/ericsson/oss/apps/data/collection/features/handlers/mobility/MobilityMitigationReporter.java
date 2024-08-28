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
package com.ericsson.oss.apps.data.collection.features.handlers.mobility;

import com.ericsson.oss.apps.classification.CellRelationService;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.report.ReportSaver;
import com.ericsson.oss.apps.data.collection.features.report.mobility.MobilityReportRecord;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stitching reports together for mitigation, this class joins the basic information from
 * mobility changes with various KPIs, and hands over the content to
 *
 * @see com.ericsson.oss.apps.data.collection.features.report.ReportSaver
 * <p>
 * for saving on object store.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MobilityMitigationReporter {

    public static final String MOBILITY_MITIGATION = "mobility_mitigation";
    private final ReportSaver reportSaver;
    private final CellRelationService cellRelationService;

    public void handle(FeatureContext context) {
        var mobilityReportRecordList = createChangeReport(context);
        if (!mobilityReportRecordList.isEmpty()) {
            reportSaver.createReport(mobilityReportRecordList, context.getRopTimeStamp(), MOBILITY_MITIGATION);
        }
    }

    private List<MobilityReportRecord> createChangeReport(FeatureContext context) {
        Map<String, FtRopNRCellDU> ftRopNRCellDUMap = context.getFdnToFtRopNRCellDU();
        return context.getMobilityReportingStatusList().stream()
                .peek(mobilityReportingStatus -> cellRelationService.getCellDUByCellCU(mobilityReportingStatus.getNeighborCellCu())
                        .map(NRCellDU::getObjectId)
                        .ifPresent(moid -> mobilityReportingStatus.setNeighborCellFdn(moid.toString())))
                .map(MobilityReportRecord::new)
                .peek(mobilityReportRecord -> {
                    String victimCellFdn = mobilityReportRecord.getMobilityReportingStatus().getVictimCellFdn();
                    // add victim cell KPIs
                    FtRopNRCellDU ftRopNRCellDU = ftRopNRCellDUMap.getOrDefault(victimCellFdn, new FtRopNRCellDU());
                    BeanUtils.copyProperties(ftRopNRCellDU, mobilityReportRecord.getCellReportRecord());
                })
                .peek(mobilityReportRecord ->
                        // add delta IPN for neighbor cell
                        Optional.ofNullable(mobilityReportRecord.getMobilityReportingStatus().getNeighborCellFdn())
                                .map(neighborCellFdn -> ftRopNRCellDUMap.getOrDefault(neighborCellFdn, new FtRopNRCellDU()))
                                .ifPresent(ftRopNRCellDU -> mobilityReportRecord.setNeighborAvgSw8AvgDeltaIpN(ftRopNRCellDU.getAvgSw8AvgDeltaIpN()))
                ).toList();
    }

}
