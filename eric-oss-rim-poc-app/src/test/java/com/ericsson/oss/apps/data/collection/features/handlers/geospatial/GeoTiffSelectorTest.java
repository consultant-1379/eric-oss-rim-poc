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

import com.ericsson.oss.apps.client.WeatherDataProvider;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.config.DuctDetectionConfig;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GeoTiffSelectorTest {

    private static final String GRID_COVERAGE2D = "gridCoverage2D";
    private static final String E_TAG = "d41d8cd98f00b204e9800998ecf8427e-2";
    private final FileTracker fileTracker = new FileTracker();

    @Mock
    private DuctDetectionConfig ductDetectionConfig;

    @Mock
    private WeatherDataProvider weatherDataProvider;

    @Mock
    private BdrConfiguration bdrConfiguration;

    @Mock
    private InputStream inputStream;

    @Mock
    private GeoTiffLoader geoTiffLoader;

    @Mock
    GridCoverage2D gridCoverage2D;

    @InjectMocks
    private GeoTiffSelector geoTiffSelector;

    FeatureContext context;

    @BeforeEach
    public void setUpS3Mocks() throws IOException {
        Mockito.when(ductDetectionConfig.getForecastRopMinutes()).thenReturn(180);
        Mockito.when(ductDetectionConfig.getForecastAdvanceMinutes()).thenReturn(90);
        ReflectionTestUtils.setField(geoTiffSelector, "fileTracker", fileTracker);
        Mockito.when(weatherDataProvider.getETag(Mockito.anyLong())).thenReturn(E_TAG);
        Mockito.when(weatherDataProvider.getObjectInputStream(Mockito.anyLong())).thenReturn(inputStream);
        Mockito.when(geoTiffLoader.loadCoverage(inputStream)).thenReturn(gridCoverage2D);
    }

    /**
     * Happy Case test.
     * 1655219700000  = Tuesday, June 14, 2022 3:15:00 PM => 1655218800000  Tuesday, June 14, 2022 15:00:00 => ww14-15.tif ==>  /geospatial/ducting/ww-1655218800000.tif
     * 1655225100000  = Tuesday, June 14, 2022 4:45:00 PM => 1655229600000  Tuesday, June 14, 2022 18:00:00 => ww14-18.tif ==>  /geospatial/ducting/ww-1655229600000.tif
     */
    @ParameterizedTest
    @CsvSource(value = {"1655219700000, 1655218800000",
            "1655225100000, 1655229600000"})
    void getCoverageForPrevDucting(long ropTimestamp, long predictionTimestamp) throws IOException {
        context = new FeatureContext(ropTimestamp);
        fileTracker.getEtagsMap().clear();
        geoTiffSelector.handle(context);
        Mockito.verify(weatherDataProvider).getObjectInputStream(predictionTimestamp);
        Mockito.verify(geoTiffLoader).loadCoverage(inputStream);
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(Long.toString(predictionTimestamp)), "Expected ETAG not present in eTagMap");
        assertEquals(GRID_COVERAGE2D, context.getLatestCoverage().toString(), "Expected LatestCoverage to be " + GRID_COVERAGE2D);
    }

    @Test
    void loadSameGeoTiffTwiceCheckNotReLoaded() throws IOException {
        fileTracker.getEtagsMap().clear();
        long ropTimestamp = 1655219700000L;
        context = new FeatureContext(ropTimestamp);
        long predictionTimestamp = 1655218800000L;
        geoTiffSelector.handle(context);
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(Long.toString(predictionTimestamp)), "Expected ETAG not present in eTagMap");
        assertEquals(GRID_COVERAGE2D, context.getLatestCoverage().toString(), "Expected LatestCoverage to be " + GRID_COVERAGE2D);
        Mockito.verify(weatherDataProvider).getObjectInputStream(predictionTimestamp);
        geoTiffSelector.handle(context);
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(Long.toString(predictionTimestamp)), "Expected ETAG not present in eTagMap");
        assertEquals(GRID_COVERAGE2D, context.getLatestCoverage().toString(), "Expected LatestCoverage to be " + GRID_COVERAGE2D);
        Mockito.verify(weatherDataProvider).getObjectInputStream(predictionTimestamp);
    }

    @Test
    void getCoverageFailsOnLoading() throws IOException {
        fileTracker.getEtagsMap().clear();
        Mockito.when(geoTiffLoader.loadCoverage(inputStream)).thenThrow(IOException.class);
        long ropTimestamp = 1655218800000L + 7 * 900000L;
        context = new FeatureContext(ropTimestamp);
        geoTiffSelector.handle(context);
        Mockito.verify(geoTiffLoader).loadCoverage(inputStream);
        Mockito.verify(weatherDataProvider).getObjectInputStream(1655229600000L);
        assertTrue(fileTracker.getEtagsMap().isEmpty(), "Expected Empty eTagMap");
    }

}