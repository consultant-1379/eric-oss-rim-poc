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
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineHoCoefficient;
import com.ericsson.oss.apps.model.mom.*;
import com.ericsson.oss.apps.repositories.PmBaselineHoCoefficientRepo;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.NRCELL_CU_FDN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankAndLimitOverlappingNeighboursTest {

    @InjectMocks
    RankAndLimitOverlappingNeighbours rankAndLimitOverlappingNeighbours;
    @Mock
    CellSelectionConfig cellSelectionConfig;
    @Mock
    PmBaselineHoCoefficientRepo pmBaselineHoCoefficientRepo;
    @Mock
    Counter counter;

    FeatureContext context;
    FtRopNRCellDU ftRopNRCellDU;

    @BeforeEach
    void setUp() {
        context = new FeatureContext(0);
        ftRopNRCellDU = new FtRopNRCellDU(new MoRopId(FDN1, 0));
        context.setFtRopNRCellDUCellsForMitigation(List.of(ftRopNRCellDU));
        context.getFdnToFtRopNRCellDU().put(FDN1, ftRopNRCellDU);
    }

    /**
     * given
     * a map of 6 neighbor to cu is available in context (indexed 1,2,3,4,5,6)
     * 5 of the neighbors (indexed 1,2,3,4,5) have handovers in baseline (as in 1,2,3,4,5 handovers each)
     * the 6th neighbor does not have handovers in baseline
     * the threshold for handover coverage is set to 90%
     * when handle is invoked
     * then
     * 4 neighbors are ranked in order (5,4,3,2)
     * the total of hos is 15 for all the ranked neighbors
     * the cumulative ho percent is respectively 33.33, 60, 80, 93.3
     */
    @Test
    void handle() {
        handle(this::getAutoNciFdn);
    }

    /**
     * if we use netsim we don't remap fdns for baseline lookup
     */
    @Test
    void handleNetsim() {
        ReflectionTestUtils.setField(rankAndLimitOverlappingNeighbours, "retainHoFdnAsIs", true);
        handle(ManagedObject::toFdn);
    }

    void handle(Function<NRCellRelation, String> nciMapper) {
        //6 relations and corresponding CUs, with index at the end of FDN
        List<NRCellRelation> relationList = IntStream.range(1, 7).mapToObj(index -> {
            NRCellRelation relation = new NRCellRelation(NRCELL_CU_FDN + ",NRCellRelation=auto" + index);
            NRCellCU nrCellCU = new NRCellCU(NRCELL_CU_FDN + index);
            relation.setTargetCell(nrCellCU);
            context.getFdnToFtRopNRCellDU().get(FDN1).getCellRelationMap().put(relation, nrCellCU);
            setupCgi(nrCellCU, index);
            return relation;
        }).collect(Collectors.toList());
        //mock baseline reply, only 5 have baseline - last one (index 6) missing
        List<String> relationFdns = relationList.stream().map(nciMapper).collect(Collectors.toList());

        List<PmBaselineHoCoefficient> pmBaselineHoCoefficients = IntStream.range(1, 6).mapToObj(index -> {
            PmBaselineHoCoefficient pmBaselineHoCoefficient = new PmBaselineHoCoefficient();
            // ho repo will send back an auto nci fdn
            String relationFdn = nciMapper.apply(relationList.get(index - 1));
            pmBaselineHoCoefficient.setFdnNrCellRelation(relationFdn);
            // number of handovers a 1,2,3,4,5, total 15
            pmBaselineHoCoefficient.setNumberHandovers(index);
            return pmBaselineHoCoefficient;
        }).collect(Collectors.toList());
        when(pmBaselineHoCoefficientRepo.findAllById(relationFdns)).thenReturn(pmBaselineHoCoefficients);
        //at 90% requested will cut off the last relation in order of HOs (relation 1)
        when(cellSelectionConfig.getAcceptHandoversAboveHoPercent()).thenReturn(90D);
        rankAndLimitOverlappingNeighbours.handle(context);
        Map<String, HandOvers> cuFdnHoMap = context.getFdnToFtRopNRCellDU().get(FDN1).getCuFdnToHandovers();
        List<String> cuFdns = new ArrayList<>(cuFdnHoMap.keySet());
        // check they come out in the right order (5,4,3,2)
        assertEquals(IntStream.range(0, 4).map(index -> 5 - index).mapToObj(index -> NRCELL_CU_FDN + index).collect(Collectors.toList()), cuFdns);
        // checking values are correct
        checkRankedCu(5, 33.33, cuFdnHoMap);
        checkRankedCu(4, 60, cuFdnHoMap);
        checkRankedCu(3, 80, cuFdnHoMap);
        checkRankedCu(2, 93.33, cuFdnHoMap);

        assertEquals(4, cuFdns.size());
    }

    private String getAutoNciFdn(NRCellRelation relation) {
        return NRCELL_CU_FDN + ",NRCellRelation=auto" + relation.getTargetCell().getCGI().getAutonNci();
    }

    private void setupCgi(NRCellCU nrCellCU, int index) {
        GNodeB node = new ExternalGNBCUCPFunction();
        node.setPLMNId(new PLMNId(10, 20));
        node.setGNBId(1L);
        node.setGNBIdLength(24);
        nrCellCU.setCellLocalId(index);
        nrCellCU.setNode(node);
    }

    private void checkRankedCu(int i, double cumulativeHoPerc, Map<String, HandOvers> cuFdnHoMap) {
        assertEquals(cumulativeHoPerc, cuFdnHoMap.get(NRCELL_CU_FDN + i).getCumulativeTotalHandoversPercent(), 0.01);
        assertEquals(15, cuFdnHoMap.get(NRCELL_CU_FDN + i).getNumberTotalHandovers());

    }
}