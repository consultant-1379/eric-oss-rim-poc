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
package com.ericsson.oss.apps.data.collection.features;

import com.ericsson.oss.apps.data.collection.allowlist.AllowedMoLoaderHandler;
import com.ericsson.oss.apps.data.collection.deletion.ExpiredRopDataRemoverHandler;
import com.ericsson.oss.apps.data.collection.features.handlers.*;
import com.ericsson.oss.apps.data.collection.features.handlers.geospatial.DuctStrengthSelector;
import com.ericsson.oss.apps.data.collection.features.handlers.geospatial.GeoJsonPolygonReporterHandler;
import com.ericsson.oss.apps.data.collection.features.handlers.geospatial.GeoTiffSelector;
import com.ericsson.oss.apps.data.collection.features.report.ReportCreator;
import com.ericsson.oss.apps.data.collection.pmbaseline.PmBaselineLoaderHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"mitigation.emergencyMode=true"})
class FeatureCalculatorItEmergencyTest extends FeatureCalculatorItTest {

    @Autowired
    ApplicationContext applicationContext;

    List<Class<? extends FeatureHandler<FeatureContext>>> getExpectedHandlerList() {
        return List.of(
                GeoTiffSelector.class,
                AllowedMoLoaderHandler.class,
                PmBaselineLoaderHandler.class,
                ExpiredRopDataRemoverHandler.class,
                KpiCalculator.class,
                GeoJsonPolygonReporterHandler.class,
                DuctStrengthSelector.class,
                TDDOverlapCalculator.class,
                FrequencyOverlapCalculator.class,
                AzimuthAffinityCalculator.class,
                AggressorScoreCalculator.class,
                VictimScoreCalculator.class,
                ConnectedComponentsCalculator.class,
                FilterVictimCells.class,
                RankAndLimitOverlappingNeighbours.class,
                SeparateInterAndIntraFreqNeighbourCells.class,
                SelectionOfNeighborCellsForP0.class,
                SelectionOfNeighborCellsForCIO.class,
                ReportCreator.class
        );
    }

    @Autowired
    private FeatureCalculator featureCalculator;

    @Test
    void testHandlersEmergency() {
        testHandlers();
        assertEquals(0, applicationContext.getBeanNamesForType(PendingParameterChangesApplicator.class).length);
        assertEquals(0, applicationContext.getBeanNamesForType(PendingRelationChangesApplicator.class).length);
    }

}