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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.data.collection.features.FeatureCalculator;
import com.ericsson.oss.apps.data.collection.pmrop.PmRopLoader;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import static org.mockito.Mockito.times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;


@Slf4j
@SpringBootTest(classes = { CoreApplication.class, KafkaRopScheduler.class }, properties = { "spring.kafka.mode.enabled=true" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class KafkaRopSchedulerTest {

    private final static String CUSTOMER_ID = "tmo001";

    @SpyBean
    private KafkaRopScheduler kafkaRopScheduler;

    @MockBean
    PmRopLoader<PmRopNRCellDU> nrCellDuRopKafkaLoader;

    @MockBean
    private FeatureCalculator featureCalculator;

    @Captor
    private ArgumentCaptor<Long> ropTimeStampCaptor;

    @Captor
    private ArgumentCaptor<Long> timeWarpCaptor;

    @Captor
    private ArgumentCaptor<String> customerIdCaptor;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Value("${spring.kafka.mode.enabled}")
    protected boolean kafkaModeEnabled;

    @Value("${spring.kafka.listenerId}")
    private String listenerId;

    @Value("${app.data.pm.rop.kafka.cron}")
    private String kafkaCron;

    /**
     * Test to check that kafka listener is started when the Spring boot context has spring.kafka.mode.enabled=false
     * Put it here so an not to have to create new class with @SpringBootTest annotation.
     */
    @Test
    @Order(2)
    void kafkaListenerStarted() {
        log.info("kafkaModeEnabled = '{}'", kafkaModeEnabled);
        assertTrue(kafkaModeEnabled);
        MessageListenerContainer messageListenerContainer = registry.getListenerContainer(listenerId);
        assertNotNull(messageListenerContainer);
    }

    @Test
    @Order(3)
    void timeWarpCalculation() throws IOException {
        kafkaRopScheduler.loadAndProcessRopData(1659709870000L, 96, CUSTOMER_ID);
        verify(nrCellDuRopKafkaLoader).loadPmRop(1659623400000L, CUSTOMER_ID);
    }

    @Test
    @Order(4)
    void loadingParamMapping() {
        ReflectionTestUtils.setField(kafkaRopScheduler, "waitTime", 0);

        kafkaRopScheduler.loadAndProcessRopData(1659709870000L, 0L, CUSTOMER_ID);
        kafkaRopScheduler.processRopDataFromKafka();
        await().atMost(Duration.of(2000, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(50)).untilAsserted(() -> {
            verify(featureCalculator, times(1)).calculateFeatures(ropTimeStampCaptor.capture(), customerIdCaptor.capture());
        });
    }

    @Test
    @Order(1)
    void asyncLoadingParamMapping() {
        ReflectionTestUtils.setField(kafkaRopScheduler, "waitTime", 0);
        kafkaRopScheduler.loadAndProcessRopDataAsync(1659709870000L, CUSTOMER_ID, 2);
        await().atMost(Duration.of(2000, ChronoUnit.MILLIS))
                .pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> {
                verify(kafkaRopScheduler, times(2)).loadAndProcessRopData(ropTimeStampCaptor.capture(),
                            timeWarpCaptor.capture(),
                            customerIdCaptor.capture());
                assertTrue(timeWarpCaptor.getAllValues().stream().allMatch(timeWarp -> timeWarp == 0));
                assertTrue(customerIdCaptor.getAllValues().stream().allMatch(customerId -> customerId.equals(CUSTOMER_ID)));

                assertEquals(1659709870000L, ropTimeStampCaptor.getAllValues().get(0));
                assertEquals(1659709870000L + 900000, ropTimeStampCaptor.getAllValues().get(1));
                verify(featureCalculator).calculateFeatures(1659709800000L, CUSTOMER_ID);
                verify(featureCalculator).calculateFeatures(1659710700000L, CUSTOMER_ID);
            });
    }
}