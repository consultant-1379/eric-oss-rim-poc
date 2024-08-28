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


package com.ericsson.oss.apps.data.collection.features;

import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import javax.annotation.PostConstruct;

/**
 * this class is used to group together feature execution
 * once multiple features are calculated it is worth checking
 * if we can extract common interfaces and make it more flexible
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureCalculator {

    private final List<FeatureHandler<FeatureContext>> featureHandlers;


    @PostConstruct
    void setup() {
        featureHandlers.sort(Comparator.comparingInt(FeatureHandler::getPriority));
    }

    public FeatureContext calculateFeatures(long ropTimestamp, String customerId) {
        log.info("Calculating features for ROP {} and customer ID {}", ropTimestamp, customerId);

        FeatureContext featureContext = new FeatureContext(ropTimestamp);
        for (FeatureHandler<FeatureContext> handler : featureHandlers) {
            handler.handle(featureContext);
            if (handler.isLast(featureContext)) {
                break;
            }
        }
        log.info("Finished calculating features for ROP {} and customer ID {}", ropTimestamp, customerId);
        return featureContext;
    }
}
