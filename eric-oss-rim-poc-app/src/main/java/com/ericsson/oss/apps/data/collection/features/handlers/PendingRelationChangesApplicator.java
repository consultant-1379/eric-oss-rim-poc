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
package com.ericsson.oss.apps.data.collection.features.handlers;

import com.ericsson.oss.apps.data.collection.features.handlers.mobility.MobilityMitigationAction;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${mitigation.closeLoopMode} && !${mitigation.emergencyMode}")
public class PendingRelationChangesApplicator {

    private final CellRelationChangeRepo cellRelationChangeRepo;
    private final MobilityMitigationAction mobilityMitigationAction;

    @EventListener(ApplicationReadyEvent.class)
    public void applyPendingChanges() {
        log.info("=== Checking pending relation changes ===");
        List<CellRelationChange> cellRelationChangeList = cellRelationChangeRepo.findByMitigationState(MitigationState.PENDING);
        log.info("Found {} relation changes", cellRelationChangeList.size());

        List<CellRelationChange> failedRelationChanges = mobilityMitigationAction.applyPendingChanges(cellRelationChangeList);
        failedRelationChanges.stream().findFirst().ifPresent(ignored -> log.error("Failed to apply {} mobility changes", failedRelationChanges.size()));
    }

}
