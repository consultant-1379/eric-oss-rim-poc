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
package com.ericsson.oss.apps.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.apps.CoreApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = { CoreApplication.class, CellSelectionConfig.class })
class ReportingConfigTests {
    @Autowired
    private ReportingConfig reportingConfig;

    @Test
    void thresholdConfig_test() {
        log.info("Testing 'reporting' from application.yaml");
        assertEquals(100000, reportingConfig.getMaxNumberRecordsPerOutputFile(), "Incorrect value for 'maxNumberRecordsPerOutputFile'");
    }

}
