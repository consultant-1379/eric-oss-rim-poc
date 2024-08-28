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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class VictimScoreCalculatorTest {

    VictimScoreCalculator victimScoreCalculator = new VictimScoreCalculator();

    @ParameterizedTest
    @CsvSource(value = {
            "5, 10, 11.1933",
            "5, NaN, 5",
            "NaN, 5, 5",
            "Infinity, 5, 5",
            "-Infinity, 5, 5",
            "NaN, NaN, null",
    }, nullValues = {"null"})
    void handle(double aggScore1, double aggScore2, Double result) {
        FeatureContext featureContext = new FeatureContext(0L);
        featureContext.getFdnToFtRopNRCellDU().put(FDN2, new FtRopNRCellDU());
        FtRopNRCellDUPair ftRopNRCellDUPair1 = buildFtRopNRCellDUPairAggressorScore(FDN1, FDN2, aggScore1);
        FtRopNRCellDUPair ftRopNRCellDUPair2 = buildFtRopNRCellDUPairAggressorScore(FDN1, FDN2, aggScore2);
        featureContext.setFtRopNRCellDUPairs(Arrays.asList(ftRopNRCellDUPair1, ftRopNRCellDUPair2));
        victimScoreCalculator.handle(featureContext);
        if (result != null) {
            assertEquals(result, featureContext.getFdnToFtRopNRCellDU().get(FDN2).getVictimScore(), 0.0001);
        } else {
            assertNull(featureContext.getFdnToFtRopNRCellDU().get(FDN2).getVictimScore());
        }
    }
    
}