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
import com.ericsson.oss.apps.data.collection.HandOverUtils;
import com.ericsson.oss.apps.data.collection.HandOvers;
import com.ericsson.oss.apps.repositories.PmBaselineHoCoefficientRepo;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
//@formatter:off

/**
 Ranks overlapping neighbors and cuts them off to the required percentage of overlap.
 Sets the result in descending order of HOs in FtRopNrCellDU
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RankAndLimitOverlappingNeighbours implements FeatureHandler<FeatureContext> {

    private final PmBaselineHoCoefficientRepo pmBaselineHoCoefficientRepo;
    private final CellSelectionConfig cellSelectionConfig;
    private final Counter numNeighborCellsDroppedNoHandovers;

    @Value("${app.data.netsim}")
    @Setter
    private boolean retainHoFdnAsIs;

    @Override
    @Timed
    public void handle(FeatureContext featureContext) {
        AtomicInteger totalDroppedRelationsNoHo = new AtomicInteger(0);
        // get the list of cells for mitigation
        featureContext.getFtRopNRCellDUCellsForMitigation()
                .forEach(mitigationCell ->  {
                    // Get HandOvers for each neighbor (CU fdn).
                    Map<String, HandOvers> couplingCoefficientMap = HandOverUtils.filterHoCoefficeintFromBaseLine(mitigationCell.getCellRelationMap(), pmBaselineHoCoefficientRepo, retainHoFdnAsIs);
                    // Rank by HO Coefficient ; High to low
                    Map<String, HandOvers> couplingCoefficientRankedMap = HandOverUtils.sortMapByValue(couplingCoefficientMap);
                    // get the neighbors covering top X handovers%
                    Map<String, HandOvers> cuFdnToHandovers = HandOverUtils.filterTopPercentByCdf(couplingCoefficientRankedMap,cellSelectionConfig.getAcceptHandoversAboveHoPercent());
                    mitigationCell.setCuFdnToHandovers(cuFdnToHandovers);

                    int droppedRelations = mitigationCell.getCellRelationMap().size() - couplingCoefficientMap.size();
                    totalDroppedRelationsNoHo.getAndAdd(droppedRelations);
                    numNeighborCellsDroppedNoHandovers.increment(droppedRelations);
                });
        log.info("Ranked neighbors for {} mitigation cells, dropped {} relations", featureContext.getFtRopNRCellDUCellsForMitigation().size(), totalDroppedRelationsNoHo);
    }

    @Override
    public int getPriority() {
        return 163;
    }


}
