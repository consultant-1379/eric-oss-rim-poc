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
package com.ericsson.oss.apps.api.retries;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Configuration
public class RetriesConfiguration {

    @Value("${client.retries.apiRetries}")
    int apiRetries;

    @Value("${client.retries.serverErrorWaitMs}")
    int serverErrorWaitMs;

    @Value("${client.retries.serverErrorMaxWaitMs}")
    int serverErrorMaxWaitMs;


    @Bean
    public RetryOperationsInterceptor customRetryInterceptor() {
        ExponentialBackOffPolicy exponentialBackOffPolicy = new  ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(serverErrorWaitMs);
        exponentialBackOffPolicy.setMaxInterval(serverErrorMaxWaitMs);
        return RetryInterceptorBuilder
                .stateless()
                .retryPolicy(new SimpleRetryPolicy(apiRetries, new RestClientExceptionClassifier(true)))
                .backOffPolicy(exponentialBackOffPolicy)
                .build();
    }
}
