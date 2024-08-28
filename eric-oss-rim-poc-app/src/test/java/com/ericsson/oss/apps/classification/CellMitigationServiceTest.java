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
package com.ericsson.oss.apps.classification;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.buildFtRopNRCellDUInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.repositories.PmBaselineNrCellDuRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class CellMitigationServiceTest {

    @InjectMocks
    private CellMitigationService cellMitigationService;

    @Mock
    PmBaselineNrCellDuRepo pmBaselineNrCellDuRepo;

    @Test
    void testGetCellsAboveDeltaIpnWMAndBelowUETPBaseline() {
        List<FtRopNRCellDU> ftRopNRCellDUList = List.of(
                buildFtRopNRCellDUInput("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00001", 1.1, 0),
                buildFtRopNRCellDUInput("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002", 1.1, 0),
                buildFtRopNRCellDUInput("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00003", 0.9, 0),
                buildFtRopNRCellDUInput("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00004", 1.1, 0)
        );
        ftRopNRCellDUList.get(0).setAvgSw2UlUeThroughput(20);
        ftRopNRCellDUList.get(1).setAvgSw2UlUeThroughput(21);
        ftRopNRCellDUList.get(2).setAvgSw2UlUeThroughput(20);
        ftRopNRCellDUList.get(0).setUeTpBaseline(21);
        ftRopNRCellDUList.get(1).setUeTpBaseline(21);
        ftRopNRCellDUList.get(2).setUeTpBaseline(21);

        Set<String> cellListFdns = cellMitigationService.getCellsAboveDeltaIpnThresholdAndBelowUETPBaselineFdns(1, ftRopNRCellDUList);
        assertEquals(cellListFdns, Set.of("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00001"));
    }

}
