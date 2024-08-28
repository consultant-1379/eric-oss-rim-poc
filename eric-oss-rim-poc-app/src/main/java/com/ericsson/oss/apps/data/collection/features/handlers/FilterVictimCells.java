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

import com.ericsson.oss.apps.classification.AllowedCellService;
import com.ericsson.oss.apps.classification.CellRelationService;
import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineNRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.repositories.PmBaselineNrCellDuRepo;
import com.ericsson.oss.apps.utils.LockByKey;
import com.ericsson.oss.apps.utils.Utils;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
//@formatter:off

/**
 * The Class FilterVictimCells.
 * a. Get List of Cells where RI > threshold
 * ftRopNRCellDUList <--- getCellsAboveDeltaInterferenceLevel();
 * <p>
 * b. Filter resultant list of cells that have UL UE Throughput below baseline 'ftRopNRCellDUListCellsAboveRiThresholdAndUeUlTpBelowBaseLine'
 * <p>
 * <p>
 * c. Filter resultant list of cells from (b), where
 * - RI > Threshold,
 * - UE UL TP < Baseline
 * - Cells in Duct.
 * <p>
 * This can be done by selecting the cells from ftRopNRCellDUList that are in a cluster.
 * If the victim cells is experiencing RI , it will be part of ftRopNRCellDUList,
 * If the victim cell is in cluster ( ==> In a Duct) then then the connected component ID will be not null.
 * Then check corresponding cluster size > 20 and score > -5 to filter relevant victim cells.
 * <p>
 * OUTPUT : List of Cells where mitigation is required:
 * 'ftRopNRCellDUListCells-AboveRiThreshold-AndUeTpBelowBaseLine-AndInDuct-AndInCluster'
 * <p>
 * THEN
 * For each CELL in the resultant filtered Cell list ( where mitigation is required),
 * Get List Neighbor Cells for that victim
 * if RI (of neighbor cell ) is less than neighbor-Cell-RI-Threshold
 * Add to list of 'eligibleNeighborCells'
 * <p>
 * OUTPUT: List of Cells where mitigation is required, with Neighbor Cells.
 * Neighbor Cells have RI < Neighbor Cell RI Threshold.
 * <p>
 * Each Victim (Mitigation) Cell (FtRopNRCellDU) holds the list of good neighbor cells
 * Feature context holds the List Victim (Mitigation) Cells
 */
//@formatter:on

@Component
@RequiredArgsConstructor
@Slf4j
public class FilterVictimCells implements FeatureHandler<FeatureContext> {

    private final CellRelationService relationService;
    private final PmBaselineNrCellDuRepo pmBaselineNrCellDuRepo;
    private final AllowedCellService allowedCellService;
    private final CellSelectionConfig cellSelectionConfig;
    private final ThreadingConfig threadingConfig;

    /**
     * Main entry point for Class.
     *
     * @param featureContext the feature context
     */
    @Override
    @Timed
    public void handle(FeatureContext featureContext) {
        Map<String, FtRopNRCellDU> ftRopVictimCellsMap = featureContext.getFdnToFtRopNRCellDU().entrySet().stream()
                .filter(entry -> allowedCellService.isAllowed(entry.getValue().getMoRopId().getFdn()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<FtRopNRCellDU> ftRopNRCellDUCellsForMitigationList = filterVictimCells(ftRopVictimCellsMap);
        selectNeighborCells(featureContext, ftRopNRCellDUCellsForMitigationList);
        featureContext.setFtRopNRCellDUCellsForMitigation(ftRopNRCellDUCellsForMitigationList);
    }

    @Override
    public int getPriority() {
        return 162;
    }

    private List<FtRopNRCellDU> filterVictimCells(Map<String, FtRopNRCellDU> ftRopVictimCellsMap) {
        log.info("Filtering {} Victim Cells For Mitigation based on threshold criteria: "
                        + "UE UP TP < BaseLine for cell.",
                ftRopVictimCellsMap.size());
        Map<String, Double> pmBaselineUlUeToVictimCellsMap = getPmBaselineUlUeTpVictimCells(ftRopVictimCellsMap.keySet());
        log.info("Found PmBaseline UL UE TP for {} Victim Cells ", pmBaselineUlUeToVictimCellsMap.size());
        List<FtRopNRCellDU> ftRopNRCellDUCellsForMitigationList = filterCellsForMitigation(ftRopVictimCellsMap, pmBaselineUlUeToVictimCellsMap);
        ftRopNRCellDUCellsForMitigationList.forEach(cells -> log.trace("List cells where RI > Threshold ({}) && UE UP TP < Baseline ({})  = {}",
                cellSelectionConfig.getMinRemoteInterferenceVictimCellDb(), pmBaselineUlUeToVictimCellsMap.get(cells.getMoRopId().getFdn()), cells));
        log.info("Filtered Victim Cells For Mitigation based on threshold criteria from {} --> {} Cells", ftRopVictimCellsMap.size(),
                ftRopNRCellDUCellsForMitigationList.size());

        return ftRopNRCellDUCellsForMitigationList;
    }

    Map<String, Double> getPmBaselineUlUeTpVictimCells(Set<String> victimCellsFdns) {
        return Utils.of().findAllById(new ArrayList<>(victimCellsFdns), 1000, pmBaselineNrCellDuRepo)
                .stream()
                .filter(pmBaselineNRCellDU -> pmBaselineNRCellDU.getFdn() != null)
                .filter(pmBaselineNRCellDU -> pmBaselineNRCellDU.getUplInkThroughputQuartile50() != null)
                .collect(Collectors.toMap(PmBaselineNRCellDU::getFdn, PmBaselineNRCellDU::getUplInkThroughputQuartile50));
    }

    private List<FtRopNRCellDU> filterCellsForMitigation(Map<String, FtRopNRCellDU> cellsMap, Map<String, Double> pmBaselinePerCellMap) {
        return cellsMap
                .values()
                .parallelStream()
                .filter(ftRopNRCellDU -> pmBaselinePerCellMap.get(ftRopNRCellDU.getMoRopId().getFdn()) != null)
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getAvgSw8AvgDeltaIpN() > cellSelectionConfig.getMinRemoteInterferenceVictimCellDb())
                .filter(FtRopNRCellDU::isRemoteInterference)
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getAvgSw8UlUeThroughput() < pmBaselinePerCellMap.get(ftRopNRCellDU.getMoRopId().getFdn()))
                .sorted(Comparator.comparing(a -> a.getMoRopId().getFdn()))
                .toList();
    }

