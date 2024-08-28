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

import com.ericsson.oss.apps.config.ClusteringConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectedComponentsCalculator implements FeatureHandler<FeatureContext> {

    private final ClusteringConfig clusteringConfig;

    private final Counter numConnectedComponents;

    private final Counter numConnectedComponentsAboveMinSize;

    @Override
    @Timed
    public void handle(FeatureContext context) {
        List<FtRopNRCellDUPair> nrCEllDUPairs = context.getFtRopNRCellDUPairs();
        log.info("calculating connected components for {} cell pairs and rop {}", nrCEllDUPairs.size(), context.getRopTimeStamp());
        double minimumConnectedEdgeWeight = clusteringConfig.getMinimumConnectedEdgeWeight();

        List<FtRopNRCellDUPair> nrCEllDUPairsFilterValid = nrCEllDUPairs.parallelStream()
                .filter(ftRopNRCellDUPair -> !Double.isNaN(ftRopNRCellDUPair.getAggressorScore()))
                .filter(ftRopNRCellDUPair -> ftRopNRCellDUPair.getAggressorScore() > Double.NEGATIVE_INFINITY)
                .filter(ftRopNRCellDUPair -> ftRopNRCellDUPair.getAggressorScore() < Double.POSITIVE_INFINITY)
                .collect(Collectors.toUnmodifiableList());

        log.info("selected {} cell pairs valid for rop {}", nrCEllDUPairsFilterValid.size(), context.getRopTimeStamp());

        SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> cellGraph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Map<String, FtRopNRCellDU> fdnToFtRopNRCellDU = context.getFdnToFtRopNRCellDU();

        List<FtRopNRCellDUPair> filteredNRCellDUPairs = nrCEllDUPairsFilterValid.stream()
                .filter(ftRopNRCellDUPair -> (ftRopNRCellDUPair.getAggressorScore() > minimumConnectedEdgeWeight))
                .filter(ftRopNRCellDUPair -> filterFtRopNRCellDUPairsForClustering(fdnToFtRopNRCellDU.get(ftRopNRCellDUPair.getFdn1()),
                        fdnToFtRopNRCellDU.get(ftRopNRCellDUPair.getFdn2()))).collect(Collectors.toUnmodifiableList());

        //add vertices
        filteredNRCellDUPairs.stream().flatMap(ftRopNRCellDUPair -> Stream.of(ftRopNRCellDUPair.getFdn1(), ftRopNRCellDUPair.getFdn2()))
                .distinct().forEach(cellGraph::addVertex);

        log.info("added {} vertices to graph", cellGraph.vertexSet().size());

        //filter edges and add to the graph
        filteredNRCellDUPairs.forEach(ftRopNRCellDUPair -> {
                    DefaultWeightedEdge edge = cellGraph.addEdge(ftRopNRCellDUPair.getFdn1(), ftRopNRCellDUPair.getFdn2());
                    cellGraph.setEdgeWeight(edge, ftRopNRCellDUPair.getAggressorScore());
                });

        log.info("added {} edges to graph", cellGraph.edgeSet().size());

        //jgrapht magic
        StrongConnectivityAlgorithm<String, DefaultWeightedEdge> scAlg = new GabowStrongConnectivityInspector<>(cellGraph);
        List<Graph<String, DefaultWeightedEdge>> connectedComponents = scAlg.getStronglyConnectedComponents();

        log.info("found {} connected components", connectedComponents.size());
        numConnectedComponents.increment(connectedComponents.size());

        //assign a component id as a feature to the cells
        connectedComponents.stream()
                .filter(connectedComponent -> connectedComponent.vertexSet().size() > clusteringConfig.getMinimumConnectedComponentSize())
                .forEach(withCounter((componentId, connectedComponent) ->
                        {
                            connectedComponent.vertexSet().forEach(fdn -> fdnToFtRopNRCellDU.get(fdn).setConnectedComponentId(componentId));
                            numConnectedComponentsAboveMinSize.increment();
                        }
                ));
    }

    private boolean filterFtRopNRCellDUPairsForClustering(FtRopNRCellDU sourceFtRopNRCellDU, FtRopNRCellDU targetFtRopNRCellDU) {
        return (sourceFtRopNRCellDU.getVictimScore() != null && sourceFtRopNRCellDU.getVictimScore() > clusteringConfig.getMinimumVictimScore() &&
                targetFtRopNRCellDU.getVictimScore() != null && targetFtRopNRCellDU.getVictimScore() > clusteringConfig.getMinimumVictimScore());
    }

    private static <T> Consumer<T> withCounter(BiConsumer<Long, T> consumer) {
        AtomicLong componentId = new AtomicLong(0);
        return item -> consumer.accept(componentId.incrementAndGet(), item);
    }

    @Override
    public int getPriority() {
        return 53;
    }

}
