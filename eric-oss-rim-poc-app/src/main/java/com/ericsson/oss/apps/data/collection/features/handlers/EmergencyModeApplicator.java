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

import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.mobility.MobilityMitigationAction;
import com.ericsson.oss.apps.data.collection.features.report.ParametersChangesReportObject;
import com.ericsson.oss.apps.data.collection.features.report.RelationChangesReportObject;
import com.ericsson.oss.apps.data.collection.features.report.ReportSaver;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import com.ericsson.oss.apps.repositories.ParameterChangesRepo;
import com.ericsson.oss.apps.utils.MitigationUtils;
import com.ericsson.oss.apps.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mitigation.emergencyMode", havingValue = "true")
public class EmergencyModeApplicator {

    private final ParameterChangesRepo parameterChangesRepo;
    private final CellRelationChangeRepo cellRelationChangeRepo;
    private final ConfigChangeImplementor configChangeImplementor;
    private final MobilityMitigationAction mobilityMitigationAction;
    private final ReportSaver reportSaver;
    private final ThreadingConfig threadingConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void applyEmergencyModules() {
        log.info("=== Applying emergency rollback steps ===");
        List<ParametersChanges> failedParametersChangesList = applyEmergencyP0ChangeRollBack();
        List<CellRelationChange> failedCellRelationChangeList = applyEmergencyRelationChangeRollBack();
        long currentRopTime = getCurrentTime();
        emergencyChangeRollBackReportCreator(failedParametersChangesList, currentRopTime);
        emergencyRelationRollBackReportCreator(failedCellRelationChangeList, currentRopTime);
    }

    private List<ParametersChanges> applyEmergencyP0ChangeRollBack() {
        log.info("--- Checking parameter rollback actions ---");
        List<ParametersChanges> parametersChangesList = parameterChangesRepo.findAll().stream()
                .filter(parametersChanges -> !MitigationState.ROLLBACK_SUCCESSFUL.equals(parametersChanges.getMitigationState()))
                .collect(Collectors.toList());
        log.info("Found {} emergency rollback P0 change actions...", parametersChangesList.size());
        return Utils.of().processInThreadPool(parametersChangesList,
                threadingConfig.getPoolSizeForULPowerMitigation(),
                this::applyEmergencyPZeroChangeRollBackParallel, null);
    }

    @NotNull
    private List<ParametersChanges> applyEmergencyPZeroChangeRollBackParallel(Collection<ParametersChanges> parametersChangesList, Object ignored) {
        return parametersChangesList.parallelStream()
                .peek(ParametersChanges::setToOriginalValue)
                .peek(parametersChanges ->
                        parametersChanges.setMitigationState(configChangeImplementor.implementChange(parametersChanges.getNrCellDU()) ? MitigationState.ROLLBACK_SUCCESSFUL : MitigationState.ROLLBACK_FAILED))
                .peek(parameterChangesRepo::save)
                .filter(parametersChanges -> MitigationState.ROLLBACK_FAILED.equals(parametersChanges.getMitigationState()))
                .peek(MitigationUtils::logFailedParametersChanges)
                .collect(Collectors.toList());
    }

    private List<CellRelationChange> applyEmergencyRelationChangeRollBack() {
        log.info("--- Checking relation rollback actions ---");
        List<CellRelationChange> cellRelationChangeList = cellRelationChangeRepo.findAll().stream()
                .filter(cellRelationChange -> !MitigationState.ROLLBACK_SUCCESSFUL.equals(cellRelationChange.getMitigationState()))
                .collect(Collectors.toList());
        log.info("Found {} emergency rollback relation change actions...", cellRelationChangeList.size());
        return mobilityMitigationAction.rollBackChanges(cellRelationChangeList);
    }

    private void emergencyChangeRollBackReportCreator(List<ParametersChanges> failedParametersChangesList, Long currentRopTime) {
        List<ParametersChangesReportObject> parametersChangesReportList = failedParametersChangesList.stream().map(parametersChanges -> {
            ParametersChangesReportObject parametersChangesReportObject = new ParametersChangesReportObject();
            parametersChangesReportObject.setFdn(parametersChanges.getObjectId().toFdn());
            parametersChangesReportObject.setPZeroNomPuschGrant(parametersChanges.getPZeroNomPuschGrantChangeState().getRequiredValue());
            parametersChangesReportObject.setPZeroUePuschOffset256Qam(parametersChanges.getPZeroUePuschOffset256QamChangeState().getRequiredValue());
            return parametersChangesReportObject;
        }).collect(Collectors.toList());
        reportSaver.createReport(parametersChangesReportList, currentRopTime, "ParameterChangeFail");
    }

    private void emergencyRelationRollBackReportCreator(List<CellRelationChange> failedCellRelationChangeList, Long currentRopTime) {
        List<RelationChangesReportObject> relationChangesReportList = failedCellRelationChangeList.stream().map(cellRelationChange -> {
            RelationChangesReportObject relationChangesReportObject = new RelationChangesReportObject();
            relationChangesReportObject.setTargetRelation(cellRelationChange.getTargetRelation().getObjectId().toFdn());
            relationChangesReportObject.setSourceRelation(cellRelationChange.getSourceRelation().getObjectId().toFdn());
            relationChangesReportObject.setInitialValue(cellRelationChange.getRequiredValue());
            return relationChangesReportObject;
        }).collect(Collectors.toList());
        reportSaver.createReport(relationChangesReportList, currentRopTime, "RelationChangeFail");
    }

    protected Long getCurrentTime() {
        return Instant.now().toEpochMilli();
    }
}
