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
package com.ericsson.oss.apps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The Class KafkaConfigRimProducerData.
 * Holds Kafka producer properties from application.yaml
 */
@Getter
@Setter

/**
 * To string.
 *
 * @return the java.lang. string
 */
@ToString
@Configuration
@ConfigurationProperties(prefix = "spring.kafka.producer")
public class KafkaConfigRimProducerData {
    private String acksConfig;
    private int retriesConfig;
    private int batchSizeConfig;
    private int lingerMsConfig;
    private int bufferMemoryConfig;
    private int maxInFlightRequestsPerConnection;
}