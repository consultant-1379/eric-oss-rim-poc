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

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.data.pm.rop")
public class RopProcessingConfig {
    private int longSlidingWindowRops;
    private int shortSlidingWindowRops;
    private int lastSeenWindowRops;
    private int minValidMaxDeltaIpnSteps;
    private int minValidSymbolDeltaIpnSteps;
    private double pmSWAvgSymbolDeltaIPNScalingFactor;
    private double otherInterferencePercThreshold;
    private double mixedInterferencePercThreshold;
    private int minRopCountForDetection;
}