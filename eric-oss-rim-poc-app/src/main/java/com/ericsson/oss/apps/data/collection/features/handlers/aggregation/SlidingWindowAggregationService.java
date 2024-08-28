/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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
package com.ericsson.oss.apps.data.collection.features.handlers.aggregation;

import static com.ericsson.oss.apps.data.collection.RopScheduler.ROP_MILLIS;

import com.ericsson.oss.apps.config.RopProcessingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlidingWindowAggregationService {

    private final EntityManager entityManager;
    private final RopProcessingConfig ropProcessingConfig;

    // annotation related to https://github.com/spring-projects/spring-data-jpa/issues/1369

    /**
     * Calculates long sliding window average for avgdeltaipn, long and short sliding window for average uplink throughput, see
     * {@link  com.ericsson.oss.apps.data.collection.features.handlers.aggregation.PmSWNRCellDU  PmSWNRCellDU} for the query.
     * Those are 8 and 2 ROPs in the original implementation of the algorithm, but can be changed by configuration if required.
     *
     * @param currentRopTime the current ROP time, will be included in the sliding window calculation
     * @return a list of {@link  com.ericsson.oss.apps.data.collection.features.handlers.aggregation.PmSWNRCellDU  PmSWNRCellDU}
     */
    @Transactional
    public List<PmSWNRCellDU> calculateSlidingWindowCounters(long currentRopTime) {
        try {
            TypedQuery<PmSWNRCellDU> query = entityManager.createNamedQuery("SWNRCellDU", PmSWNRCellDU.class);
            long slidingWindow8RopTime = currentRopTime - ROP_MILLIS * (ropProcessingConfig.getLongSlidingWindowRops() - 1L);
            long slidingWindow2RopTime = currentRopTime - ROP_MILLIS * (ropProcessingConfig.getShortSlidingWindowRops() - 1L);
            long lastSeenWindowRopsTime = currentRopTime - ROP_MILLIS * (ropProcessingConfig.getLastSeenWindowRops() - 1L);
            query.setParameter(1, slidingWindow2RopTime);
            query.setParameter(2, slidingWindow2RopTime);
            query.setParameter(3, slidingWindow8RopTime);
            query.setParameter(4, currentRopTime);
            query.setParameter(5, lastSeenWindowRopsTime);
            query.setParameter(6, ropProcessingConfig.getMinValidSymbolDeltaIpnSteps());
            query.setParameter(7, ropProcessingConfig.getMinValidMaxDeltaIpnSteps());
            query.setParameter(8, ropProcessingConfig.getMinValidSymbolDeltaIpnSteps());
            return query.getResultStream().toList();
        } catch (Exception exception) {
            log.error("Unable to compute fdn To Sliding Window CountersMap for roptTime {}, Reason {} , Exception ", currentRopTime,
                    exception.getMessage(), exception);
            return Collections.emptyList();
        }
    }


}
