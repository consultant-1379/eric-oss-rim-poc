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
import com.ericsson.oss.apps.data.collection.HandOvers;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//@formatter:off
/**
 * The Class SelectionOfNeighborCellsForP0.
 *
 * REF:
 * Mitigation Algorithm Solution Sketch - DG Automation - PDUOSS Confluence (ericsson.com)
 *    https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PA/Mitigation+Algorithm+Solution+Sketch
 * Cell Coupling Investigation - DG Automation - PDUOSS Confluence (ericsson.com)
 *    https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PA/Cell+Coupling+Investigation
 */
//@formatter:on

@Component
@RequiredArgsConstructor
@Slf4j
public class SelectionOfNeighborCellsForP0 implements FeatureHandler<FeatureContext> {

    private final CellSelectionConfig thresholdConfig;
    private final Counter numNeighborCellsSelectedForKp0;

    /**
     * Main entry point for Class..
     *
     * @param featureContext the feature context
     */
    @Override
    @Timed
    public void handle(FeatureContext featureContext) {
        List<FtRopNRCellDU> ftRopNRCellDUCellsForMitigationList = featureContext.getFtRopNRCellDUCellsForMitigation();
        log.info("KP0: Processing {} Mitigation cells for Selection Of Neighbor Cells For P0", ftRopNRCellDUCellsForMitigationList.size());
        AtomicInteger numberIntra = new AtomicInteger(0);
        AtomicInteger numberIntraSelected = new AtomicInteger(0);
        AtomicInteger numberVictimSelected = new AtomicInteger(0);
        AtomicInteger numberVictimNotSelectedNoNeighbor = new AtomicInteger(0);

        ftRopNRCellDUCellsForMitigationList.forEach(mitigationCell -> {
            String fdn = mitigationCell.getMoRopId().getFdn();
            if (mitigationCell.getIntraFneighborNrCellCu().isEmpty()) {
                log.trace("KP0: No Intra Cells available for Mitigation cell with fdn '{}'.", fdn);
                numberVictimNotSelectedNoNeighbor.getAndIncrement();
                return;
            }
            numberVictimSelected.getAndIncrement();
            log.trace("KP0: Calculating 'Selection Of Neighbor Cells For P0' for Mitigation cell with fdn '{}', which has {} Intra Neighbors",
                    fdn, mitigationCell.getIntraFneighborNrCellCu().size());

            numberIntra.getAndAdd(mitigationCell.getIntraFneighborNrCellCu().size());

            // Get HandOvers for each neighbor.
            Map<String, HandOvers> cuFdnToHandovers = mitigationCell.getCuFdnToHandovers().entrySet().stream()
                    // Keep Intra cell if its #HO is over cut off value
                    .filter(entry -> entry.getValue().getNumberHandovers() >= thresholdConfig.getP0RejectNumberHandoversBelowValue())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            List<NRCellCU> kP0IntraFneighborNrCellCu = mitigationCell.getIntraFneighborNrCellCu()
                    .stream()
                    .filter(neighborNrCellCuFdn -> cuFdnToHandovers.containsKey(neighborNrCellCuFdn.toFdn()))
                    .collect(Collectors.toList());

            numberIntraSelected.getAndAdd(kP0IntraFneighborNrCellCu.size());
            log.trace("KP0: Selected {} IntraNeighbors for P0 Mitigation cell with fdn '{}'", kP0IntraFneighborNrCellCu.size(), fdn);
            mitigationCell.setKp0IntraFneighborNrCellCu(kP0IntraFneighborNrCellCu);
        });
        log.info("KP0: Processed '{}' Mitigation cells. # Mitigation cells Selected = {}, # Mitigation cells NOT Selected = {} (no neighbors). "
            + "Filtered {} -> {} Intra Neighbors.", ftRopNRCellDUCellsForMitigationList
                .size(), numberVictimSelected.get(), numberVictimNotSelectedNoNeighbor.get(), numberIntra.get(), numberIntraSelected.get());
        numNeighborCellsSelectedForKp0.increment(numberIntraSelected.get());
    }

    @Override
    public int getPriority() {
        return 165;
    }
}
