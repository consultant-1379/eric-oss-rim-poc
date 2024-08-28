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
package com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower;

import com.ericsson.oss.apps.classification.CellMitigationService;
import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.repositories.CmNrCellDuRepo;
import com.ericsson.oss.apps.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mitigation.emergencyMode", havingValue = "false")
public class UplinkPowerMitigationRequestHandler implements FeatureHandler<FeatureContext> {

    private final CmNrCellDuRepo cmNrCellDuRepo;
    private final MitigationProcessor mitigationProcessor;
    private final CellSelectionConfig cellSelectionConfig;


    private final CellMitigationService cellMitigationService;
    private final ParamChangesHelper paramChangesHelper;

    @Override

    public void handle(FeatureContext context) {

        long ropTimeStamp = context.getRopTimeStamp();
        double deltaIpNLowWatermark = cellSelectionConfig.getMaxDeltaIPNThresholdDb();


        Pair<Set<String>, Set<String>> fdnVictimsAndfdnVictimsInObservation = paramChangesHelper.getFdnSetByObservationState(ropTimeStamp);

        // all victim fdns under mitigation
        Set<String> fdnsUnderMitigation = fdnVictimsAndfdnVictimsInObservation.getFirst();
        log.info("fdns under active victim mitigation: {}", fdnsUnderMitigation.size());
        // fdns under mitigation and within observation window
        Set<String> fdnsInObservation = fdnVictimsAndfdnVictimsInObservation.getSecond();
        log.info("fdns under observation for victim mitigation: {}", fdnsInObservation.size());

        Map<String, List<String>> newFdnsRequiringMitigationWithListNeighbors = getNewCellsRequiringMitigation(context.getFtRopNRCellDUCellsForMitigation(), fdnsUnderMitigation);

        log.info("new cells requiring mitigation: {}", newFdnsRequiringMitigationWithListNeighbors.size());

        Set<String> fdnsRequiringMoreMitigation = getCellsRequiringMoreMitigation(cellMitigationService.getCellsAboveDeltaIpnThresholdAndBelowUETPBaselineFdns(deltaIpNLowWatermark, context.getFdnToFtRopNRCellDU().values()),
                fdnsInObservation,
                fdnsUnderMitigation);
        log.info("fdn requiring further victim mitigation: {}", fdnsRequiringMoreMitigation.size());

        Map<ManagedObjectId, Set<ManagedObjectId>> neighborsUnderMitigation = paramChangesHelper.getUplinkpowerNeighborsUnderMitigation();

        // build a dedup map for NRCellDUs
        Set<ManagedObjectId> moidSet = dedupObjectIds(newFdnsRequiringMitigationWithListNeighbors, fdnsRequiringMoreMitigation, neighborsUnderMitigation);
        Map<String, NRCellDU> dedupMap = Utils.of().findAllById(new ArrayList<>(moidSet), 1000, cmNrCellDuRepo).stream().collect(Collectors.toMap(nrCellDU -> nrCellDU.getObjectId().toFdn(), Function.identity()));
        log.info("deduplication map size {} cells", dedupMap.size());

        Map<NRCellDU, List<NRCellDU>> newCellsRequiringMitigation = mapToNRCellDU(newFdnsRequiringMitigationWithListNeighbors, dedupMap);

        log.info("{} cells for new mitigation", newCellsRequiringMitigation.size());

        Map<NRCellDU, List<NRCellDU>> cellsRequiringMoreMitigation = mapToNRCellDU(fdnsRequiringMoreMitigation, neighborsUnderMitigation, dedupMap);

        log.info("{} cells for next mitigation round", cellsRequiringMoreMitigation.size());

        // Set change requests for the cells in troubles
        List<NRCellDU> newCellsMitigated = mitigationProcessor.processCellsForMitigation(newCellsRequiringMitigation);
        List<NRCellDU> cellsSecondMitigation = mitigationProcessor.processCellsForMitigation(cellsRequiringMoreMitigation);

        // Remove duplicates (no point sending twice to db)
        // object level equals is ok - all cells have been remapped to the same object
        List<NRCellDU> cellsToSave = Stream.concat(newCellsMitigated.stream(), cellsSecondMitigation.stream())
                .distinct()
                .collect(Collectors.toUnmodifiableList());

        log.info("saving {} cells for mitigation (including neighbors))", cellsToSave.size());

        cmNrCellDuRepo.saveAll(cellsToSave);
    }

