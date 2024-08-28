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

import com.ericsson.oss.apps.classification.AllowedCellService;
import com.ericsson.oss.apps.classification.CellRelationService;
import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.MobilityMitigationState;
import com.ericsson.oss.apps.model.mitigation.NeighborDictionary;
import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import com.ericsson.oss.apps.repositories.NeighborDictionaryRepo;
import io.micrometer.core.instrument.Counter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@RequiredArgsConstructor
class MobilityMitigationPolicy {

    private final FeatureContext featureContext;

    private final CellSelectionConfig cellSelectionConfig;
    private final MitigationConfig mitigationConfig;

    private final CellRelationService cellRelationService;
    private final MobilityMitigationAction mobilityMitigationAction;

    private final CellRelationChangeRepo changeRepo;
    private final NeighborDictionaryRepo neighborRepo;

    private final AllowedCellService allowedCellService;

    private final Counter numCellCioMitigationNeighborBlocked;
    private final Counter numCellCioMitigationCellRegistered;

    @Getter
    private final MobilityMitigationState mitigationState = new MobilityMitigationState();

    public void checkPreviousMitigations() {
        fetchChanges().forEach(this::checkPreviousChanges);
    }

    private Map<String, List<CellRelationChange>> fetchChanges() {
        Map<String, List<CellRelationChange>> duMappedChanges = new HashMap<>();
        Map<NRCellCU, List<CellRelationChange>> cuMappedChanges = Stream.concat(
                changeRepo.findByMitigationState(MitigationState.CONFIRMED).stream(),
                        MitigationState.FAILED_STATES.stream()
                                .flatMap(state -> changeRepo.findByMitigationState(state).stream()))
                .collect(groupingBy(change -> change.getSourceRelation().getCell()));

        cuMappedChanges.forEach((victimCellCU, changes) -> cellRelationService.getCellDUByCellCU(victimCellCU)
                .ifPresentOrElse(victimCellDU -> duMappedChanges.put(victimCellDU.getObjectId().toFdn(), changes),
                        () -> log.warn("Couldn't resolve DU for CU")));
        return duMappedChanges;
    }

    private void checkPreviousChanges(String victimCellDU, List<CellRelationChange> changes) {
        Optional.ofNullable(featureContext.getFtRopNRCellDU(victimCellDU))
                .ifPresentOrElse(ftRopNRCellDU -> {
                    if (ftRopNRCellDU.getNRopsInLastSeenWindow() == 0) {
                        mitigationState.getRollbackChanges().put(victimCellDU, changes);
                        log.debug("Insufficient data for: {}", victimCellDU);
                    } else if (ftRopNRCellDU.getAvgSw8AvgDeltaIpN() <= cellSelectionConfig.getMaxDeltaIPNThresholdDb() ||
                            Double.isNaN(ftRopNRCellDU.getAvgSw8UlUeThroughput()) ||
                            !allowedCellService.isAllowed(ftRopNRCellDU.getMoRopId().getFdn())) {
                        mitigationState.getRollbackChanges().put(victimCellDU, changes);
                        log.debug("No further mobility mitigation required for: {}", victimCellDU);
                    } else if (!Double.isNaN(ftRopNRCellDU.getUeTpBaseline()) &&
                            ftRopNRCellDU.getAvgSw2UlUeThroughput() < ftRopNRCellDU.getUeTpBaseline() &&
                            isNotInObservationWindow(changes)
                    ) {
                        mitigationState.getStepChanges().put(victimCellDU, changes);
                        log.debug("Increased mobility mitigation required for: {}", victimCellDU);
                    } else {
                        mitigationState.getNoChanges().put(victimCellDU, changes);
                        log.debug("No additional mobility mitigation required for: {}", victimCellDU);
                    }
                }, () -> {
                    mitigationState.getRollbackChanges().put(victimCellDU, changes);
                    log.debug("No PM for previously mitigated cell: {}", victimCellDU);
                });
    }


    private boolean isNotInObservationWindow(List<CellRelationChange> changes) {
        return changes.stream().map(CellRelationChange::getLastChangedTimestamp)
                .mapToLong(t -> t).max().stream().boxed()
                .allMatch(t -> t + mitigationConfig.getObservationWindowMs() <= featureContext.getRopTimeStamp());
    }

    public void rollbackMitigations() {
        rollbackChanges();
        rollbackPreviousFails();
        deleteOutDatedNeighborEntries();
    }

    public void rollbackChanges() {
        List<CellRelationChange> changesToRollBack = mitigationState.getRollbackChanges().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        mobilityMitigationAction.rollBackChanges(changesToRollBack);
    }

    private void rollbackPreviousFails() {
        List<CellRelationChange> changesToRollBack = Stream.concat(mitigationState.getStepChanges().entrySet().stream(),
            mitigationState.getNoChanges().entrySet().stream())
                .flatMap(e -> e.getValue().stream())
                .filter(change -> !change.getMitigationState().equals(MitigationState.CONFIRMED))
                .collect(Collectors.toList());
        mobilityMitigationAction.rollBackChanges(changesToRollBack);
    }

    private void deleteOutDatedNeighborEntries() {
        Set<String> outdatedNeighborEntries = mitigationState.getRollbackChanges().keySet();
        outdatedNeighborEntries.parallelStream()
                .filter(neighborRepo::existsById)
                .forEach(neighborRepo::deleteById);
    }

