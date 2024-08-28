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
package com.ericsson.oss.apps.data.collection.features.handlers;

import com.ericsson.oss.apps.client.NcmpClient;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigChangeImplementor {

    final NcmpClient ncmpClient;
    @Value("${mitigation.closeLoopMode}")
    private boolean closeLoopMode;


    public boolean implementChange(NRCellDU nrCellDU) {
        NRCellDU patchedNRCellDU = NRCellDU.builder()
                .pZeroNomPuschGrant(nrCellDU.getParametersChanges().getPZeroNomPuschGrantChangeState().getRequiredValue())
                .pZeroUePuschOffset256Qam(nrCellDU.getParametersChanges().getPZeroUePuschOffset256QamChangeState().getRequiredValue())
                .build();
        patchedNRCellDU.setObjectId(nrCellDU.getObjectId());
        return pushChange(patchedNRCellDU);
    }

    public boolean implementChange(CellRelationChange relationChange) {
        return implementChange(relationChange.getSourceRelation(), relationChange.getRequiredValue())
                && implementChange(relationChange.getTargetRelation(), -relationChange.getRequiredValue());
    }

    private boolean implementChange(NRCellRelation relation, Integer requiredValue) {
        if (!relation.getCellIndividualOffsetNR().equals(requiredValue)) {
            NRCellRelation sourcePatchRelation = new NRCellRelation(relation.getObjectId().toString());
            sourcePatchRelation.setCellIndividualOffsetNR(requiredValue);
            boolean state = pushChange(sourcePatchRelation);
            if (state) {
                relation.setCellIndividualOffsetNR(requiredValue);
            }
            return state;
        } else {
            return true;
        }
    }

    @VisibleForTesting
    private boolean pushChange(ManagedObject object) {
        if (!closeLoopMode) return true;
        return ncmpClient.patchCmResource(object);
    }

}
