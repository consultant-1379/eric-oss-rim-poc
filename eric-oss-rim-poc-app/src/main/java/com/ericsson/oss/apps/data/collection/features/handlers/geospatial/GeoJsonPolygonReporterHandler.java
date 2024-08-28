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

import com.ericsson.oss.apps.config.DuctDetectionConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cell-selection-config", name = "use-weather-data", havingValue = "true")
public class GeoJsonPolygonReporterHandler implements FeatureHandler<FeatureContext> {

    private final DuctDetectionConfig ductDetectionConfig;
    private final GeoJsonPolygonReporter geoJsonPolygonReporter;

    @Override
    @Timed
    public void handle(FeatureContext context) {
        try {
            if (context.getLatestCoverage() == null) {
                log.error("Cannot produce refractive index polygons for ROP {}, no weather data available", context.getRopTimeStamp());
                return;
            }
            DuctStrengthCalculator ductStrengthCalculator = new DuctStrengthCalculator(context.getLatestCoverage(), ductDetectionConfig);
            geoJsonPolygonReporter.reportGeoJsonPolygons(ductStrengthCalculator.getMinStrengthAndDuctingPolygonsList(), context.getRopTimeStamp());
        } catch (AwsServiceException e) {
            log.error("Cannot save polygons for rop {}!", context.getRopTimeStamp(), e);
        }
    }

    @Override
    public int getPriority() {
        return 9;
    }

}
