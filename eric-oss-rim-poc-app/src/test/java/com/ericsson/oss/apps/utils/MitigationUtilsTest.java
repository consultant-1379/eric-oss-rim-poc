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
package com.ericsson.oss.apps.utils;

import com.ericsson.oss.apps.model.mitigation.MitigationState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MitigationUtilsTest {

    @ParameterizedTest
    @CsvSource(value = {
            "true, true, ROLLBACK_SUCCESSFUL",
            "true, false, CONFIRMED",
            "false, true, ROLLBACK_FAILED",
            "false, false, CHANGE_FAILED",
    })
    void mapMitigationState(boolean isImplemented, boolean isRollback, MitigationState mitigationState) {
        assertEquals(mitigationState, MitigationUtils.mapMitigationState(isImplemented, isRollback));
    }
}