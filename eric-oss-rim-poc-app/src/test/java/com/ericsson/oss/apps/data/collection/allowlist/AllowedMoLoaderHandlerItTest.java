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

import static org.mockito.Mockito.verify;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {"app.data.customerid=customer"})
class AllowedMoLoaderHandlerItTest {

    @Autowired
    AllowedMoLoaderHandler allowedMoLoaderHandler;

    @MockBean
    private AllowedMoLoader<AllowedNrCellDu> allowedMoLoader;

    @Test
    void handle() {
        FeatureContext context = new FeatureContext(1234L);
        allowedMoLoaderHandler.handle(context);
        verify(allowedMoLoader).load("customer");
    }
}