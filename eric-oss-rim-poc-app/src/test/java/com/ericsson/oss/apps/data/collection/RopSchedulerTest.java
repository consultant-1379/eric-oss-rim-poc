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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.ericsson.oss.apps.data.collection.features.FeatureCalculator;
import com.ericsson.oss.apps.data.collection.pmrop.PmRopLoader;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;


@Slf4j
@SpringBootTest(properties = { "app.data.pm.rop.scheduler.cron=* * * * * *", "spring.kafka.mode.enabled=false" })
class RopSchedulerTest {

    private final static String CUSTOMER_ID = "200238";

    @SpyBean
    private RopScheduler ropScheduler;

    @MockBean
    private PmRopLoader<PmRopNRCellDU> nrCellDuRopLoader;

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

    /**
     * Test to check that kafka listener is NOT started when the Spring boot context has spring.kafka.mode.enabled=false
     * Put it here so an not to have to create new class with @SpringBootTest annotation.
     */
    @Test
    void kafkaListenerNotStarted() {
        log.info("kafkaModeEnabled = '{}'", kafkaModeEnabled);
        assertFalse(kafkaModeEnabled);
        MessageListenerContainer messageListenerContainer = registry.getListenerContainer(listenerId);
        assertNull(messageListenerContainer);
    }

    @Test
    void scheduleIsTriggered() {
        await().atMost(Duration.of(2000, ChronoUnit.MILLIS))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(ropScheduler, atLeast(1)).loadAndProcessRopData());
    }

    @Test
    void timeWarpCalculation() throws IOException {
        ropScheduler.loadAndProcessRopData(1659709870000L, 96, CUSTOMER_ID);
        verify(nrCellDuRopLoader).loadPmRop(1659623400000L, CUSTOMER_ID);
        verify(featureCalculator).calculateFeatures(1659623400000L, CUSTOMER_ID);
    }

    @Test
    void asyncLoadingParamMapping() {
        ropScheduler.loadAndProcessRopDataAsync(1659709870000L, CUSTOMER_ID, 2);
        await().atMost(Duration.of(1000, ChronoUnit.MILLIS))
                .pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> {
                    verify(ropScheduler, times(2)).loadAndProcessRopData(ropTimeStampCaptor.capture(),
                            timeWarpCaptor.capture(),
                            customerIdCaptor.capture());
                    assertTrue(timeWarpCaptor.getAllValues().stream().allMatch(timeWarp -> timeWarp == 0));
                    assertTrue(customerIdCaptor.getAllValues().stream().allMatch(customerId -> customerId.equals(CUSTOMER_ID)));
                    assertEquals(1659709870000L, ropTimeStampCaptor.getAllValues().get(0));
                    assertEquals(1659709870000L + 900000, ropTimeStampCaptor.getAllValues().get(1));
                });
    }
}