    boolean selectNeighborCells(FeatureContext featureContext, List<FtRopNRCellDU> cellsForMitigationList) {
        log.info("Selecting Neighbor Cells for {} Mitigation (Victim) cells", cellsForMitigationList.size());
        NeighborCellContext neighborCellContext = new NeighborCellContext(featureContext);
        List<Boolean> results = Utils.of().processInThreadPool(cellsForMitigationList, threadingConfig.getPoolSizeForRelationSyncQuery(), this::selectNeighborCellsParallel, neighborCellContext);

        log.info("Processed {} Mitigation (Victim) Cells. Selected total of {} neighbor Cells. "
                        + "Dropped '{}' neighbors (isHoAllowed is false)",
                cellsForMitigationList.size(), neighborCellContext.numberNeighbor.get(), neighborCellContext.numberIsHoAllowedFalse.get());
        return results.stream().allMatch(Boolean::booleanValue);
    }

    @NotNull
    private List<Boolean> selectNeighborCellsParallel(Collection<FtRopNRCellDU> cellsForMitigationList,
                                                      NeighborCellContext neighborCellContext) {
        return cellsForMitigationList.parallelStream().map(ftRopNRCellDU -> {
            FeatureContext featureContext = neighborCellContext.featureContext;
            String fdn = ftRopNRCellDU.getMoRopId().getFdn();
            log.debug("Selecting Neighbor Cells for Mitigation (Victim) Cell with fdn '{}' ", fdn);
            if (!isValidFeatureContextFtRopNRCellDUMap(featureContext, fdn)) {
                return false;
            }
            NRCellDU nrCellDU = featureContext.getFdnToNRCellDUMap().get(fdn);

            Map<NRCellRelation, NRCellCU> cellRelationMap = relationService.getAllowedCellRelationMap(nrCellDU, neighborCellContext.lockByKey);
            ftRopNRCellDU.getCellRelationMap().putAll(cellRelationMap);

            processCellRelations(featureContext, neighborCellContext.numberNeighbor, neighborCellContext.numberIsHoAllowedFalse, ftRopNRCellDU, fdn, cellRelationMap);

            return checkIfOriginalVictimCellIsUnique(ftRopNRCellDU);
        }).collect(Collectors.toList());
    }


