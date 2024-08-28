/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.data.collection.HandOvers;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.NRCELL_CU_FDN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectionOfNeighborCellsForP0Test {


    public static final int OK_NUMBER_HO = 10;
    FeatureContext context;
    FtRopNRCellDU ftRopNRCellDU;

    @InjectMocks
    SelectionOfNeighborCellsForP0 selectionOfNeighborCellsForP0;

    @Mock
    CellSelectionConfig thresholdConfig;
    @Mock
    Counter counter;

    @BeforeEach
    void setUp() {
        context = new FeatureContext(0);
        ftRopNRCellDU = new FtRopNRCellDU(new MoRopId(FDN1, 0));
        context.setFtRopNRCellDUCellsForMitigation(List.of(ftRopNRCellDU));
    }

    /**
     * given
     * there is one cell for mitigation in context
     * there are 5 intra neighbors (0,1,2,3,4) for the cell
     * neighbor 0 is not in ranked cells map
     * neighbor 2 has insufficient handovers
     * the threshold for hos is set to 10
     * the ranked cell map is ordered as (4,3,2,1)
     * when
     * handle is invoked
     * then
     * the kP0InterAndIntraNeighborsNrCellCu list for the mitigation cells contains neighbors 4,3,1
     */
    @Test
    void handle() {
        List<NRCellCU> neighborList = IntStream.range(0, 5).mapToObj(index -> new NRCellCU(NRCELL_CU_FDN + index)).collect(Collectors.toList());
        ftRopNRCellDU.getIntraFneighborNrCellCu().addAll(neighborList);

        int[] rankOrder = new int[]{4, 3, 2, 1};
        Set<Integer> insufficientHoIndex = Set.of(2);
        // add all to ranked map in order
        IntStream.of(rankOrder).forEach(index -> {
            String neighborFdn = neighborList.get(index).toFdn();
            // set a coup[le of them with a lower value
            int nHandovers = insufficientHoIndex.contains(index) ? OK_NUMBER_HO - 1 : OK_NUMBER_HO;
            ftRopNRCellDU.getCuFdnToHandovers().put(neighborFdn, new HandOvers(null, nHandovers, 0));
        });

        when(thresholdConfig.getP0RejectNumberHandoversBelowValue()).thenReturn(OK_NUMBER_HO);

        selectionOfNeighborCellsForP0.handle(context);

        FtRopNRCellDU cellForMitigation = context.getFtRopNRCellDUCellsForMitigation().get(0);

        // neighbors, minus the ones excluded because of ho number or no ranking
        int[] expectedNeighborIndex = new int[]{4, 3, 1};

        Set<NRCellCU> expectedNeighbors = IntStream.of(expectedNeighborIndex).mapToObj(neighborList::get).collect(Collectors.toSet());
        Set<NRCellCU> p0Neighbors = new HashSet<>(cellForMitigation.getKp0IntraFneighborNrCellCu());

        assertEquals(expectedNeighbors, p0Neighbors);
    }
}