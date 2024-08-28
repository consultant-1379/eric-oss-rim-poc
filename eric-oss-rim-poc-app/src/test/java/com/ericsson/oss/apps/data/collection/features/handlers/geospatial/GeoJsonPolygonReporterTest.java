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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.config.DuctDetectionConfig;
import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class GeoJsonPolygonReporterTest {

    @Mock
    private BdrClient bdrClient;
    @Mock
    private BdrConfiguration bdrConfiguration;
    @InjectMocks
    private GeoJsonPolygonReporter geoJsonPolygonReporter;
    private DuctDetectionConfig ductDetectionConfig;

    @Captor
    ArgumentCaptor<String> bucketNameCaptor;

    @Captor
    ArgumentCaptor<String> objectNameCaptor;

    @Captor
    ArgumentCaptor<byte[]> argumentCaptor;

    List<Integer> rangeList = Arrays.asList(35, 43, 55, 65, 75, 95, 110, 150);

    @BeforeEach
    void setup() {
        ductDetectionConfig = new DuctDetectionConfig();
        ductDetectionConfig.setMaxLon(8d);
        ductDetectionConfig.setMinLon(-5d);
        ductDetectionConfig.setMaxLat(51d);
        ductDetectionConfig.setMinLat(42d);
        ductDetectionConfig.setMinAvgDeltaIpn(3D);
        ductDetectionConfig.setDuctStrengthRanges(rangeList);
    }

    /**
     * given a list of range and polygons is available
     * when geoJsonPolygonReporter.reportGeoJsonPolygons in invoked on the list of polygons and a timestamp
     * then
     * - the bdrclient is invoked wit the right path
     * - the input stream corresponds to valid reference geojson
     */
    @Test
    void reportGeoJsonPolygons() throws IOException {
        GridCoverage2D gridCoverage2D = GeotiffTestUtils.loadGridCoverage2D(GeotiffTestUtils.GEO_TIFF_FILE_PATH);
        DuctStrengthCalculator ductStrengthCalculator = new DuctStrengthCalculator(gridCoverage2D, ductDetectionConfig);
        var rangeAndDuctingPolygonList = ductStrengthCalculator.getMinStrengthAndDuctingPolygonsList();
        when(bdrConfiguration.getBucket()).thenReturn("RIM");
        geoJsonPolygonReporter.reportGeoJsonPolygons(rangeAndDuctingPolygonList, 1234L);
        verify(bdrClient, times(8)).uploadInputStreamObject(bucketNameCaptor.capture(), objectNameCaptor.capture(), argumentCaptor.capture());
        for (int i = 0; i < rangeList.size(); i++) {
            assertEquals("RIM", bucketNameCaptor.getAllValues().get(i));
            assertEquals(String.format("geospatial/polygons/ducting-polygons-%d-%d.json", rangeList.get(i), 1234L), objectNameCaptor.getAllValues().get(i));
        }

    }
}