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
package com.ericsson.oss.apps.data.collection.features.handlers.geospatial;

import com.ericsson.oss.apps.client.CtsClient;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.model.GeoData;
import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefractiveIndexCalculatorTest {

    private static GridCoverage2D gridCoverage2D;
    @InjectMocks
    private RefractiveIndexCalculator refractiveIndexCalculator;

    @Mock
    CtsClient ctsClient;

    private FeatureContext featureContext;

    @BeforeAll
    static void loadCoverage() throws IOException {
        gridCoverage2D = GeotiffTestUtils.loadGridCoverage2D(GeotiffTestUtils.GEO_TIFF_FILE_PATH);
    }

    @BeforeEach
    void setup() {
        featureContext = new FeatureContext(0L);
        FtRopNRCellDU ftRopNRCellDU = new FtRopNRCellDU(new MoRopId(FDN1, 0L));
        featureContext.getFdnToFtRopNRCellDU().put(FDN1, ftRopNRCellDU);
        featureContext.setLatestCoverage(gridCoverage2D);
    }

    /**
     * given
     * a cells in context
     * cts client returns valid coordinates
     * the cell has a valid refractive index in the weather data
     * when
     * the handler is invoked
     * then
     * the cell refractive index field is populated to the required value
     */
    @Test
    void handle() {
        when(ctsClient.getNrCellGeoData(FDN1)).thenReturn(Optional.of(GeoData.builder().coordinate(new Coordinate(3.4974, 42.6313)).fdn(FDN1).build()));
        refractiveIndexCalculator.handle(featureContext);
        assertEquals(75.7329, featureContext.getFtRopNRCellDU(FDN1).getRefractiveIndex(), 0.0001);
    }

    /**
     * given
     * a cells in context
     * cts client returns no data
     * when
     * the handler is invoked
     * then
     * the cell refractive index field is not populated (NaN)
     */
    @Test
    void handleNoCtsData() {
        when(ctsClient.getNrCellGeoData(FDN1)).thenReturn(Optional.empty());
        refractiveIndexCalculator.handle(featureContext);
        assertTrue(Double.isNaN(featureContext.getFtRopNRCellDU(FDN1).getRefractiveIndex()));
    }

    /**
     * given
     * a cells in context
     * cts client returns data with no coordinates
     * when
     * the handler is invoked
     * then
     * the cell refractive index field is not populated (NaN)
     */
    @Test
    void handleNoCoordinates() {
        when(ctsClient.getNrCellGeoData(FDN1)).thenReturn(Optional.of(GeoData.builder().fdn(FDN1).build()));
        refractiveIndexCalculator.handle(featureContext);
        assertTrue(Double.isNaN(featureContext.getFtRopNRCellDU(FDN1).getRefractiveIndex()));
    }

    /**
     * given
     * a cells in context
     * the cell does not have a valid refractive index in the weather data
     * when
     * the handler is invoked
     * then
     * the cell refractive index field is not populated (NaN)
     */
    @Test
    void handleNoRefractiveIndex() {
        when(ctsClient.getNrCellGeoData(FDN1)).thenReturn(Optional.of(GeoData.builder().coordinate(new Coordinate(0, 90)).fdn(FDN1).build()));
        refractiveIndexCalculator.handle(featureContext);
        assertTrue(Double.isNaN(featureContext.getFtRopNRCellDU(FDN1).getRefractiveIndex()));
    }

    /**
     * given
     * a cells in context
     * there is no weather data
     * when
     * the handler is invoked
     * then
     * the cell refractive index field is not populated (NaN)
     * there is no interaction with the cts mock
     */
    @Test
    void handleNoWeatherData() {
        featureContext.setLatestCoverage(null);
        refractiveIndexCalculator.handle(featureContext);
        assertTrue(Double.isNaN(featureContext.getFtRopNRCellDU(FDN1).getRefractiveIndex()));
        verify(ctsClient, never()).getNrCellGeoData(FDN1);
    }

}