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
package com.ericsson.oss.apps.data.collection.features;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class FeatureCalculatorTest {

    public static final String A_CUSTOMER_ID = "a customer id";
    public static final long ROP_TIME_STAMP = 0L;

    @Mock
    FeatureHandler<FeatureContext> handler1;
    @Mock
    FeatureHandler<FeatureContext> handler2;

    @Test
    void calculateFeatures() {
        when(handler1.getPriority()).thenReturn(1);
        when(handler2.getPriority()).thenReturn(2);
        List<FeatureHandler<FeatureContext>> handlerList = new ArrayList<>(List.of(handler2, handler1));
        FeatureCalculator featureCalculator = new FeatureCalculator(handlerList);
        featureCalculator.setup();
        featureCalculator.calculateFeatures(ROP_TIME_STAMP, A_CUSTOMER_ID);
        handlerList.forEach(handler -> verify(handler, times(1)).handle(any(FeatureContext.class)));
    }
}