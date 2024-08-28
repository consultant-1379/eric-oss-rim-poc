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

import com.ericsson.oss.apps.config.AggressorScoreWeights;
import com.ericsson.oss.apps.config.ClusteringConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggressorScoreCalculator implements FeatureHandler<FeatureContext> {

    private final ClusteringConfig clusteringConfig;

    @Override
    @Timed
    public void handle(FeatureContext context) {
        List<FtRopNRCellDUPair> nrCEllDUPairs = context.getFtRopNRCellDUPairs();
        log.info("calculating aggressor score for {} cell pairs and rop {}",
                nrCEllDUPairs.size(),
                context.getRopTimeStamp());
        AggressorScoreWeights aggressorScoreWeights = clusteringConfig.getAggressorScoreWeights();
        nrCEllDUPairs.parallelStream()
                .filter(ftRopNRCellDUPair -> ftRopNRCellDUPair.getFrequencyOverlap() > 0)
                .filter(ftRopNRCellDUPair -> ftRopNRCellDUPair.getTddOverlap() > 0)
                .forEach(ftRopNRCellDUPair -> {
                    Pair<FtRopNRCellDU, FtRopNRCellDU> cells = context.getFtRopNRCellDUs(ftRopNRCellDUPair);
                    if (cells != null) {
                        double aggressorScore = aggressorScore(ftRopNRCellDUPair, cells, aggressorScoreWeights);
                        ftRopNRCellDUPair.setAggressorScore(aggressorScore);
                    } else {
                        log.warn("cannot find cell data for {} and {}, this should not happen at this point and it's most likely a bug.",
                                ftRopNRCellDUPair.getFdn1(),
                                ftRopNRCellDUPair.getFdn2());
                    }
                });
    }

    @Override
    public int getPriority() {
        return 40;
    }

    private double aggressorScore(FtRopNRCellDUPair ftRopNRCellDUPair,
                                  Pair<FtRopNRCellDU, FtRopNRCellDU> cells,
                                  AggressorScoreWeights aggressorScoreWeights) {
        return aggressorScoreWeights.getAzimuthAffinity() * ftRopNRCellDUPair.getAzimuthAffinity()
                + aggressorScoreWeights.getPmRadioMaxDeltaIpNAvgC2() * cells.getSecond().getAvgSw8AvgDeltaIpN()
                - aggressorScoreWeights.getDistance() * 10 * Math.log10(ftRopNRCellDUPair.getDistance())
                + aggressorScoreWeights.getDlRBSymUtilC1() * 10 * Math.log10(cells.getFirst().getDlRBSymUtil())
                + aggressorScoreWeights.getTddOverlap() * 10 * Math.log10(ftRopNRCellDUPair.getTddOverlap())
                + aggressorScoreWeights.getFrequencyOverlap() * 10 * Math.log10(ftRopNRCellDUPair.getFrequencyOverlap())
                + aggressorScoreWeights.getDuctStrength() * 10 * Math.log10(ftRopNRCellDUPair.getDuctStrength());
    }

}
