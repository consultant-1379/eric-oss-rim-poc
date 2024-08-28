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

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN2;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.getCellRelationChange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ericsson.oss.apps.classification.CellRelationService;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.MitigationReporterTestParent;
import com.ericsson.oss.apps.data.collection.features.report.mobility.MobilityReportRecord;
import com.ericsson.oss.apps.data.collection.features.report.mobility.MobilityReportingStatus;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

class MobilityMitigationReporterTest extends MitigationReporterTestParent {

    @Mock
    CellRelationService cellRelationService;

    @InjectMocks
    MobilityMitigationReporter mobilityMitigationReporter;

    @Captor
    ArgumentCaptor<List<MobilityReportRecord>> reportArgumentCaptor;
    @Captor
    ArgumentCaptor<String> reportNameArgumentCaptor;

    private final static int DELTA_FOR_TESTING = 10;

    NRCellCU neighborCellCu = new NRCellCU(FDN2);

    @BeforeEach
    void setup() {
        context = new FeatureContext(ROP_TIME_STAMP);
    }

    /**
     * given
     * there is a mitigation MobilityReportingStatus in context for a cell
     * cell has feature data in context
     * cell has connectedComponent data
     * neighbor cell can be resolved
     * neighbor cell has feature data in context
     * <p>
     * when
     * the handler is run
     * then
     * report saver is invoked with a victim cell and the correct mapped parameters
     * report saver is invoked with a neighbor cell and the correct mapped parameters
     */
    @Test
    void testChangeReportWithLookupData() {
        withTargetCellDu(true)
                .withChangeReport()
                .withFtRopNRCellDU(FDN1, 0)
                .withFtRopNRCellDU(FDN2, DELTA_FOR_TESTING);
        mobilityMitigationReporter.handle(context);
        verify(reportSaver, times(1)).createReport(reportArgumentCaptor.capture(), ropArgumentCaptor.capture(), reportNameArgumentCaptor.capture());
        verifyReportRecordContent();
    }

    /**
     * given
     * there is a mitigation MobilityReportingStatus in context for a cell
     * cell has no feature data in context
     * cell has no connectedComponent data
     * neighbor cell cannot be resolved
     * <p>
     * when
     * the handler is run
     * then
     * report saver is invoked with the correct mapped parameters
     */
    @Test
    void testChangeReportNoData() {
        withTargetCellDu(false)
                .withChangeReport()
                .withFtRopNRCellDU(FDN1 + "fail_match", 0);
        mobilityMitigationReporter.handle(context);
        verify(reportSaver, times(1)).createReport(reportArgumentCaptor.capture(), ropArgumentCaptor.capture(), reportNameArgumentCaptor.capture());
        verifyChangeStatusReport(false);
    }

    private void verifyReportRecordContent() {
        MobilityReportRecord mobilityReportRecord = verifyChangeStatusReport(true);
        verifyConnectedComponent(mobilityReportRecord.getCellReportRecord());
        verifyFeatureData(mobilityReportRecord.getCellReportRecord(), 0);
        assertEquals(FDN1, mobilityReportRecord.getMobilityReportingStatus().getSourceRelationFdn());
        assertEquals(FDN2, mobilityReportRecord.getMobilityReportingStatus().getTargetRelationFdn());
        assertEquals(mobilityReportRecord.getCellReportRecord().getAvgSw8AvgDeltaIpN() + DELTA_FOR_TESTING, mobilityReportRecord.getNeighborAvgSw8AvgDeltaIpN());
    }

    @NotNull
    private MobilityReportRecord verifyChangeStatusReport(boolean hasTargetCellDu) {
        MobilityReportRecord mobilityReportRecord = reportArgumentCaptor.getValue().get(0);
        MobilityReportingStatus cellReport = mobilityReportRecord.getMobilityReportingStatus();
        assertEquals(hasTargetCellDu ? FDN2 : null, cellReport.getNeighborCellFdn());
        assertEquals(context.getMobilityReportingStatusList().get(0), cellReport);
        return mobilityReportRecord;
    }

    private MobilityMitigationReporterTest withTargetCellDu(boolean hasTargetCellDu) {
        when(cellRelationService.getCellDUByCellCU(neighborCellCu)).thenReturn(hasTargetCellDu ? Optional.of(new NRCellDU(FDN2)) : Optional.empty());
        return this;
    }

    private MobilityMitigationReporterTest withChangeReport() {
        CellRelationChange cellRelationChange = getCellRelationChange();
        MobilityReportingStatus mobilityReportingStatus = new MobilityReportingStatus(ROP_TIME_STAMP, FDN1, cellRelationChange);
        context.getMobilityReportingStatusList().add(mobilityReportingStatus);
        return this;
    }

}