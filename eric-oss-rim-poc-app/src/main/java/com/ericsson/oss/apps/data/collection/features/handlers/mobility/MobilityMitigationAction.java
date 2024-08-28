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
package com.ericsson.oss.apps.data.collection.features.handlers.mobility;

import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.ConfigChangeImplementor;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import com.ericsson.oss.apps.utils.Utils;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.model.mitigation.MitigationState.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MobilityMitigationAction {

    private static final String INCREMENTED_CHANGES_LOG_SUMMARY =
            "Number Cells where Mobility Mitigation not mitigated  = {}, closedLoopMode = {}";

    private static void logPendingCellRelationChange(CellRelationChange change) {
        log.info("Found sourceRelation -> {} - targetRelation -> {} on pending changes",
                change.getSourceRelation().getObjectId().toFdn(),
                change.getTargetRelation().getObjectId().toFdn());
    }

    private static void logFailedCellRelationChange(CellRelationChange change) {
        log.error("Failed to set IndividualOffsetNR from {} to {} for sourceRelation: {} targetRelation: {}",
                change.getSourceRelation().getCellIndividualOffsetNR(),
                change.getRequiredValue(),
                change.getSourceRelation().getObjectId().toFdn(),
                change.getTargetRelation().getObjectId().toFdn());
    }

    @Value("${mitigation.closeLoopMode}")
    private boolean closedLoopMode;

    private final MitigationConfig mitigationConfig;
    private final ConfigChangeImplementor changeImplementor;
    private final CellRelationChangeRepo changeRepo;

    private final Counter numCellCioMitigationNeighborSucc;
    private final Counter numCellCioMitigationNeighborFailed;
    private final Counter numCellCioRollbackNeighborSucc;
    private final Counter numCellCioRollbackNeighborFailed;
    private final ThreadingConfig threadingConfig;
    private long numCellChangeNotMitigated;

    @Setter
    private Consumer<CellRelationChange> changeCustomizer = change -> change.setLastChangedTimestamp(System.currentTimeMillis());

    public List<CellRelationChange> applyPendingChanges(Collection<CellRelationChange> changes) {
        return Utils.of().processInThreadPool(changes, threadingConfig.getPoolSizeForCIOMitigation(), this::applyPendingChangesParallel, null);
    }

    private List<CellRelationChange> applyPendingChangesParallel(Collection<CellRelationChange> changes, Object ignored) {
        return changes.parallelStream()
                .filter(change -> PENDING.equals(change.getMitigationState()))
                .peek(this::applyPending)
                .filter(change -> FAILED_STATES.contains(change.getMitigationState()))
                .collect(Collectors.toList());
    }

    private void applyPending(CellRelationChange change) {
        logPendingCellRelationChange(change);
        if (change.getOriginalValue().equals(change.getRequiredValue())) {
            applyRollBack(change);
        } else {
            applyIncrement(change);
        }
    }

    public List<CellRelationChange> rollBackChanges(Collection<CellRelationChange> changes) {
        return Utils.of().processInThreadPool(changes, threadingConfig.getPoolSizeForCIOMitigation(), this::rollBackChangesParallel, null);
    }

    private List<CellRelationChange> rollBackChangesParallel(Collection<CellRelationChange> changes, Object ignored) {
        return changes.parallelStream()
                .filter(change -> !MitigationState.ROLLBACK_SUCCESSFUL.equals(change.getMitigationState()))
                .peek(CellRelationChange::setToOriginalValue)
                .peek(this::applyRollBack)
                .collect(Collectors.toList());
    }

    private void applyRollBack(CellRelationChange change) {
        if (PENDING.equals(change.getMitigationState()) &&
                !changeImplementor.implementChange(change)) {
            numCellCioRollbackNeighborFailed.increment();
            change.setMitigationState(ROLLBACK_FAILED);
            changeCustomizer.accept(change);
            changeRepo.save(change);
            logFailedCellRelationChange(change);
        } else {
            numCellCioRollbackNeighborSucc.increment();
            change.setMitigationState(ROLLBACK_SUCCESSFUL);
            changeCustomizer.accept(change);
            changeRepo.delete(change);
            log.debug("Mobility mitigation rollback succeeded: {}", change);
        }
    }

    public void incrementChanges(Collection<CellRelationChange> changes) {
        numCellChangeNotMitigated = 0;
        Utils.of().processInThreadPool(changes, threadingConfig.getPoolSizeForCIOMitigation(), this::incrementChangesParallel, null);
        if (closedLoopMode) {
            log.error(INCREMENTED_CHANGES_LOG_SUMMARY, numCellChangeNotMitigated, closedLoopMode);
        } else {
            log.info(INCREMENTED_CHANGES_LOG_SUMMARY, numCellChangeNotMitigated, closedLoopMode);
        }
    }

    private List<CellRelationChange> incrementChangesParallel(Collection<CellRelationChange> changes, Object ignored) {
        return changes.parallelStream()
                .filter(change -> !FAILED_STATES.contains(change.getMitigationState()))
                .peek(this::incrementChange)
                .peek(this::applyIncrement)
                .collect(Collectors.toList());
    }

    private void incrementChange(CellRelationChange change) {
        var cioParameterConfig = mitigationConfig.getCellIndividualOffset();
        change.setRequiredValue(NumberUtils.min(cioParameterConfig.getMaxAbsoluteValue(),
                change.getSourceRelation().getCellIndividualOffsetNR()+ cioParameterConfig.getStepSize(),
                change.getOriginalValue() + (cioParameterConfig.getStepSize() * cioParameterConfig.getMaxSteps())));
    }

    private void applyIncrement(CellRelationChange change) {
        if (PENDING.equals(change.getMitigationState()) &&
                !changeImplementor.implementChange(change)) {
            numCellCioMitigationNeighborFailed.increment();
            numCellChangeNotMitigated++;
            change.setMitigationState(CHANGE_FAILED);
            logFailedCellRelationChange(change);
        } else {
            numCellCioMitigationNeighborSucc.increment();
            change.setMitigationState(CONFIRMED);
            log.debug("Mobility mitigation increment succeeded: {}", change);
        }
        changeCustomizer.accept(change);
        changeRepo.save(change);
    }
}
