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

import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class VictimScoreCalculator implements FeatureHandler<FeatureContext> {

    @Override
    @Timed
    public void handle(FeatureContext context) {

        List<FtRopNRCellDUPair> nrCEllDUPairsFilterValid = context.getFtRopNRCellDUPairs().parallelStream()
                .filter(ftRopNRCellDUPair -> !Double.isNaN(ftRopNRCellDUPair.getAggressorScore()))
                .filter(ftRopNRCellDUPair -> ftRopNRCellDUPair.getAggressorScore() > Double.NEGATIVE_INFINITY)
                .filter(ftRopNRCellDUPair -> ftRopNRCellDUPair.getAggressorScore() < Double.POSITIVE_INFINITY)
                .collect(Collectors.toUnmodifiableList());

        log.info("calculating victim score based on  {} cell pairs and rop {}",
                nrCEllDUPairsFilterValid.size(),
                context.getRopTimeStamp());

        Map<String, FtRopNRCellDU> fdnToFtRopNRCellDU = context.getFdnToFtRopNRCellDU();

        nrCEllDUPairsFilterValid.stream()
                .map(ftRopNRCellDUPair -> Pair.of(ftRopNRCellDUPair.getFdn2(), Math.pow(10, ftRopNRCellDUPair.getAggressorScore() / 10)))
                .collect(Collectors.groupingBy(Pair::getFirst, Collectors.summingDouble(Pair::getSecond)))
                .forEach((fdn, score) -> fdnToFtRopNRCellDU.get(fdn).setVictimScore(10 * Math.log10(score)));
    }

    @Override
    public int getPriority() {
        return 50;
    }

}
