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
import com.ericsson.oss.apps.config.DuctDetectionConfig;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cell-selection-config", name = "use-weather-data", havingValue = "true")
public class GeoTiffSelector implements FeatureHandler<FeatureContext> {

    public static final long MIN_MILLIS = 60000;

    private final DuctDetectionConfig ductDetectionConfig;

    private GridCoverage2D latestCoverage;

    private final GeoTiffLoader geoTiffLoader;
    private final FileTracker fileTracker;
    private final WeatherDataProvider weatherDataProvider;

    @Override
    @Timed
    public void handle(FeatureContext context) {
        try {
            loadCoverageForRop(context.getRopTimeStamp());
            context.setLatestCoverage(latestCoverage);
        } catch (IOException | AwsServiceException e) {
            log.error("Cannot access weather information for ROP {}", context.getRopTimeStamp(), e);
        }
    }

    /**
     * Loaded the GeoTiff file that corresponds to the ROP.
     * Note that the geoTiff are produced once every 3 hours, so the nearest geotiff must be
     * selected.
     * <p>
     * Check if the file is already loaded.
     * If it is, don't reload it.
     *
     * @param ropTimestamp rop timestamp geotiff file is required for
     * @throws IOException propagated from GeoTools geotiff loading && weatherDataProvider.
     */
    private void loadCoverageForRop(long ropTimestamp) throws IOException {
        log.trace("GeoTiff latest Coverage BEFORE loading new ROP is '{}' ", latestCoverage == null ? "null" : latestCoverage.toString());
        log.info("Loading ducting forecast for ROP {}", ropTimestamp);
        long predictionTimestamp = getNearestDuctingPrediction(ropTimestamp);

        final String currentEtag = weatherDataProvider.getETag(predictionTimestamp);
        if (fileTracker.fileAlreadyLoaded(currentEtag, Long.toString(predictionTimestamp))) {
            log.info("GeoTiff data for ROP {} already loaded from geodata with timestamp {} ({}}, will not reload it.", ropTimestamp, predictionTimestamp, currentEtag);
            log.trace("GeoTiff latest Coverage loaded is '{}'", latestCoverage.toString());
            return;
        }

        try (InputStream inputStream = weatherDataProvider.getObjectInputStream(predictionTimestamp)) {
            latestCoverage = geoTiffLoader.loadCoverage(inputStream);
            fileTracker.addEtagToMap(currentEtag, Long.toString(predictionTimestamp));
            log.trace("GeoTiff NEW Coverage loaded '{}' ", latestCoverage.toString());
        }
    }


    private long getNearestDuctingPrediction(long ropTimestamp) {
        long forecastRopMillis = ductDetectionConfig.getForecastRopMinutes() * MIN_MILLIS;
        long forecastAdvanceRopMillis = ductDetectionConfig.getForecastAdvanceMinutes() * MIN_MILLIS;
        long ropFromPrediction = ropTimestamp % forecastRopMillis;
        long prevPrediction = ropTimestamp - ropFromPrediction;
        return (ropFromPrediction > (forecastRopMillis - forecastAdvanceRopMillis) ? (forecastRopMillis + prevPrediction) : prevPrediction);
    }

}
