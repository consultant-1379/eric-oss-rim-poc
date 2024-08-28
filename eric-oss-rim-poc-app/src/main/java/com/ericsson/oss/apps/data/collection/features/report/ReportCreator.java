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

import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.config.ClusteringConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import com.ericsson.oss.apps.data.collection.features.handlers.mobility.MobilityMitigationReporter;
import com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.UplinkPowerMitigationReporter;
import com.ericsson.oss.apps.data.collection.features.report.nrcelldu.NRCellDUReporter;
import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportCreator implements FeatureHandler<FeatureContext> {

    private final ReportSaver reportSaver;
    private final CellSelectionConfig cellSelectionConfig;
    private final ClusteringConfig clusteringConfig;
    private final NRCellDUReporter nrCellDUReporter;
    private final UplinkPowerMitigationReporter uplinkPowerMitigationReporter;
    private final MobilityMitigationReporter mobilityMitigationReporter;

    @Override
    @Timed
    public void handle(FeatureContext context) {
        long ropTime = context.getRopTimeStamp();
        double minimumConnectedEdgeWeight = clusteringConfig.getMinimumConnectedEdgeWeight();

        reportSaver.createReport(new ArrayList<>(context.getFdnToFtRopNRCellDU().values()), ropTime, "FtRopNRCellDU");
        reportSaver.createReport(getFtNRCellDUPairsAboveMin(context, minimumConnectedEdgeWeight), ropTime, "FtRopNRCellDUPair");
        reportSaver.createReport(List.of(cellSelectionConfig), ropTime, "CellSelectionConfig");
        reportSaver.createReport(filterConnectedComponentCells(context), ropTime, "RI_DetectionReport");
        nrCellDUReporter.handle(context);
        uplinkPowerMitigationReporter.handle(context);
        mobilityMitigationReporter.handle(context);

    }

    List<FtRopNRCellDUPair> getFtNRCellDUPairsAboveMin(FeatureContext context, double minimumConnectedEdgeWeight) {
        if (context.getFtRopNRCellDUPairs() == null || context.getFtRopNRCellDUPairs().isEmpty()) {
            return Collections.emptyList();
        }
        return context.getFtRopNRCellDUPairs()
            .stream()
            .filter(ftRopNRCellDUPairs -> ftRopNRCellDUPairs.getAggressorScore() > minimumConnectedEdgeWeight)
            .collect(Collectors.toList());
    }

    @VisibleForTesting
    List<RIDetectionReportObject> filterConnectedComponentCells(FeatureContext context) {
        return context.getFdnToFtRopNRCellDU().values().stream()
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getConnectedComponentId() != null)
                .map(ftRopNRCellDElement -> new RIDetectionReportObject(
                        ftRopNRCellDElement.getConnectedComponentId(),
                        ftRopNRCellDElement.getPmMacVolUl(),
                        ftRopNRCellDElement.getPmMacVolDl(),
                        ftRopNRCellDElement.getPmMacVolUlResUe(),
                        ftRopNRCellDElement.getPmMacTimeUlResUe(),
                        ftRopNRCellDElement.getMoRopId(),
                        ftRopNRCellDElement.getAvgSw2UlUeThroughput(),
                        ftRopNRCellDElement.getAvgSw8UlUeThroughput(),
                        ftRopNRCellDElement.getAvgSw8AvgDeltaIpN(),
                        ftRopNRCellDElement.getDlRBSymUtil()))
                .toList();
    }

    @Override
    public int getPriority() {
        return 183;
    }
}
