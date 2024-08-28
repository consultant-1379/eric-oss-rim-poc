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

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.buildFtRopNRCellDUPairAggressorScore;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.getFtRopNRCellDUPairs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import com.ericsson.oss.apps.config.ClusteringConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ConnectedComponentsCalculatorTest {

    private static final double VICTIM_SCORE = 1D;

    private static final double NEGATIVE_VICTIM_SCORE = -1D;

    @Mock
    private ClusteringConfig clusteringConfig;

    @Mock
    private Counter counter;

    @InjectMocks
    private ConnectedComponentsCalculator connectedComponentsCalculator;


    FtRopNRCellDU buildFtRopNRCellDUInput(String fdn, double victimScore) {
        var ftRopNRCellDU = new FtRopNRCellDU(new MoRopId(fdn, 0L));
        ftRopNRCellDU.setVictimScore(victimScore);
        return ftRopNRCellDU;
    }

    @Test
    void testHandleEmptyList() {
        var featureContext = new FeatureContext(0L);
        featureContext.setFtRopNRCellDUPairs(Collections.emptyList());
        connectedComponentsCalculator.handle(featureContext);
    }

    @Test
    void testConnectedComponents() {
        when(clusteringConfig.getMinimumConnectedEdgeWeight()).thenReturn(-1D);
        when(clusteringConfig.getMinimumConnectedComponentSize()).thenReturn(1);
        var featureContext = new FeatureContext(0L);
        Map<Long, List<FtRopNRCellDU>> connectedComponents = getConnectedComponents(featureContext, VICTIM_SCORE);
        verifyComponents(connectedComponents);
    }

    private static void verifyComponents(Map<Long, List<FtRopNRCellDU>> connectedComponents) {
        assertEquals(2, connectedComponents.size());
        List<Integer> componentSizeList = connectedComponents.values().stream().map(List::size).sorted().collect(Collectors.toUnmodifiableList());
        assertEquals(2, componentSizeList.get(0));
        assertEquals(3, componentSizeList.get(1));
    }

    @Test
    void testConnectedComponentsWithNegativeVictimScore() {
        when(clusteringConfig.getMinimumConnectedEdgeWeight()).thenReturn(-1D);
        var featureContext = new FeatureContext(0L);
        Map<Long, List<FtRopNRCellDU>> connectedComponents = getConnectedComponents(featureContext, NEGATIVE_VICTIM_SCORE);
        assertEquals(0, connectedComponents.size());
    }

    @Test
    void testConnectedComponentsEdgeWeight() {
        when(clusteringConfig.getMinimumConnectedEdgeWeight()).thenReturn(0D);
        when(clusteringConfig.getMinimumConnectedComponentSize()).thenReturn(1);
        var featureContext = new FeatureContext(0L);
        Map<Long, List<FtRopNRCellDU>> connectedComponents = getConnectedComponents(featureContext, VICTIM_SCORE);
        verifyComponents(connectedComponents);
    }

    private Map<Long, List<FtRopNRCellDU>> getConnectedComponents(FeatureContext featureContext, double victimScore) {

        var fdnToFtRopNRCellDU = featureContext.getFdnToFtRopNRCellDU();
        IntStream.range(1, 7)
                .mapToObj(fdnId -> (FDN1 + fdnId))
                .forEach(fdn -> fdnToFtRopNRCellDU.put(fdn, buildFtRopNRCellDUInput(fdn, victimScore)));
        List<FtRopNRCellDUPair> ftRopNRCellDUPairs = getFtRopNRCellDUPairs();
        featureContext.setFtRopNRCellDUPairs(ftRopNRCellDUPairs);

        connectedComponentsCalculator.handle(featureContext);

        return featureContext
                .getFdnToFtRopNRCellDU()
                .values()
                .stream()
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getConnectedComponentId() != null)
                .collect(Collectors.groupingBy(FtRopNRCellDU::getConnectedComponentId));
    }

    @Test
    void testMissingAggressorScore() {
        var featureContext = new FeatureContext(0L);
        var cellPairInput1 = buildFtRopNRCellDUPairAggressorScore(FDN1 + 1, FDN1 + 2, Double.NaN);
        var cellPairInput2 = buildFtRopNRCellDUPairAggressorScore(FDN1 + 2, FDN1 + 3, Double.NEGATIVE_INFINITY);
        featureContext.setFtRopNRCellDUPairs(Arrays.asList(cellPairInput1, cellPairInput2));
        connectedComponentsCalculator.handle(featureContext);
    }
}