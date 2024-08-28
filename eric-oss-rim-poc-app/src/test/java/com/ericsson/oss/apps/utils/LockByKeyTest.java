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
package com.ericsson.oss.apps.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LockByKeyTest {

    @Test
    void lock() {
        LockByKey<String> lockByKey = new LockByKey<>();
        lockByKey.lock("key");
        AtomicInteger atomicInteger = new AtomicInteger(1);
        new Thread(() -> {
            lockByKey.lock("key");
            atomicInteger.incrementAndGet();
            lockByKey.unlock("key");
        }).start();
        Utils.of().waitRetryInterval(100);
        assertEquals(1, atomicInteger.get());
        lockByKey.unlock("key");
        await().atMost(Duration.of(100, ChronoUnit.MILLIS)).pollInterval(Duration.ofMillis(10))
                .untilAsserted(() -> assertEquals(2, atomicInteger.get()));
    }

}