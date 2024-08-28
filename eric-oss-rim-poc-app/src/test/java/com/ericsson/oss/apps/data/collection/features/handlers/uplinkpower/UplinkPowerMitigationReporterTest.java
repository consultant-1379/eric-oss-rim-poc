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

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.MitigationReporterTestParent;
import com.ericsson.oss.apps.data.collection.features.report.uplinkpower.UplinkPowerReportRecord;
import com.ericsson.oss.apps.data.collection.features.report.uplinkpower.UplinkPowerReportingStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;


@ExtendWith(MockitoExtension.class)
class UplinkPowerMitigationReporterTest extends MitigationReporterTestParent {


    @InjectMocks
    UplinkPowerMitigationReporter uplinkPowerMitigationReporter;

    @Captor
    ArgumentCaptor<List<UplinkPowerReportRecord>> reportArgumentCaptor;
    @Captor
    ArgumentCaptor<String> reportNameArgumentCaptor;

    @BeforeEach
    void setup() {
        context = new FeatureContext(ROP_TIME_STAMP);
    }

    /**
     * given
     * there is a mitigation UplinkPowerReportingStatus in context for a victim cell
     * there is a mitigation UplinkPowerReportingStatus in context for a neighbor cell
     * both victim and neighbor have feature data in context
     * both victim and neighbor have connectedComponent data
     * <p>
     * when
     * the handler is run
     * then
     * report saver is invoked with a victim cell and the correct mapped parameters
     * report saver is invoked with a neighbor cell and the correct mapped parameters
     */
    @Test
    void testChangeReportWithLookupData() {
        withChangeReport(FDN1)
                .withChangeReport(FDN2)
                .withFtRopNRCellDU(FDN1, 0)
                .withFtRopNRCellDU(FDN2, 10);
        uplinkPowerMitigationReporter.handle(context);
        verify(reportSaver, times(1)).createReport(reportArgumentCaptor.capture(), ropArgumentCaptor.capture(), reportNameArgumentCaptor.capture());
        verifyReportRecordContent(true);
        verifyReportRecordContent(false);
    }

    /**
     * given
     * there is a mitigation UplinkPowerReportingStatus in context for a victim cell
     * there is a mitigation UplinkPowerReportingStatus in context for a neighbor cell
     * both victim and neighbor have no feature data in context
     * <p>
     * when
     * the handler is run
     * then
     * report saver is invoked with a victim cell and the correct mapped parameters
     * report saver is invoked with a neighbor cell and the correct mapped parameters
     */
    @Test
    void testChangeReportNoData() {
        withChangeReport(FDN1)
                .withChangeReport(FDN2)
                .withFtRopNRCellDU(FDN1 + "fail_match", 0)
                .withFtRopNRCellDU(FDN2 + "fail_match", 10);
        uplinkPowerMitigationReporter.handle(context);
        verify(reportSaver, times(1)).createReport(reportArgumentCaptor.capture(), ropArgumentCaptor.capture(), reportNameArgumentCaptor.capture());
        verifyChangeStatusReport(true);
        verifyChangeStatusReport(false);
    }

    private void verifyReportRecordContent(boolean victimOrNeighbor) {
        UplinkPowerReportRecord uplinkPowerReportRecord = verifyChangeStatusReport(victimOrNeighbor);
        verifyConnectedComponent(uplinkPowerReportRecord.getCellReportRecord());
        verifyFeatureData(uplinkPowerReportRecord.getCellReportRecord(), victimOrNeighbor ? 0 : 10);
    }

    @NotNull
    private UplinkPowerReportRecord verifyChangeStatusReport(boolean victimOrNeighbor) {
        int recordIndex = victimOrNeighbor ? 0 : 1;
        UplinkPowerReportRecord uplinkPowerReportRecord = reportArgumentCaptor.getAllValues().get(0).get(recordIndex);
        UplinkPowerReportingStatus victimCellReport = uplinkPowerReportRecord.getUplinkPowerReportingStatus();
        assertEquals(victimOrNeighbor, uplinkPowerReportRecord.getUplinkPowerReportingStatus().isVictim());
        assertEquals(context.getUplinkPowerReportingStatusList().get(recordIndex), victimCellReport);
        return uplinkPowerReportRecord;
    }


    private UplinkPowerMitigationReporterTest withChangeReport(String cellFdn) {
        UplinkPowerReportingStatus uplinkPowerReportingStatus = UplinkPowerReportingStatus.builder()
                .cellFdn(cellFdn)
                .requesterVictimCellFdn(FDN1)
                .isVictim(FDN1.equals(cellFdn))
                .build();
        context.getUplinkPowerReportingStatusList().add(uplinkPowerReportingStatus);
        return this;
    }

}