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
package com.ericsson.oss.apps.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tropoduct")
public class DuctDetectionConfig {
    private List<Integer> ductStrengthRanges;

    private int forecastRopMinutes;
    private int forecastAdvanceMinutes;
    private int minDetectedDuctStrength;
    private double minAvgDeltaIpn;

    // boundaries of the area/region of interest
    private double maxLon;
    private double maxLat;
    private double minLon;
    private double minLat;
    private int imageSize = 1600;
    private int maxDetectedCells;

    private boolean loadFromUrl;
    private String baseUrlAndPath;
    private int retryUrlTimes;
    private int backoffRetryUrlMilliSeconds;

}
