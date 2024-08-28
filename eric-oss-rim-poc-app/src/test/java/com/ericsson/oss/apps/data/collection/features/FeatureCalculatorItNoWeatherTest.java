/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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
import com.ericsson.oss.apps.data.collection.features.handlers.mobility.MobilityMitigationHandler;
import com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.RollbackHandler;
import com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.UplinkPowerMitigationChangeHandler;
import com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.UplinkPowerMitigationRequestHandler;
import com.ericsson.oss.apps.data.collection.features.report.ReportCreator;
import com.ericsson.oss.apps.data.collection.pmbaseline.PmBaselineLoaderHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(properties = {"cell-selection-config.use-weather-data=false"})
class FeatureCalculatorItNoWeatherTest extends FeatureCalculatorItTest {

    List<Class<? extends FeatureHandler<FeatureContext>>> getExpectedHandlerList() {
        return List.of(
                AllowedMoLoaderHandler.class,
                PmBaselineLoaderHandler.class,
                ExpiredRopDataRemoverHandler.class,
                KpiCalculator.class,
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
                MobilityMitigationHandler.class,
                UplinkPowerMitigationRequestHandler.class,
                RollbackHandler.class,
                UplinkPowerMitigationChangeHandler.class,
                ReportCreator.class
        );
    }

    @Test
    void testHandlersNormalExec() {
        testHandlers();
    }

}