    @NotNull
    private Map<NRCellDU, List<NRCellDU>> mapToNRCellDU(Set<String> fdnsRequiringMoreMitigation,
                                                        Map<ManagedObjectId, Set<ManagedObjectId>> neighborsUnderMitigation,
                                                        Map<String, NRCellDU> dedupMap) {
        return fdnsRequiringMoreMitigation.stream()
                .filter(dedupMap::containsKey)
                .map(fdn -> Pair.of(dedupMap.get(fdn),
                        neighborsUnderMitigation.getOrDefault(ManagedObjectId.of(fdn), Collections.emptySet()).stream()
                        .map(neighborMoid -> dedupMap.get(neighborMoid.toFdn()))
                        .collect(Collectors.toUnmodifiableList())))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @NotNull
    private Map<NRCellDU, List<NRCellDU>> mapToNRCellDU(Map<String, List<String>> newFdnsRequiringMitigation,
                                                        Map<String, NRCellDU> dedupMap) {
        return newFdnsRequiringMitigation.entrySet().stream()
                .filter(fdnAndNeighbors -> dedupMap.containsKey(fdnAndNeighbors.getKey()))
                .map(fdnAndNeighbors -> Pair.of(dedupMap.get(fdnAndNeighbors.getKey()),
                        fdnAndNeighbors.getValue().stream().map(dedupMap::get).collect(Collectors.toUnmodifiableList())))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @NotNull
    private Set<ManagedObjectId> dedupObjectIds(Map<String, List<String>> newFdnsRequiringMitigation,
                                                Set<String> fdnsRequiringMoreMitigation,
                                                Map<ManagedObjectId, Set<ManagedObjectId>> neighborsUnderMitigation) {
        //merge all the fdns and moids considered
        Set<String> fdnSet = newFdnsRequiringMitigation.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        fdnSet.addAll(newFdnsRequiringMitigation.keySet());
        fdnSet.addAll(fdnsRequiringMoreMitigation);
        Set<ManagedObjectId> moidSet = neighborsUnderMitigation.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        moidSet.addAll(neighborsUnderMitigation.keySet());
        moidSet.addAll(fdnSet.stream().map(ManagedObjectId::of).collect(Collectors.toUnmodifiableList()));
        return moidSet;
    }


    /**
     * @param cellsToEvaluate         input list
     * @param cellsUnderMitigationFdn fdns already under mitigation - used for filtering
     * @return cells and ranked neighbors that fit the criteria to enter mitigation.
     */
    @NotNull
    private Map<String, List<String>> getNewCellsRequiringMitigation(List<FtRopNRCellDU> cellsToEvaluate,
                                                                     Set<String> cellsUnderMitigationFdn) {
        return cellsToEvaluate.stream()
                // not under observation
                .filter(ftRopNRCellDU -> !cellsUnderMitigationFdn.contains(ftRopNRCellDU.getMoRopId().getFdn()))
                .collect(Collectors.toMap(ftRopNrCellDu -> ftRopNrCellDu.getMoRopId().getFdn(), FtRopNRCellDU::getKp0IntraFreqNeighborNrCellDuFdns));
    }

    /**
     * @param cellsAboveMAxDeltaIPNLowFdn input list
     * @param cellsInObservationFdn       fdns in observation window (we are waiting to see what happens) - used for filtering
     * @param cellsUnderMitigationFdn     already under mitigation - used for filtering
     * @return victim fdns in for a second round
     */
    @NotNull
    private Set<String> getCellsRequiringMoreMitigation(Set<String> cellsAboveMAxDeltaIPNLowFdn,
                                                        Set<String> cellsInObservationFdn,
                                                        Set<String> cellsUnderMitigationFdn) {
        return cellsAboveMAxDeltaIPNLowFdn.stream()
                // in mitigation
                .filter(cellsUnderMitigationFdn::contains)
                // not in observation window
                .filter(fdn -> !cellsInObservationFdn.contains(fdn))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int getPriority() {
        return 180;
    }

}
