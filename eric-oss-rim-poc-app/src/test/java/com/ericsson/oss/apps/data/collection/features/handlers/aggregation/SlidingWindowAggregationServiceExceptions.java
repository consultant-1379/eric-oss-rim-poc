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
package com.ericsson.oss.apps.data.collection.features.handlers.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
class SlidingWindowAggregationServiceExceptions {

    @Autowired
    SlidingWindowAggregationService slidingWindowAggregationService;


    @Test
    void calculateSlidingWindowCountersThrowsExceptionFail() {
        final EntityManager entityManagerMock = Mockito.mock(EntityManager.class);
        Mockito.when(entityManagerMock.createNamedQuery(Mockito.any(), Mockito.any())).thenThrow(new RimHandlerException("Test Query Exception"));
        ReflectionTestUtils.setField(slidingWindowAggregationService, "entityManager", entityManagerMock);
        List<PmSWNRCellDU> slidingWindowCounters = slidingWindowAggregationService.calculateSlidingWindowCounters(7200000);
        assertThat(slidingWindowCounters).isEmpty();
    }

}