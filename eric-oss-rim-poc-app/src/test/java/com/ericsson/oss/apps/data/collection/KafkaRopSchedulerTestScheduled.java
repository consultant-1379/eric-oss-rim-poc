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

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import com.ericsson.oss.apps.CoreApplication;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.mockito.Mockito.atLeast;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;


@SpringBootTest(classes = { CoreApplication.class, KafkaRopScheduler.class }, properties = { "app.data.pm.rop.kafka.cron=* * * * * *",
    "spring.kafka.mode.enabled=true" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class KafkaRopSchedulerTestScheduled {

    @SpyBean
    private KafkaRopScheduler kafkaRopScheduler;

    @Test
    void scheduleIsTriggered() {
        await().atMost(Duration.of(2000, ChronoUnit.MILLIS))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> verify(kafkaRopScheduler, atLeast(1)).processRopDataFromKafka());
    }

}