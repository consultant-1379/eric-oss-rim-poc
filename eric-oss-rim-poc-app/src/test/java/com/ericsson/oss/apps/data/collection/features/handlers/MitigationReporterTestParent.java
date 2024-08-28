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

package com.ericsson.oss.apps.data.collection.features.handlers;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.buildFtRopNRCellDUInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.report.CellReportRecord;
import com.ericsson.oss.apps.data.collection.features.report.ReportSaver;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * packing common methods for report verification here
 */
@ExtendWith(MockitoExtension.class)
public class MitigationReporterTestParent {

    public static final long ROP_TIME_STAMP = 1;
    @Mock
    protected ReportSaver reportSaver;
    @Captor
    protected ArgumentCaptor<Long> ropArgumentCaptor;
    protected FeatureContext context;

    protected void verifyFeatureData(CellReportRecord cellReportRecord, int deltaForTesting) {
        assertEquals(25 + deltaForTesting, cellReportRecord.getDlRBSymUtil());
        assertEquals(26 + deltaForTesting, cellReportRecord.getVictimScore());
        assertEquals(27 + deltaForTesting, cellReportRecord.getAvgSw2UlUeThroughput());
        assertEquals(22 + deltaForTesting, cellReportRecord.getAvgSw8UlUeThroughput());
        assertEquals(23 + deltaForTesting, cellReportRecord.getAvgSw8AvgDeltaIpN());
        assertEquals(28 + deltaForTesting, cellReportRecord.getUeTpBaseline());
    }

    protected void verifyConnectedComponent(CellReportRecord cellReportRecord) {
        assertEquals(1, cellReportRecord.getConnectedComponentId());
    }

    public MitigationReporterTestParent withFtRopNRCellDU(String cellFdn, int deltaForTesting) {
        FtRopNRCellDU ftRopNRCellDU = buildFtRopNRCellDUInput(cellFdn, 21 + deltaForTesting, 22 + deltaForTesting, 23 + deltaForTesting, 1L);
        ftRopNRCellDU.setDlRBSymUtil(25 + deltaForTesting);
        ftRopNRCellDU.setVictimScore(26D + deltaForTesting);
        ftRopNRCellDU.setAvgSw2UlUeThroughput(27 + deltaForTesting);
        ftRopNRCellDU.setUeTpBaseline(28 + deltaForTesting);
        context.getFdnToFtRopNRCellDU().put(cellFdn, ftRopNRCellDU);
        return this;
    }

}
