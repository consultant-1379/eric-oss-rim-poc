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


package com.ericsson.oss.apps.data.collection;

import com.ericsson.oss.apps.data.collection.features.FeatureCalculator;
import com.ericsson.oss.apps.data.collection.pmrop.PmRopLoader;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.utils.Utils;
import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaRopScheduler {

    private static final int WAIT_MS_30000 = 30000;
    private static final int ROP_MILLIS = 900000;

    @Value("${app.data.timewarp}")
    private long timewarp;

    @Value("${app.data.customerid}")
    private String customerId;

    @Value("${spring.kafka.mode.enabled}")
    private boolean kafkaModeEnabled;

    private int waitTime = WAIT_MS_30000;

    @Autowired
    PmRopLoader<PmRopNRCellDU> nrCellDuRopKafkaLoader;

    @Autowired
    FeatureCalculator featureCalculator;

    //TOOD: parameter to disable scheduler for 'Offline Kafka and CSV mode.
    @Scheduled(cron = "${app.data.pm.rop.kafka.cron}")
    public void processRopDataFromKafka() {
        long now = System.currentTimeMillis();
        long ropTimestamp = (now - now % ROP_MILLIS);
        log.info("Process RopData From Kafka: now = {}, ropTimestamp {}, customerId = {}", now, ropTimestamp, customerId);
        if (kafkaModeEnabled) {
            featureCalculator.calculateFeatures(ropTimestamp, customerId);
        }
    }

    @VisibleForTesting
    public long loadAndProcessRopData(long now, long timewarp, String customerId) {
        long ropTimestamp = (now - now % ROP_MILLIS) - timewarp * ROP_MILLIS;
        log.info("loadAndProcessRopData: now = {}, ropTimestamp {}, customerId = {}", now, ropTimestamp, customerId);
        nrCellDuRopKafkaLoader.loadPmRop(ropTimestamp, customerId);
        return ropTimestamp;
    }

    @Async
    public void loadAndProcessRopDataAsync(long now, String customerId, int nRops) {
        AtomicInteger i = new AtomicInteger(0);
        IntStream.range(0, nRops).forEach(nrop -> {
            log.info("loadAndProcessRopDataAsync: now = {}, now + ROP_MILLIS * nrop, = {}, customerId = {}, nRops = {}/{}", now,
                now + ROP_MILLIS * nrop, customerId,
                i.get(), nRops - 1);
            long ropTimestamp = loadAndProcessRopData(now + ROP_MILLIS * nrop, 0, customerId);
            Utils.of().waitRetryInterval(waitTime);
            featureCalculator.calculateFeatures(ropTimestamp, customerId);
            i.getAndIncrement();
        });
    }

}



