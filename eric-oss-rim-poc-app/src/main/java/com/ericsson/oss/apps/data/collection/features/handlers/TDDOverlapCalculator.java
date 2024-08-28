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

import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.model.Constants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TDDOverlapCalculator implements FeatureHandler<FeatureContext> {

    private final Counter numCellPairsTDDOverlap;

    @Override
    @Timed
    public void handle(FeatureContext featureContext) {
        // grab all the cell pairs after ducting calculation
        List<FtRopNRCellDUPair> cellPairsWithOverlap = featureContext.getFtRopNRCellDUPairs().parallelStream()
                .peek(ftRopNRCellDUPair -> {
                    CellTDDInfo sourceCell = extractTddInfo(featureContext.getNrCellDU(ftRopNRCellDUPair.getFdn1()));
                    CellTDDInfo targetCell = extractTddInfo(featureContext.getNrCellDU(ftRopNRCellDUPair.getFdn2()));
                    ftRopNRCellDUPair.setTddOverlap(calculateTDDOverlap(ftRopNRCellDUPair.getDistance(), sourceCell, targetCell));
                })
                .filter(ftRopNRCellDUPair -> ftRopNRCellDUPair.getTddOverlap() > 0)
                .collect(Collectors.toUnmodifiableList());
        log.info("Cells {} detected with TDD overlap", cellPairsWithOverlap.size());
        numCellPairsTDDOverlap.increment(cellPairsWithOverlap.size());
        featureContext.setFtRopNRCellDUPairs(cellPairsWithOverlap);
    }

    private static CellTDDInfo extractTddInfo(NRCellDU nrCellDU) {
        return CellTDDInfo.builder()
                .subCarrierSpacing(nrCellDU.getSubCarrierSpacing())
                .tddSpecialSlotPattern(nrCellDU.getTddSpecialSlotPattern())
                .guardSymbols(nrCellDU.getEffectiveGuardSymbols())
                .tddUlDlPattern(nrCellDU.getTddUlDlPattern())
                .build();
    }

    static Double calculateTDDOverlap(double distance, CellTDDInfo sourceCell, CellTDDInfo targetCell) {
        double symbolsOverGuard = getSymbolsOverGuard(distance, sourceCell);
        return calculateOverlap(symbolsOverGuard, targetCell);
    }

    private static double getSymbolsOverGuard(double distance, CellTDDInfo sourceCell) {
        // do null checks
        if (sourceCell.subCarrierSpacing == null || sourceCell.tddSpecialSlotPattern == null) {
            return 0D;
        }

        // calculate the delay (in symbols) between cell 1 and cell 2
        double symbolsDelay = (distance / LIGHT_SPEED) / (SYMBOL_DURATION_15_KHZ * DEFAULT_SCS / sourceCell.subCarrierSpacing);
        // calculate how many symbols spill over
        return symbolsDelay - sourceCell.guardSymbols;
    }

    private static double calculateOverlap(double symbolsOverGuard, CellTDDInfo targetCell) {
        if (symbolsOverGuard <= 0 || targetCell.tddUlDlPattern == null || targetCell.tddSpecialSlotPattern == null) {
            // within guard
            return 0D;
        }

        if (!NRCellDU.TddUlDlPattern.TDD_ULDL_PATTERN_03.equals(targetCell.tddUlDlPattern)) {
            //running under the assumption that we only hit the
            //next set of UL slots, hitting another set (after DL)
            //will require a silly long distance
            double percentageOverlap = symbolsOverGuard /
                    (14D * targetCell.tddUlDlPattern.getUlSlots());
            return Math.min(1, percentageOverlap);
        } else {
            return (Math.min(symbolsOverGuard, 14D) +
                            Math.min(symbolsOverGuard, 14D * 2)) / (42);
        }
    }

    @Override
    public int getPriority() { return 20; }

    @Builder
    static class CellTDDInfo {
        private NRCellDU.TddSpecialSlotPattern tddSpecialSlotPattern;
        private NRCellDU.TddUlDlPattern tddUlDlPattern;
        private Integer subCarrierSpacing;
        private Integer guardSymbols;
    }

}
