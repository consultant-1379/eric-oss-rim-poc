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

import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//@formatter:off
/**
 * The Class SelectionOfNeighborCellsForCIO.
 *
 * REF:
 * Mitigation Algorithm Solution Sketch - DG Automation - PDUOSS Confluence (ericsson.com)
 *    https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PA/Mitigation+Algorithm+Solution+Sketch
 * Cell Coupling Investigation - DG Automation - PDUOSS Confluence (ericsson.com)
 *    https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PA/Cell+Coupling+Investigation
 *
 *
 */
//@formatter:on

@Component
@RequiredArgsConstructor
@Slf4j
public class SelectionOfNeighborCellsForCIO implements FeatureHandler<FeatureContext> {

    private final CellSelectionConfig thresholdConfig;
    private final Counter numNeighborCellsSelectedForKcio;

    /**
     * Main entry point for Class..
     *
     * @param featureContext the feature context
     */
    @Override
    @Timed
    public void handle(FeatureContext featureContext) {
        List<FtRopNRCellDU> ftRopNRCellDUCellsForMitigationList = featureContext.getFtRopNRCellDUCellsForMitigation();
        log.info("KCIO: Processing {} Mitigation cells for Selection Of Neighbor Cells For CIO",
                ftRopNRCellDUCellsForMitigationList.size());
        AtomicInteger numberInterAndIntraNeighbors = new AtomicInteger(0);
        AtomicInteger numberNeighborsSelected = new AtomicInteger(0);
        AtomicInteger numberVictimSelected = new AtomicInteger(0);
        AtomicInteger numberVictimNotSelectedNoNeighbor = new AtomicInteger(0);

        ftRopNRCellDUCellsForMitigationList.forEach(mitigationCell -> {
            String fdn = mitigationCell.getMoRopId().getFdn();
            if (mitigationCell.getIntraFneighborNrCellCu().isEmpty() && mitigationCell.getInterFneighborNrCellCu().isEmpty()) {
                log.trace("KCIO: No Neighbor Cells available for Mitigation cell with fdn '{}'.", fdn);
                numberVictimNotSelectedNoNeighbor.getAndIncrement();
                return;
            }
            numberVictimSelected.getAndIncrement();
            log.trace("KCIO: Calculating 'Selection Of {} Inter and {} Intra Neighbor Cells For CIO' for Mitigation cell with fdn '{}'",
                    mitigationCell.getInterFneighborNrCellCu().size(), mitigationCell.getIntraFneighborNrCellCu().size(), fdn);

            Map<String, NRCellCU> fdnToNeighborCuMap = Stream.concat(
                            mitigationCell.getIntraFneighborNrCellCu().stream(),
                            mitigationCell.getInterFneighborNrCellCu().stream())
                    .collect(Collectors.toMap(ManagedObject::toFdn, Function.identity()));


            numberInterAndIntraNeighbors.getAndAdd(fdnToNeighborCuMap.size());

            // Get HandOvers for each neighbor.
            List<NRCellCU> kcioInterAndIntraNeighborsNrCellCu = mitigationCell.getCuFdnToHandovers().entrySet().stream()
                    // Keep cell if its #HO is over cut off value
                    .filter(entry -> entry.getValue().getNumberHandovers() >= thresholdConfig.getCioRejectNumberHandoversBelowValue())
                    .map(Map.Entry::getKey)
                    .flatMap(neighborFdn -> Optional.ofNullable(fdnToNeighborCuMap.get(neighborFdn)).stream())
                    .limit(thresholdConfig.getCioAcceptTopRankedValue())
                    .collect(Collectors.toList());

            numberNeighborsSelected.getAndAdd(kcioInterAndIntraNeighborsNrCellCu.size());
            log.trace("KCIO: Selected {} Inter & Intra Neighbors for CIO Mitigation cell with fdn '{}'", kcioInterAndIntraNeighborsNrCellCu.size(),
                    fdn);
            mitigationCell.setKcioNeighborNrCellCu(kcioInterAndIntraNeighborsNrCellCu);
        });
        log.info("KCIO: Processed '{}' Mitigation cells. # Mitigation cells Selected = {}, # Mitigation cells NOT Selected = {} (no neighbors). "
            + "Filtered {} -> {} Intra & Inter Neighbors.", ftRopNRCellDUCellsForMitigationList.size(), numberVictimSelected
                .get(), numberVictimNotSelectedNoNeighbor.get(), numberInterAndIntraNeighbors.get(), numberNeighborsSelected.get());
        numNeighborCellsSelectedForKcio.increment(numberInterAndIntraNeighbors.get());
    }


    @Override
    public int getPriority() {
        return 166;
    }
}
