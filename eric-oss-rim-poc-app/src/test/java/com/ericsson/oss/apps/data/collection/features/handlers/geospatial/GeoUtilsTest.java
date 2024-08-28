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
import com.github.jknack.handlebars.internal.lang3.builder.EqualsBuilder;
import lombok.val;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.process.ProcessException;
import org.jaitools.numeric.Range;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static com.ericsson.oss.apps.data.collection.features.handlers.geospatial.GeoUtils.*;
import static com.ericsson.oss.apps.data.collection.features.handlers.geospatial.GeotiffTestUtils.getDuctDetectionConfig;
import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {
    private DuctDetectionConfig ductDetectionConfig;
    private static GridCoverage2D gridCoverage2D;
    private static Range<Integer> range;

    @BeforeEach
    void setUp() {
        ductDetectionConfig = getDuctDetectionConfig();
    }

    @BeforeAll
    static void loadGeoTiff() throws IOException {
        gridCoverage2D = GeotiffTestUtils.loadGridCoverage2D(GeotiffTestUtils.GEO_TIFF_FILE_PATH);
    }

    @BeforeAll
    static void setRange() {
        range = new Range<>(35, true, 1000, false);
    }

    @Test
    void createEnvelopeTest() {
        val resultEnvelope = createEnvelope(ductDetectionConfig, gridCoverage2D);
        val upperCorner = new DirectPosition2D(ductDetectionConfig.getMaxLon(), ductDetectionConfig.getMaxLat());
        val lowerCorner = new DirectPosition2D(ductDetectionConfig.getMinLon(), ductDetectionConfig.getMinLat());

        assertTrue(EqualsBuilder.reflectionEquals(resultEnvelope.getUpperCorner().getCoordinate()
                , upperCorner.getDirectPosition().getCoordinate()));

        assertTrue(EqualsBuilder.reflectionEquals(resultEnvelope.getLowerCorner().getCoordinate()
                , lowerCorner.getDirectPosition().getCoordinate()));
    }

    @Test
    void cropCoverageTest() {
        final double TOLERANCE = 0.125;
        val resultGrid = cropCoverage(gridCoverage2D, createEnvelope(ductDetectionConfig, gridCoverage2D));
        val resultEnvelope = createEnvelope(ductDetectionConfig, gridCoverage2D);

        assertEquals(resultGrid.getEnvelope().getUpperCorner().getCoordinate()[0], resultEnvelope.getUpperCorner().getCoordinate()[0], TOLERANCE);
        assertEquals(resultGrid.getEnvelope().getUpperCorner().getCoordinate()[1], resultEnvelope.getUpperCorner().getCoordinate()[1], TOLERANCE);
        assertEquals(resultGrid.getEnvelope().getLowerCorner().getCoordinate()[0], resultEnvelope.getLowerCorner().getCoordinate()[0], TOLERANCE);
        assertEquals(resultGrid.getEnvelope().getLowerCorner().getCoordinate()[1], resultEnvelope.getLowerCorner().getCoordinate()[1], TOLERANCE);
    }


    @Test
    void calculatePolygonsTest() {
        assertThrows(ProcessException.class, () -> calculatePolygons(range, null));
    }

}