    public void registerNewMitigations() {
        featureContext.getFtRopNRCellDUCellsForMitigation().stream()
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getNRopsInLastSeenWindow() > 0)
                .forEach(this::registerNewMitigation);
    }

    private void registerNewMitigation(FtRopNRCellDU ftRopNRCellDU) {
        String fdn = ftRopNRCellDU.getMoRopId().getFdn();
        if (!neighborRepo.existsById(fdn)) {
            Map<NRCellRelation, NRCellRelation> mutualRelationMap = getMutualRelationMap(ftRopNRCellDU);
            if (mutualRelationMap.size() > 0) {
                NeighborDictionary neighborDictionary = new NeighborDictionary();
                neighborDictionary.setFdn(fdn);
                neighborDictionary.setNeighbors(mutualRelationMap);
                numCellCioMitigationCellRegistered.increment();
                try {
                    neighborRepo.save(neighborDictionary);
                } catch (DataIntegrityViolationException dataIntegrityViolationException) {
                    log.error("Duplicated relation in cell {}, CIO mitigation not registered. ",
                            ftRopNRCellDU.getMoRopId().getFdn(),
                            dataIntegrityViolationException);
                }
            }
        }
    }

    private Map<NRCellRelation, NRCellRelation> getMutualRelationMap(FtRopNRCellDU ftRopNRCellDU) {
        List<NRCellCU> neighborNrCellCu = ftRopNRCellDU.getKcioNeighborNrCellCu();
        Map<NRCellRelation, NRCellCU> neighborRelations = ftRopNRCellDU.getCellRelationMap().entrySet().stream()
                .filter(e -> neighborNrCellCu.contains(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return cellRelationService.mapMutualRelations(neighborRelations);
    }

    public void authorizeMitigations() {
        neighborRepo.findAll().forEach(this::authorizeChanges);
    }

    private void authorizeChanges(NeighborDictionary neighbors) {
        String fdn = neighbors.getFdn();
        Map<NRCellRelation, CellRelationChange> appliedChanges;
        Set<NRCellRelation> allowedRelation = new HashSet<>();
        Set<NRCellRelation> blockedRelation = new HashSet<>();
        List<CellRelationChange> noChanges = new LinkedList<>();

        if (mitigationState.getNoChanges().containsKey(fdn)) {
            appliedChanges = mitigationState.getNoChanges().get(fdn).stream()
                    .collect(Collectors.toMap(CellRelationChange::getSourceRelation, Function.identity()));

            mitigationState.getNoChanges().get(fdn).forEach(change -> checkNeighborCondition(featureContext, change.getTargetRelation().getCell(),
                    e -> noChanges.add(change), () -> blockedRelation.add(change.getSourceRelation())));
        } else if (mitigationState.getStepChanges().containsKey(fdn)) {
            appliedChanges = mitigationState.getStepChanges().get(fdn).stream()
                    .collect(Collectors.toMap(CellRelationChange::getSourceRelation, Function.identity()));

            neighbors.getNeighbors().forEach((key, value) -> checkNeighborCondition(featureContext, value.getCell(),
                    e -> allowedRelation.add(key), () -> blockedRelation.add(key)));
        } else {
            appliedChanges = new HashMap<>();
            neighbors.getNeighbors().forEach((key, value) -> checkNeighborCondition(featureContext, value.getCell(),
                    e -> allowedRelation.add(key), () -> blockedRelation.add(key)));
        }
        numCellCioMitigationNeighborBlocked.increment(blockedRelation.size());

        if (allowedRelation.isEmpty() && noChanges.isEmpty()) {
            neighborRepo.deleteById(fdn);
        }

        mitigationState.getNoChanges().put(fdn, noChanges);

        List<CellRelationChange> blockChanges = blockedRelation.stream()
                .flatMap(relation -> Optional.ofNullable(appliedChanges.get(relation)).stream())
                .collect(Collectors.toList());
        mitigationState.getRollbackChanges().put(fdn, blockChanges);

        List<CellRelationChange> stepChanges = neighbors.getNeighbors().entrySet().stream()
                .filter(e -> allowedRelation.contains(e.getKey()))
                .map(e -> appliedChanges.computeIfAbsent(e.getKey(), k -> new CellRelationChange(k, e.getValue())))
                .collect(Collectors.toList());
        mitigationState.getStepChanges().put(fdn, stepChanges);

        changeRepo.saveAll(stepChanges);
    }

    private void checkNeighborCondition(FeatureContext context, NRCellCU neighborCell, Consumer<FtRopNRCellDU> actionOnGoodCondition, Runnable actionOnBadCondition) {
        cellRelationService.getCellDUByCellCU(neighborCell)
                .map(ManagedObject::getObjectId).map(ManagedObjectId::toFdn)
                .map(context::getFtRopNRCellDU)
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getNRopsInLastSeenWindow() > 0)
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getAvgSw8AvgDeltaIpN() <= cellSelectionConfig.getMaxDeltaIPNThresholdDb())
                .ifPresentOrElse(actionOnGoodCondition, actionOnBadCondition);
    }

    public void applyMitigations() {
        List<CellRelationChange> changes = mitigationState.getStepChanges().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        mobilityMitigationAction.incrementChanges(changes);
        List<CellRelationChange> failedChanges = mitigationState.getStepChanges().values().stream()
                .flatMap(Collection::stream)
                .filter(change -> MitigationState.FAILED_STATES.contains(change.getMitigationState()))
                .collect(Collectors.toList());
        mobilityMitigationAction.rollBackChanges(failedChanges);
    }

}