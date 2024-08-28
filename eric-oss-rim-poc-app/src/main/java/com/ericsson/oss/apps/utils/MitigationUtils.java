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

import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import static com.ericsson.oss.apps.model.mitigation.MitigationState.*;

@UtilityClass
@Slf4j
public class MitigationUtils {
    public static void logFailedParametersChanges(ParametersChanges parametersChanges) {
        log.error("FDN: {} failed to return pZeroNomPuschGrant: {} PZeroUePuschOffset256Qam: {} to their initial state",
                parametersChanges.getObjectId().toFdn(),
                parametersChanges.getPZeroNomPuschGrantChangeState().getRequiredValue(),
                parametersChanges.getPZeroUePuschOffset256QamChangeState().getRequiredValue());
    }

    /**
     * maps the results of mitigation to a state
     *
     * @param isImplemented true if the change is implemented successfully
     * @param isRollback true if the change is a rollback
     * @return the mitigation state
     */
    public static MitigationState mapMitigationState(boolean isImplemented, boolean isRollback) {
        if (isImplemented) {
            return isRollback ? ROLLBACK_SUCCESSFUL : CONFIRMED;
        }
        return isRollback ? ROLLBACK_FAILED : CHANGE_FAILED;
    }
}
