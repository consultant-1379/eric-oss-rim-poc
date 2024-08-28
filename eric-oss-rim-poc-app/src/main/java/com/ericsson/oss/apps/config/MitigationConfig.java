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
@ConfigurationProperties(prefix = "mitigation")
public class MitigationConfig {

    private long observationWindowMs;

    private NumericParameterConfig<Integer> cellIndividualOffset = new NumericParameterConfig<>();

    private NumericParameterConfig<Integer> pZeroNomPuschGrantDb = new NumericParameterConfig<>();

    private NumericParameterConfig<Integer> pZeroUePuschOffset256QamDb = new NumericParameterConfig<>();

}