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
 * The Class KafkaConfigRim Consumer Data.
 * Holds Kafka consumer properties from application.yaml
 */
@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "spring.kafka.consumer")
public class KafkaConfigRimConsumerData {
    private String groupId;
    private String autoOffsetReset;
    private String partitionAssignmentStrategy;
    private int concurrency;
    private int maxPollRecords;
    private int sessionTimeoutMs;
    private int maxPollReconnectTimeoutMs;
    private int maxPollIntervalMs;
    private int retryBackoffMs;
    private int reconnectBackoffMs;
    private int reconnectBackoffMaxMs;
    private int requestTimeoutMs;
}