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
package com.ericsson.oss.apps.data.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;


@Slf4j
class RopSchedulerBasicTest {

    private final static String CUSTOMER_ID = "200238";

    @Value("${spring.kafka.mode.enabled}")
    protected boolean kafkaModeEnabled;

    @Test
    void kafkaListenerNotStartedLoadAndProcessRopDataAsync() {
        RopScheduler ropScheduler = new RopScheduler();
        ReflectionTestUtils.setField(ropScheduler, "kafkaModeEnabled", true);
        String exceptionMessage = assertThrows(NotImplementedException.class,
            () -> ropScheduler.loadAndProcessRopDataAsync(1659709870000L, CUSTOMER_ID, 96), "Expected NotImplementedException").getMessage();
        log.info("EXCEPTION_MESSAGE is '{}'", exceptionMessage);
        assertThat(exceptionMessage).contains("Only scheduled processing is supported when Kafka mode is enabled");
        ReflectionTestUtils.setField(ropScheduler, "kafkaModeEnabled", false);
    }

}