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

import com.ericsson.oss.apps.config.ArfcnRange;
import com.ericsson.oss.apps.config.ClusteringConfig;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRSectorCarrier;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Cells in the same band may be set up on completely different frequencies (in such a case they should not interfere),
 * or partially overlapping frequencies (in such a case interference should be reduced).
 * This class calculates an overlap coefficient to be used as part of the aggressor score.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FrequencyOverlapCalculator implements FeatureHandler<FeatureContext> {

    private final ClusteringConfig clusteringConfig;
    private final Counter numCellPairsFrequencyOverlap;
    private final HashMap<Integer, Double> centerFrequencyMap = new HashMap<>();

    @Override
    @Timed
    public void handle(FeatureContext featureContext) {
        log.info("Calculating frequency overlap for {} cell pairs ", featureContext.getFtRopNRCellDUPairs().size());
        featureContext.getFtRopNRCellDUPairs().forEach(ftRopNRCellDUPair -> {
            Optional<CellArfcnInfo> optSourceCellArfcnInfo = extractArfcnInfo(featureContext.getNrCellDU(ftRopNRCellDUPair.getFdn1()));
            Optional<CellArfcnInfo> optTargetCellArfcnInfo = extractArfcnInfo(featureContext.getNrCellDU(ftRopNRCellDUPair.getFdn2()));
            optSourceCellArfcnInfo.ifPresent(sourceCellArfcnInfo -> optTargetCellArfcnInfo.ifPresent(
                    targetCellArfcnInfo -> {
                        double frequencyOverlap = calculateFrequencyOverlapScore(clusteringConfig.getArfcnRanges(), sourceCellArfcnInfo, targetCellArfcnInfo);
                        ftRopNRCellDUPair.setFrequencyOverlap(frequencyOverlap);
                        if (frequencyOverlap > 0) {
                            numCellPairsFrequencyOverlap.increment();
                        }
                    }
            ));
        });
    }

    private double calculateFrequencyOverlapScore(List<ArfcnRange> arfcnRanges, CellArfcnInfo sourceCellArfcnInfo, CellArfcnInfo targetCellArfcnInfo) {
        if (sourceCellArfcnInfo.equals(targetCellArfcnInfo)) return 1;
        CellFreqRange freqRangeSourceCell = getFrequencyRange(arfcnRanges, sourceCellArfcnInfo);
        CellFreqRange freqRangeTargetCell = getFrequencyRange(arfcnRanges, targetCellArfcnInfo);
        double overlapMhz = Math.min(freqRangeTargetCell.max, freqRangeSourceCell.max) - Math.max(freqRangeTargetCell.min, freqRangeSourceCell.min);
        return (overlapMhz > 0) ? (overlapMhz / targetCellArfcnInfo.bsChannelBw) : 0;
    }

    private CellFreqRange getFrequencyRange(List<ArfcnRange> arfcnRanges, CellArfcnInfo cellArfcnInfo) {
        double centerFrequency = getCenterFrequencyMhz(arfcnRanges, cellArfcnInfo.arfcn);
        return new CellFreqRange(centerFrequency - cellArfcnInfo.bsChannelBw / 2D, centerFrequency + cellArfcnInfo.bsChannelBw / 2D);
    }

    /**
     * The formula for 5G NR ARFCN FREF = FREF-Offs + ΔFGlobal (NREF – NREF-Offs) is described in
     * <a href="https://portal.3gpp.org/desktopmodules/Specifications/SpecificationDetails.aspx?specificationId=3202">3GPP TS 38.104 chapter 5.4.2.1.</a>
     *
     * @param arfcnranges arfcn ranges and offsets according to standards
     * @param arfcn       given arfcn
     * @return the center frequency for the given arfcn
     */
    private double getCenterFrequencyMhz(List<ArfcnRange> arfcnranges, int arfcn) {
        return centerFrequencyMap.computeIfAbsent(arfcn, newArfcn -> arfcnranges.stream()
                .filter(arfcnRange -> newArfcn <= arfcnRange.getMaxNRef() && newArfcn >= arfcnRange.getMinNRef())
                .findFirst()
                .map(arfcnRange -> (double) arfcnRange.getFREFOffsMHz() + arfcnRange.getDeltaFGlobalKhz() * (newArfcn - arfcnRange.getMinNRef()))
                .map(centerFrequencyKhz -> centerFrequencyKhz / 1000)
                .orElse(Double.NaN));
    }

    private Optional<CellArfcnInfo> extractArfcnInfo(NRCellDU nrCellDU) {
        Integer bsChannelBwDL = nrCellDU.getNRSectorCarriers().stream().findFirst().map(NRSectorCarrier::getBSChannelBwDL).orElse(null);
        Integer arfcnDL = nrCellDU.getNRSectorCarriers().stream().findFirst().map(NRSectorCarrier::getArfcnDL).orElse(null);
        if (bsChannelBwDL != null && arfcnDL != null) {
            return Optional.of(CellArfcnInfo.builder()
                    .bsChannelBw(bsChannelBwDL)
                    .arfcn(arfcnDL)
                    .build());
        }
        return Optional.empty();
    }


    @Override
    public int getPriority() {
        return 21;
    }

    @Builder
    @EqualsAndHashCode
    static class CellArfcnInfo {
        private int bsChannelBw;
        private int arfcn;
    }

    @AllArgsConstructor
    private static class CellFreqRange {
        private double min;
        private double max;
    }

}
