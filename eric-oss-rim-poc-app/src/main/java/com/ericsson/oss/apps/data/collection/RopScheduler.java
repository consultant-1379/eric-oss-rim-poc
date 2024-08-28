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

import com.ericsson.oss.apps.data.collection.features.FeatureCalculator;
import com.ericsson.oss.apps.data.collection.pmrop.PmRopLoader;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.IntStream;

@Component
@Slf4j
public class RopScheduler {

    public static final int ROP_MILLIS = 900000;
    @Value("${app.data.timewarp}")
    private long timewarp;

    @Value("${app.data.customerid}")
    private String customerId;

    @Value("${spring.kafka.mode.enabled}")
    protected boolean kafkaModeEnabled;

    @Autowired
    PmRopLoader<PmRopNRCellDU> nrCellDuDBRopLoader;

    @Autowired
    FeatureCalculator featureCalculator;

    @Scheduled(cron = "${app.data.pm.rop.scheduler.cron}")
    public void loadAndProcessRopData() {
        loadAndProcessRopData(System.currentTimeMillis(), timewarp, customerId);
    }


    @VisibleForTesting
    public void loadAndProcessRopData(long now, long timewarp, String customerId) {
        long ropTimestamp = (now - now % ROP_MILLIS) - timewarp * ROP_MILLIS;
        if (!kafkaModeEnabled) {
            nrCellDuDBRopLoader.loadPmRop(ropTimestamp, customerId);
            featureCalculator.calculateFeatures(ropTimestamp, customerId);
        }
    }

    @Async
    public void loadAndProcessRopDataAsync(long now, String customerId, int nRops) {
        if (kafkaModeEnabled) {
            throw new NotImplementedException("Only scheduled processing is supported when Kafka mode is enabled.");
        }
        IntStream.range(0, nRops).forEach(nrop -> {
            long rop = now + (long) ROP_MILLIS * nrop;
            try {
                loadAndProcessRopData(rop, 0, customerId);
            } catch (Exception e) {
                log.error("cannot process ROP {}", rop, e);
            }
        });
    }
}


