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

import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.model.GeoData;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
class AzimuthAffinityCalculatorTest {

    @InjectMocks
    AzimuthAffinityCalculator azimuthAffinityCalculator;
    @Spy
    BearingCalculator bearingCalculator;

    @ParameterizedTest
    @CsvSource(value = {
            "3.5, 43.59, 3.8, 43.61, 84.655",
            "3.8, 43.61, 3.5, 43.59, -95.138"
    })
    void testBearing(double lon1, double lat1, double lon2, double lat2, double result) {
        Coordinate c1 = new Coordinate(lon1, lat1);
        Coordinate c2 = new Coordinate(lon2, lat2);
        assertEquals(result, bearingCalculator.calculateBearing(c1, c2), 0.001);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "3.5, 43.59, 10, 3.8, 43.61, 10, -46.034, -46.034, 4 , 1, 5, 0",
            "3.5, 43.59, null, 3.8, 43.61, 10, NaN, NaN, 4 , 1, NaN, NaN",
            "3.5, 43.59, 10, 3.8, 43.61, null, NaN, NaN, NaN, NaN, NaN, NaN"
    }, nullValues = {"null"})
    void testHandle(double lon1, double lat1, Integer azimuth1,
                    double lon2, double lat2, Integer azimuth2,
                    double affinity1,
                    double affinity2,
                    double e_dtilts1, double m_dtilts1,
                    double e_dtilts2, double m_dtilts2) {
        FeatureContext context = new FeatureContext(0L);
        FtRopNRCellDUPair cellPair1 = buildFtRopNRCellDUPair(FDN1, FDN2);
        FtRopNRCellDUPair cellPair2 = buildFtRopNRCellDUPair(FDN2, FDN1);
        context.setFtRopNRCellDUPairs(Arrays.asList(cellPair1, cellPair2));
        GeoData geoData1 = new GeoData(FDN1, new Coordinate(lon1, lat1), azimuth1, e_dtilts1, m_dtilts1);
        GeoData geoData2 = new GeoData(FDN2, new Coordinate(lon2, lat2), azimuth2, e_dtilts2, m_dtilts2);
        context.getFdnToGeoDataMap().put(FDN1, geoData1);
        context.getFdnToGeoDataMap().put(FDN2, geoData2);
        azimuthAffinityCalculator.handle(context);
        assertEquals(affinity1, cellPair1.getAzimuthAffinity(), 0.001);
        assertEquals(affinity2, cellPair2.getAzimuthAffinity(), 0.001);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "10, 20, 10",
            "10, 0, 10",
            "-10, 340, 10",
            "-10, 30, 40",
            "-10, 180, 170",
            "180, 180, 0",
            "-90, 270, 0"
    })
    void testGetRelativeAzimuth(double bearing, double azimuth, double result) {
        assertEquals(result, AzimuthAffinityCalculator.getRelativeAzimuth(bearing, azimuth));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "0, 0",
            "15, -1.25",
            "120, -30",
            "180, -30"
    })
    void testGetRelativeAzimuth(double azimuth, double result) {
        assertEquals(result, AzimuthAffinityCalculator.getHgain(azimuth));
    }

}