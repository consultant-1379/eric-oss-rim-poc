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
package com.ericsson.oss.apps.data.collection.features;

import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

abstract class FeatureCalculatorItTest {

    @Autowired
    FeatureCalculator featureCalculator;

    abstract List<Class<? extends FeatureHandler<FeatureContext>>> getExpectedHandlerList();

    void testHandlers() {
        List<FeatureHandler<FeatureContext>> featureHandlers = (List<FeatureHandler<FeatureContext>>)
                ReflectionTestUtils.getField(featureCalculator, "featureHandlers");
        Assertions.assertNotNull(featureHandlers);
        Iterator<FeatureHandler<FeatureContext>> handlers = featureHandlers.iterator();

        for (Class<? extends FeatureHandler<FeatureContext>> expectedHandler : getExpectedHandlerList()) {
            FeatureHandler<FeatureContext> handler = handlers.next();
            Assertions.assertEquals(expectedHandler, handler.getClass());
        }
        Assertions.assertThrows(NoSuchElementException.class, handlers::next);
    }

}