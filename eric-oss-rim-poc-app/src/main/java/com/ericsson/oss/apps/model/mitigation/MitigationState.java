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
package com.ericsson.oss.apps.model.mitigation;

import java.util.List;

public enum MitigationState {
    PENDING, CONFIRMED, CHANGE_FAILED, ROLLBACK_FAILED, ROLLBACK_SUCCESSFUL, OBSERVATION;

    public static final List<MitigationState> FAILED_STATES = List.of(CHANGE_FAILED, ROLLBACK_FAILED);
}
