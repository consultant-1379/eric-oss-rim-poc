/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.apps;

import java.sql.SQLException;

import javax.annotation.PostConstruct;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/**
 * Core Application, the starting point of the application.
 */
@Slf4j
@EnableCaching
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableJpaRepositories(basePackages = "com.ericsson.oss.apps.repositories")
@EnableRetry
public class CoreApplication {

    @Value("${app.data.pm.rop.csv.usePreCalculatedAvgDeltaIpN}")
    private boolean usePreCalculatedAvgDeltaIpN;

    @Value("${spring.kafka.mode.enabled}")
    private boolean kafkaModeEnabled;

    @Autowired
    BuildProperties buildProperties;

    /**
     * Main entry point of the application.
     *
     * @param args
     *     Command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }

    /**
     * Configuration bean for Web MVC.
     *
     * @return WebMvcConfigurer
     */
    @Bean
    public WebMvcConfigurer webConfigurer() {
        return new WebMvcConfigurer() {
        };
    }

    /**
     * Making a RestTemplate, using the RestTemplateBuilder, to use for consumption of RESTful interfaces.
     *
     * @param restTemplateBuilder
     *     RestTemplateBuilder instance
     * @param okHttpClient
     *     OkHttpClient instance
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder, final OkHttpClient okHttpClient) {
        return restTemplateBuilder.requestFactory(() -> new OkHttp3ClientHttpRequestFactory(okHttpClient)).build();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnExpression("${spring.datasource.exposed:false}")
    public Server inMemoryH2DatabaseaServer() throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9090");
    }

    @PostConstruct
    private void appVersion() {
        log.info("RIM Application Info: name '{}', group '{}' version '{}', build timestamp (UTC) '{}', Kafka Mode Enabled = {}, Use PreCalculated AvgDeltaIpN = {}", buildProperties.getName(),
            buildProperties.getGroup(), buildProperties.getVersion(), buildProperties.getTime().toString(),kafkaModeEnabled, usePreCalculatedAvgDeltaIpN);
    }
}