    private void processCellRelations(FeatureContext featureContext, AtomicInteger numberNeighbor, AtomicInteger numberIsHoAllowedFalse,
                                      FtRopNRCellDU ftRopNRCellDU, String fdn, Map<NRCellRelation, NRCellCU> cellRelationMap) {

        cellRelationMap.entrySet()
                .stream()
                .peek(entry -> doIsHoAllowedLog(ftRopNRCellDU.getMoRopId().getFdn(), entry.getKey(), entry.getValue(), numberIsHoAllowedFalse))
                .filter(entry -> entry.getKey().isHoAllowed())
                .forEach(entry -> {
                    NRCellCU neighborNrCelllCu = entry.getValue();
                    Optional<NRCellDU> cellDU = relationService.getCellDUByCellCU(neighborNrCelllCu);
                    cellDU.ifPresent(cell -> {
                        FtRopNRCellDU neighborFtRopNRCellDU = featureContext.getFdnToFtRopNRCellDU().get(cell.getObjectId().toFdn());
                        if (!isNeighborFtRopNRCellDuValid(fdn, neighborFtRopNRCellDU)) {
                            return;
                        }
                        ftRopNRCellDU.getNeighborFtRopNRCellDUFdns().add(neighborFtRopNRCellDU.getMoRopId().getFdn());
                        updateNeighborCellList(numberNeighbor, ftRopNRCellDU, neighborNrCelllCu, cell);
                    });
                });
        log.trace("Selected {} Neighbor Cells for Mitigation (Victim) Cell with fdn '{}' ",
                ftRopNRCellDU.getNeighborNrCell().size(), fdn);
    }

    boolean isNeighborFtRopNRCellDuValid(String fdn, FtRopNRCellDU neighborFtRopNRCellDU) {
        if (neighborFtRopNRCellDU == null) {
            log.error("Error Selecting Neighbor Cells for Mitigation (Victim) cell with fdn '{}' "
                    + "Cannot find FtRopNRCellDU information for neigbbor cell from ROP ", fdn);
            return false;
        }
        return true;
    }


    void updateNeighborCellList(AtomicInteger numberNeighbor, FtRopNRCellDU ftRopNRCellDU,
                                NRCellCU cellCu, NRCellDU cellDU) {
        ftRopNRCellDU.getNeighborNrCell().put(cellCu, cellDU);
        numberNeighbor.getAndIncrement();
    }

    boolean isValidFeatureContextFtRopNRCellDUMap(FeatureContext featureContext, String fdn) {
        if (fdn == null || !featureContext.getFdnToNRCellDUMap().containsKey(fdn)) {
            log.error("Error Selecting Neighbor Cells for Mitigation (Victim) cell with fdn '{}'. Cannot find NrCellDU information for this fdn",
                    fdn);
            return false;
        }
        return true;
    }

    /**
     * Check here if more than one match
     * <p>
     * For a given Mitigation cell, there are many neighbors, which are not
     * unique to this Mitigation/Victim Cell, these cells can be neighbors of many cells
     * But each neighbor should have a unique NR cell relation to the original
     * victim/mitigation cell. So there should be only ONE original victim/mitigation cell
     * referenced by all the NR Cell Relations for this given set of neighbors.
     *
     * @param mitigationCell the mitigation cell to check
     */
    private boolean checkIfOriginalVictimCellIsUnique(FtRopNRCellDU mitigationCell) {
        Set<String> neighborNrCellCuFdns = mitigationCell.getNeighborNrCell().keySet()
                .stream()
                .map(nRCellCU -> nRCellCU.getObjectId().toFdn())
                .collect(Collectors.toSet());

        //Filter Intra Cells Relations
        Set<NRCellRelation> victimIntraNrCellRelationsMap = mitigationCell.getCellRelationMap()
                .entrySet()
                .stream()
                .filter(entry -> neighborNrCellCuFdns.contains(entry.getValue().getObjectId().toFdn()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> victimNeCellFdns = victimIntraNrCellRelationsMap.stream()
                .map(cellRelation -> cellRelation.getObjectId().fetchParentId().toFdn())
                .collect(Collectors.toUnmodifiableSet());
        if (victimNeCellFdns.size() > 1) {
            log.error("Found Multiple Victim Cells in Cell Relation Map for given list of Neighbors for Victim Cell '{}'",
                    mitigationCell.getMoRopId().getFdn());
            return false;
        }
        return true;
    }

    private void doIsHoAllowedLog(String victimFdn, NRCellRelation neighborNRCellRelation, NRCellCU neighborNrCelllCu,
                                  AtomicInteger numberIsHoAllowedFalse) {
        if (FALSE.equals(neighborNRCellRelation.isHoAllowed())) {
            log.trace("Error Processing NRCellCU neighbor '{}' with NRCellRelation '{}' of Victim Cell '{}'. HandOver is not allowed '{}'",
                    neighborNrCelllCu.getObjectId().toFdn(), neighborNRCellRelation.getObjectId().toFdn(), victimFdn,
                    neighborNRCellRelation.isHoAllowed());
            numberIsHoAllowedFalse.getAndIncrement();
        }
    }


    @RequiredArgsConstructor
    private static class NeighborCellContext {
        final AtomicInteger numberNeighbor = new AtomicInteger(0);
        final AtomicInteger numberIsHoAllowedFalse = new AtomicInteger(0);

        final FeatureContext featureContext;

        final LockByKey<String> lockByKey = new LockByKey<>();
    }

}
