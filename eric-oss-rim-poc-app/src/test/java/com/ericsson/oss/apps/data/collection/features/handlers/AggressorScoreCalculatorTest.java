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
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AggressorScoreCalculatorTest {

    private static final AggressorScoreWeights WEIGHTS = buildAggressorScoreWeights();

    @Mock
    private ClusteringConfig clusteringConfig;

    @InjectMocks
    private AggressorScoreCalculator aggressorScoreCalculator;

    private static AggressorScoreWeights buildAggressorScoreWeights() {
        var aggressorScoreWeights = new AggressorScoreWeights();
        aggressorScoreWeights.setDistance(1);
        aggressorScoreWeights.setAzimuthAffinity(1);
        aggressorScoreWeights.setTddOverlap(1);
        aggressorScoreWeights.setDuctStrength(3);
        aggressorScoreWeights.setDlRBSymUtilC1(1);
        aggressorScoreWeights.setFrequencyOverlap(1);
        aggressorScoreWeights.setPmRadioMaxDeltaIpNAvgC2(1);
        return aggressorScoreWeights;
    }

    @ParameterizedTest
    @CsvSource(value = {
            "10, 5, 100, 0.1, 1, 100, 0.1, 35",
            "10, 5, 100, 0.1, 1, 10, 1, 15",
            "10, 5, 100, 0.1, 0.1, 10, 1, 5",
            "10, 5, 10, 0.1, 0.1, 10, 1, 15",
            "10, 0, 10, 0.1, 0.1, 10, 1, 10",
            "5, 0, 10, 0.1, 0.1, 10, 1, 5",
            "10, 5, 100, 0.1, 1, 100, NaN, NaN"
    })
    void testCalculateAggressorScore(double avgSw8AvgDeltaIpN,
                                     double azimuthAffinity,
                                     double distance,
                                     double dlRBSymUtil,
                                     double tddOverlap,
                                     double ductStrength,
                                     double frequencyOverlap,
                                     double result) {
        when(clusteringConfig.getAggressorScoreWeights()).thenReturn(WEIGHTS);
        var featureContext = new FeatureContext(0L);
        var cellInput1 = buildFtRopNRCellDUInput(FDN1, Double.NaN, dlRBSymUtil);
        var cellInput2 = buildFtRopNRCellDUInput(FDN2, avgSw8AvgDeltaIpN, Double.NaN);
        var cellPairInput = buildFtRopNRCellDUPair(FDN1, FDN2,
                azimuthAffinity,
                distance,
                tddOverlap,
                ductStrength,
                frequencyOverlap);
        featureContext.setFtRopNRCellDUPairs(List.of(cellPairInput));
        var fdnToFtRopNRCellDU = featureContext.getFdnToFtRopNRCellDU();
        fdnToFtRopNRCellDU.put(FDN1, cellInput1);
        fdnToFtRopNRCellDU.put(FDN2, cellInput2);
        aggressorScoreCalculator.handle(featureContext);
        List<FtRopNRCellDUPair> ftRopNRCellDUPairList = featureContext.getFtRopNRCellDUPairs();
        Optional<FtRopNRCellDUPair> cellPairResult = ftRopNRCellDUPairList.stream().findFirst();
        assertTrue(cellPairResult.isPresent());
        assertEquals(result, cellPairResult.get().getAggressorScore());
    }

}