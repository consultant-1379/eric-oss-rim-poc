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
class SelectionOfNeighborCellsForCIOTest {

    public static final int OK_NUMBER_HO = 10;
    FeatureContext context;
    FtRopNRCellDU ftRopNRCellDU;

    @InjectMocks
    SelectionOfNeighborCellsForCIO selectionOfNeighborCellsForCIO;

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
     * there are ten intra neighbors (0,1,2,3,4) for the cell
     * there are four inter neighbors (5,6,7,8,9,10) for the cell
     * neighbor 0 is not in ranked cells map
     * neighbor 5 is not in ranked cells map
     * neighbor 2 has insufficient handovers
     * neighbor 7 has insufficient handovers
     * the threshold for ranking is set to top 6
     * the ranked cell map is ordered as (4,3,2,1,10,9,8,7,6)
     * when
     * handle is invoked
     * then
     * the kcioInterAndIntraNeighborsNrCellCu list for the mitigation cells contains neighbors 4,3,1,10,9,8
     */
    @Test
    void testMaxNumberOfRankedHos() {
        List<NRCellCU> neighborList = IntStream.range(0, 11).mapToObj(index -> new NRCellCU(NRCELL_CU_FDN + index)).collect(Collectors.toList());
        int[] rankOrder = new int[]{4, 3, 2, 1, 10, 9, 8, 7, 6};

        ftRopNRCellDU.getIntraFneighborNrCellCu().addAll(neighborList.subList(0, 5));
        ftRopNRCellDU.getInterFneighborNrCellCu().addAll(neighborList.subList(5, 11));

        Set<Integer> insufficientHoIndex = Set.of(2, 7);

        // top 6, minus the ones excluded because of ho number
        int[] expectedNeighborIndex = new int[]{4, 3, 1, 10, 9, 8};

        Set<NRCellCU> cioNeighbors  = runAndGetFtRopNrCellDUResult(neighborList, rankOrder, insufficientHoIndex);
        Set<NRCellCU> expectedNeighbors = IntStream.of(expectedNeighborIndex).mapToObj(neighborList::get).collect(Collectors.toSet());

        assertEquals(expectedNeighbors, cioNeighbors);
    }

    /**
     * given
     * there is one cell for mitigation in context
     * there are three intra neighbors (0,1,2) for the cell
     * there are two inter neighbors (6,7) for the cell
     * neighbor 0 is not in ranked cells map
     * neighbor 2 has insufficient handovers
     * neighbor 7 has insufficient handovers
     * the threshold for ranking is set to top 6
     * the ranked cell map is ordered as (4,3,2,1,10,9,8,7,6)
     * when
     * handle is invoked
     * then
     * the kcioInterAndIntraNeighborsNrCellCu list for the mitigation cells contains neighbors 1,6
     */
    @Test
    void testLEssThanMaxNumberOfRankedHos() {
        List<NRCellCU> neighborList = IntStream.range(0, 11).mapToObj(index -> new NRCellCU(NRCELL_CU_FDN + index)).collect(Collectors.toList());
        int[] rankOrder = new int[]{4, 3, 2, 1, 10, 9, 8, 7, 6};

        int[] expectedNeighborIndex = new int[]{1, 6};

        ftRopNRCellDU.getIntraFneighborNrCellCu().addAll(neighborList.subList(0, 3));
        ftRopNRCellDU.getInterFneighborNrCellCu().addAll(neighborList.subList(6, 8));

        Set<Integer> insufficientHoIndex = Set.of(2, 7);

        Set<NRCellCU> cioNeighbors  = runAndGetFtRopNrCellDUResult(neighborList, rankOrder, insufficientHoIndex);
        Set<NRCellCU> expectedNeighbors = IntStream.of(expectedNeighborIndex).mapToObj(neighborList::get).collect(Collectors.toSet());

        assertEquals(expectedNeighbors, cioNeighbors);
    }

    private Set<NRCellCU> runAndGetFtRopNrCellDUResult(List<NRCellCU> neighborList, int[] rankOrder, Set<Integer> insufficientHoIndex) {
        // add all to ranked map in order
        IntStream.of(rankOrder).forEach(index -> {
            String neighborFdn = neighborList.get(index).toFdn();
            // set a coup[le of them with a lower value
            int nHandovers = insufficientHoIndex.contains(index) ? OK_NUMBER_HO - 1 : OK_NUMBER_HO;
            ftRopNRCellDU.getCuFdnToHandovers().put(neighborFdn, new HandOvers(null, nHandovers, 0));
        });

        when(thresholdConfig.getCioRejectNumberHandoversBelowValue()).thenReturn(OK_NUMBER_HO);
        when(thresholdConfig.getCioAcceptTopRankedValue()).thenReturn(6);

        selectionOfNeighborCellsForCIO.handle(context);

        FtRopNRCellDU cellForMitigation = context.getFtRopNRCellDUCellsForMitigation().get(0);
        return new HashSet<>(cellForMitigation.getKcioNeighborNrCellCu());
    }
}