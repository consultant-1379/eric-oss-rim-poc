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
package com.ericsson.oss.apps.data.collection.features.handlers.geospatial;

import com.ericsson.oss.apps.config.DuctDetectionConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static com.ericsson.oss.apps.data.collection.features.handlers.geospatial.GeotiffTestUtils.getDuctDetectionConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class GeoJsonPolygonReporterHandlerTest {

    GeoJsonPolygonReporterHandler geoJsonPolygonReporterHandler;
    @Mock
    private GeoJsonPolygonReporter geoJsonPolygonReporter;
    static GridCoverage2D gridCoverage2D;

    FeatureContext featureContext;

    @BeforeAll
    static void loadGRidCoverage() throws IOException {
        gridCoverage2D = GeotiffTestUtils.loadGridCoverage2D(GeotiffTestUtils.GEO_TIFF_FILE_PATH);
    }

    @BeforeEach
    void setUp() {
        featureContext = new FeatureContext(1234L);
        DuctDetectionConfig ductDetectionConfig = getDuctDetectionConfig();
        geoJsonPolygonReporterHandler = new GeoJsonPolygonReporterHandler(ductDetectionConfig, geoJsonPolygonReporter);
    }

    @Test
    void handle() {
        featureContext.setLatestCoverage(gridCoverage2D);
        geoJsonPolygonReporterHandler.handle(featureContext);
        verify(geoJsonPolygonReporter).reportGeoJsonPolygons(any(), eq(featureContext.getRopTimeStamp()));
    }

    @Test
    void handleNoWeatherData() {
        geoJsonPolygonReporterHandler.handle(featureContext);
        verifyNoMoreInteractions(geoJsonPolygonReporter);
    }
}