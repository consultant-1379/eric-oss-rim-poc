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
package com.ericsson.oss.apps.data.collection.allowlist;

import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.data.allow-list", name = "allow-all-fdn", havingValue = "false")
public class AllowedMoLoaderHandler implements FeatureHandler<FeatureContext> {

    @Value("${app.data.customerid}")
    private String customerId;

    @Autowired
    private final Collection<AllowedMoLoader<?>> allowedMoLoaders;

    @Override
    public void handle(FeatureContext context) {
        allowedMoLoaders.forEach(allowedMoLoader -> allowedMoLoader.load(customerId));
    }

    @Override
    public int getPriority() {
        return 1;
    }

}
