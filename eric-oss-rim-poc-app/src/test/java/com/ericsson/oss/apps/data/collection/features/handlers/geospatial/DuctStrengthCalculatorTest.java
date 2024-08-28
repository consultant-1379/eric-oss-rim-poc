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
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.EmptyIntersectionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DuctStrengthCalculatorTest {

    private static GridCoverage2D gridCoverage2D;
    private DuctStrengthCalculator ductStrengthCalculator;
    private DuctDetectionConfig ductDetectionConfig;

    @BeforeEach
    void setUp() {
        ductDetectionConfig = new DuctDetectionConfig();
        ductDetectionConfig.setDuctStrengthRanges(Arrays.asList(35, 43, 55, 65, 75, 95, 110, 150));
        ductDetectionConfig.setMaxLon(8);
        ductDetectionConfig.setMinLon(-5);
        ductDetectionConfig.setMaxLat(51);
        ductDetectionConfig.setMinLat(42);
        ductDetectionConfig.setMinDetectedDuctStrength(43);
    }

    @BeforeAll
    static void loadGeoTiff() throws IOException {
        gridCoverage2D = GeotiffTestUtils.loadGridCoverage2D(GeotiffTestUtils.GEO_TIFF_FILE_PATH);
    }

    @Test
    void testThrowIfCroppedCoverageIsEmpty() {
        ductDetectionConfig.setMaxLat(200);
        ductDetectionConfig.setMinLat(200);
        assertThrows(EmptyIntersectionException.class,
                () -> new DuctStrengthCalculator(gridCoverage2D, ductDetectionConfig));
    }

    @Test
    void testCroppedCoverage() {
        ductStrengthCalculator = new DuctStrengthCalculator(gridCoverage2D, ductDetectionConfig);
        var croppedCoverage = ductStrengthCalculator.getCroppedCoverage();
        var lowerCornerCoordinates = croppedCoverage.getEnvelope().getLowerCorner().getCoordinate();
        var upperCornerCoordinates = croppedCoverage.getEnvelope().getUpperCorner().getCoordinate();
        assertEquals(-5.125, lowerCornerCoordinates[0], 0.1);
        assertEquals(41.875, lowerCornerCoordinates[1], 0.1);
        assertEquals(8.125, upperCornerCoordinates[0], 0.1);
        assertEquals(51.125, upperCornerCoordinates[1], 0.1);
        var rangeAndDuctingPolygonsList = ductStrengthCalculator.getMinStrengthAndDuctingPolygonsList();
        assertEquals(8, rangeAndDuctingPolygonsList.size());
        int[] nPolygonsForRange = new int[]{4,4,5,3,2,0,0,0};
        for (int i =0; i < nPolygonsForRange.length; i++)
        {
            assertEquals(nPolygonsForRange[i], rangeAndDuctingPolygonsList.get(i).getSecond().size());
        }
    }

    @Test
    void testNoDuctDetectedForValidCoordinatesOutOfPolygons() {
        ductStrengthCalculator = new DuctStrengthCalculator(gridCoverage2D, ductDetectionConfig);
        assertTrue(ductStrengthCalculator.getDuctStrength(new Coordinate(3.21, 43.54), new Coordinate(4.68, 43.02),Float.MAX_VALUE).isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "3.65, 42, 5, 43.25, 65, 100",
            "3.81, 42.28, 4.68, 43.02, 75, 100",
            "-4.9382, 43.9112,-1.2537, 45.6564, 43, 100",
            "3.65, 42, 5, 43.25, 55, 55",
    })
    void testDuctDetectedForValidCoordinates(double c1lon, double c1lat, double c2lon, double c2lat, double ductStrength, float maxValue) {
        ductStrengthCalculator = new DuctStrengthCalculator(gridCoverage2D, ductDetectionConfig);
        assertEquals(ductStrength, ductStrengthCalculator.getDuctStrength(new Coordinate(c1lon, c1lat), new Coordinate(c2lon, c2lat), maxValue).get());
    }

}