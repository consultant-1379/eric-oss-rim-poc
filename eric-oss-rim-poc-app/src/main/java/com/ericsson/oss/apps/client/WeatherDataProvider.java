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
package com.ericsson.oss.apps.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;

import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.config.DuctDetectionConfig;
import com.ericsson.oss.apps.utils.TimeConverter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Setter
@RequiredArgsConstructor
public class WeatherDataProvider {

    private static final String SLASH = "/";
    private final DuctDetectionConfig ductDetectionConfig;
    private final BdrClient bdrClient;
    private final BdrConfiguration bdrConfiguration;
    private static final String ETAG_PREFIX = "fromUrl";
    //TODO: Update Wiremock in unit tests to use SSL, probably can remove this then.
    private boolean allowHttp = false;
    @Getter
    private String pathToWeatherData = null;


    /**
     * Gets the Weather data corresponding to the provided timestamp.
     *
     * @param predictionTimestamp
     *     the timestamp associated with the weather data
     *
     * @return the object input stream to the weather data.
     *
     * @throws IOException
     *     Signals that an I/O exception has occurred.
     */
    @Retryable(include = IOException.class, exclude = java.io.FileNotFoundException.class, maxAttemptsExpression = "${tropoduct.retryUrlTimes}", backoff = @Backoff(delayExpression = "${tropoduct.backoffRetryUrlMilliSeconds}"))
    public InputStream getObjectInputStream(long predictionTimestamp) throws IOException {
        pathToWeatherData = getObjectPath(predictionTimestamp);
        log.trace("WeatherDataProvider: predictionTimestamp = {}, pathToWeatherData = {}, isLoadFromUrl = {}", predictionTimestamp, pathToWeatherData, ductDetectionConfig.isLoadFromUrl());

        if (!ductDetectionConfig.isLoadFromUrl()) {
            log.info("WeatherDataProvider: Loading (from ObjectStore) ducting forecast for path {} to bucket {}", pathToWeatherData, bdrConfiguration.getBucket());
            return bdrClient.getObjectInputStream(bdrConfiguration.getBucket(), pathToWeatherData);
        }
        log.info("WeatherDataProvider: Loading (from url) ducting forecast for path {}", pathToWeatherData);
        URLConnection urlConnection = getUrlConnection();
        if (!allowHttp && !(urlConnection instanceof HttpsURLConnection)) {
            log.error("WeatherDataProvider ERROR: Not allowed to fetch data from non secure url: {} ", pathToWeatherData);
            return null;
        }
        return urlConnection.getInputStream();
    }

    /**
     * Recover from error, when retires exhausted. Method signature (including return) must be exactly like the @Retryable
     *
     * @param cause
     *     the cause
     * @param predictionTimestamp
     *     the timestamp of the weather data file
     *
     * @return the input stream
     *
     * @throws IOException
     *     Signals that an I/O exception has occurred.
     */
    @Recover
    public InputStream recover(Throwable cause, long predictionTimestamp) throws IOException {
        log.error("WeatherDataProvider ERROR: Unable to fetch data for timestamp '{}' from url '{}';  Retry (s) Exhausted: Exception = {}:", predictionTimestamp, getObjectPath(predictionTimestamp), cause.getClass().getName(), cause);
        return null;
    }

    /**
     * Gets the e tag.
     *
     * @param predictionTimestamp
     *     the timestamp of the weather data file
     *
     * @return the eTag
     */
    public String getETag(long predictionTimestamp) {
        pathToWeatherData = getObjectPath(predictionTimestamp);
        if (!ductDetectionConfig.isLoadFromUrl()) {
            return bdrClient.getETag(bdrConfiguration.getBucket(), pathToWeatherData);
        }
        return ETAG_PREFIX + predictionTimestamp;
    }

    protected URLConnection getUrlConnection() throws IOException {
        return new URL(pathToWeatherData).openConnection();
    }

    private String getObjectPath(long predictionTimestamp) {
        if (ductDetectionConfig.isLoadFromUrl()) {
            String geoFileName = TimeConverter.of().convertEpochToGeoFileFormat(predictionTimestamp);
            String objectPath = ductDetectionConfig.getBaseUrlAndPath() + SLASH + geoFileName;
            log.trace("WeatherDataProvider: Get Object Path (from url) for ducting forecast for path {}", objectPath);
            return objectPath;
        }
        String objectPath = String.format("%sgeospatial%sducting%sww-%d.tif", SLASH, SLASH, SLASH, predictionTimestamp);
        log.trace("WeatherDataProvider: Get Object Path (from ObjectStore) for ducting forecast for path {}", objectPath);
        return objectPath;
    }
}
