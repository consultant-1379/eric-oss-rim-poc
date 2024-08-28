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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.model.mom.NRFrequency;
import com.ericsson.oss.apps.model.mom.NRSectorCarrier;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class HandlerTestUtils {

    public static final String FDN_TEMPLATE = "SubNetwork=GINO01,MeContext=%s,ManagedElement=%s,%s=1,%s=%s";
    public static final String FDN1 = String.format(FDN_TEMPLATE, "mino", "mino", "GNBDUFunction", "NRCellDU", "lino");
    public static final String FDN2 = String.format(FDN_TEMPLATE, "pino", "pino", "GNBDUFunction", "NRCellDU", "rino");
    public static final String NRCELL_CU_FDN = String.format(FDN_TEMPLATE, "mino", "mino", "GNBDUFunction", "NRCellCU", "lino");

    public static final long BASE_NCI = 1000000L;
    public static final int altBaseArfcnDL = 424000;
    public static final int baseArfcnDL = 670000;
    public static final int baseBSChannelBwDL = 20;
    public static final int altBaseBSChannelBwDL = 10;
    private static final int basePZeroNomPuschGrant = -100;
    private static final int basePZeroUePuschOffset256Qam = -200;

    private double baseUeUlTp = 12000.0;

    static FtRopNRCellDUPair buildFtRopNRCellDUPair(String fdn1, String fdn2) {
        FtRopNRCellDUPair ftRopNRCellDUPair = new FtRopNRCellDUPair();
        ftRopNRCellDUPair.setDistance(45);
        ftRopNRCellDUPair.setRopTime(123456789L);
        ftRopNRCellDUPair.setFdn1(fdn1);
        ftRopNRCellDUPair.setFdn2(fdn2);
        return ftRopNRCellDUPair;
    }

    static FtRopNRCellDUPair buildFtRopNRCellDUPairAggressorScore(String fdn1,
                                                                  String fdn2,
                                                                  Double aggressorScore) {
        var ftRopNRCellDUPair = buildFtRopNRCellDUPair(fdn1, fdn2);
        ftRopNRCellDUPair.setAggressorScore(aggressorScore);
        return ftRopNRCellDUPair;
    }

    static FtRopNRCellDUPair buildFtRopNRCellDUPair(String fdn1,
                                                    String fdn2,
                                                    double azimuthAffinity,
                                                    double distance,
                                                    double tddOverlap,
                                                    double ductStrength,
                                                    double frequencyOverlap) {
        var ftRopNRCellDUPair = buildFtRopNRCellDUPair(fdn1, fdn2);
        ftRopNRCellDUPair.setDistance(distance);
        ftRopNRCellDUPair.setDuctStrength(ductStrength);
        ftRopNRCellDUPair.setTddOverlap(tddOverlap);
        ftRopNRCellDUPair.setAzimuthAffinity(azimuthAffinity);
        ftRopNRCellDUPair.setFrequencyOverlap(frequencyOverlap);
        return ftRopNRCellDUPair;
    }

    public static FtRopNRCellDU buildFtRopNRCellDUInput(String fdn,
                                                        double avgSw8AvgDeltaIpN,
                                                        double dlRBSymUtil) {
        var ftRopNRCellDU = buildFtRopNRCellDUInput(fdn);
        ftRopNRCellDU.getMoRopId().setRopTime(123456789L);
        ftRopNRCellDU.setAvgSw8AvgDeltaIpN(avgSw8AvgDeltaIpN);
        ftRopNRCellDU.setDlRBSymUtil(dlRBSymUtil);
        return ftRopNRCellDU;
    }

    static List<FtRopNRCellDUPair> getFtRopNRCellDUPairs() {
        List<FtRopNRCellDUPair> ftRopNRCellDUPairs = new ArrayList<>();
        ftRopNRCellDUPairs.add(buildFtRopNRCellDUPairAggressorScore(FDN1 + 1, FDN1 + 2, 1D));
        ftRopNRCellDUPairs.add(buildFtRopNRCellDUPairAggressorScore(FDN1 + 2, FDN1 + 3, 1D));
        ftRopNRCellDUPairs.add(buildFtRopNRCellDUPairAggressorScore(FDN1 + 3, FDN1 + 1, 1D));
        ftRopNRCellDUPairs.add(buildFtRopNRCellDUPairAggressorScore(FDN1 + 3, FDN1 + 4, 0.5D));
        ftRopNRCellDUPairs.add(buildFtRopNRCellDUPairAggressorScore(FDN1 + 5, FDN1 + 4, 0.5D));
        ftRopNRCellDUPairs.add(buildFtRopNRCellDUPairAggressorScore(FDN1 + 4, FDN1 + 5, 0.2D));
        ftRopNRCellDUPairs.add(buildFtRopNRCellDUPairAggressorScore(FDN1 + 5, FDN1 + 6, 0.2D));
        return ftRopNRCellDUPairs;
    }

    public static FtRopNRCellDU buildFtRopNRCellDUInput(String fdn) {
        return new FtRopNRCellDU(new MoRopId(fdn, 0L));
    }

    /**
     * Builds the list of 'fdn and map them to new 'FtRopNRCellDU' for use in test.
     *
     * @param startIndexFdn         the start index fdn
     * @param endIndexFdn           the end index fdn
     * @param pmRadioMaxDeltaIpNAvg the value to set the pmRadioMaxDeltaIpNAvg parameter to.
     * @param avgSw8UlUeThroughput  the value to set the 'avgSw8UlUeThroughput' parameter to
     * @param avgSw8AvgDelta        the value to set the 'avgSw8AvgDelta' parameter to
     * @param connectedComponentId  the connectedComponent Id value.
     * @return the map of generated 'fdn to 'FtRopNRCellDU'
     */
    static Map<String, FtRopNRCellDU> buildListFdnForTest(int startIndexFdn, int endIndexFdn, double pmRadioMaxDeltaIpNAvg,
                                                          double avgSw8UlUeThroughput,
                                                          double avgSw8AvgDelta,
                                                          Long connectedComponentId) {
        Map<String, FtRopNRCellDU> fdnToFtRopNRCellDUMap = new HashMap<>();

        for (int index = startIndexFdn; index <= endIndexFdn; index++) {
            String fdn = String.format(FDN_TEMPLATE, "testo_" + index, "testo_" + index, "GNBDUFunction", "NRCellDU", "vesto_" + index);
            FtRopNRCellDU ftRopNRCellDUForTest = buildFtRopNRCellDUInput(fdn, pmRadioMaxDeltaIpNAvg, avgSw8UlUeThroughput, avgSw8AvgDelta,
                    connectedComponentId);
            fdnToFtRopNRCellDUMap.put(fdn, ftRopNRCellDUForTest);
        }
        return fdnToFtRopNRCellDUMap;
    }

    /**
     * Builds the 'FtRopNRCellDU' for use in 'buildListFdnForTest; .
     *
     * @param fdn                   the fdn to use.
     * @param pmRadioMaxDeltaIpNAvg the value to set the pmRadioMaxDeltaIpNAvg parameter to.
     * @param avgSw8UlUeThroughput  the value to set the 'avgSw8UlUeThroughput' parameter to
     * @param avgSw8AvgDelta        the value to set the 'avgSw8AvgDelta' parameter to.
     * @param connectedComponentId  the connectedComponentId id value.
     * @return the generated 'FtRopNRCellDU'
     */
    public static FtRopNRCellDU buildFtRopNRCellDUInput(String fdn,
                                                        double pmRadioMaxDeltaIpNAvg,
                                                        double avgSw8UlUeThroughput,
                                                        double avgSw8AvgDelta,
                                                        Long connectedComponentId) {
        FtRopNRCellDU ftRopNRCellDU = buildFtRopNRCellDUInput(fdn);
        ftRopNRCellDU.getMoRopId().setRopTime(123456789L);
        ftRopNRCellDU.setPmRadioMaxDeltaIpNAvg(pmRadioMaxDeltaIpNAvg);
        ftRopNRCellDU.setAvgSw8AvgDeltaIpN(avgSw8AvgDelta);
        ftRopNRCellDU.setAvgSw8UlUeThroughput(avgSw8UlUeThroughput);
        ftRopNRCellDU.setConnectedComponentId(connectedComponentId);
        return ftRopNRCellDU;
    }

    /**
     * Builds the 'expected' cell relations map for a list of generated 'fdn'
     * Designed to be used along with 'buildListFdnForTest'
     *
     * @param startIndexFdn the start index fdn.
     * @param endIndexFdn   the end index fdn.
     * @param numRelations  the num relations.
     * @return a map of generated 'fdn' to CellRelationMap applicable for that 'fdn'
     */
    static Map<String, Map<NRCellRelation, NRCellCU>> buildExpectedCellRelationsMap(int startIndexFdn, int endIndexFdn, int numRelations) {
        Map<String, Map<NRCellRelation, NRCellCU>> fdnToCellRelationMap = new HashMap<>();

        for (int index = startIndexFdn; index <= endIndexFdn; index++) {
            Map<NRCellRelation, NRCellCU> cellRelationMap = new HashMap<>();
            String victimFdnCu = String.format(FDN_TEMPLATE, "testo_" + index, "testo_" + index, "GNBCUCPFunction", "NRCellCU", "vesto_" + index);
            String victimFdnDu = String.format(FDN_TEMPLATE, "testo_" + index, "testo_" + index, "GNBDUFunction", "NRCellDU", "vesto_" + index);

            for (int r = 1; r <= numRelations; r++) {
                int rIndex = (index * 1000) + r;
                NRCellRelation victimRelation = new NRCellRelation(victimFdnCu + ",NRCellRelation=" + rIndex);
                victimRelation.isHoAllowed(true);
                String neighborFdnCu = String.format(FDN_TEMPLATE, "testo_" + rIndex, "testo_" + rIndex, "GNBCUCPFunction", "NRCellCU",
                        "vesto_" + rIndex);

                cellRelationMap.put(victimRelation, buildNrCellCu(new NRCellCU(neighborFdnCu)));
            }
            fdnToCellRelationMap.put(victimFdnDu, cellRelationMap);
        }
        return fdnToCellRelationMap;
    }

    static NRCellDU buildNRCellDU(NRCellDU cellDU, Long nci, boolean isNonCompliantBw, boolean isInterFreq) {
        cellDU.setNCI(nci);
        cellDU.setPZeroNomPuschGrant(basePZeroNomPuschGrant);
        cellDU.setPZeroUePuschOffset256Qam(basePZeroUePuschOffset256Qam);
        NRSectorCarrier nRSectorCarrier1 = new NRSectorCarrier(cellDU.getObjectId().toFdn());
        if (isNonCompliantBw) {
            nRSectorCarrier1.setBSChannelBwDL(altBaseBSChannelBwDL);
        } else {
            nRSectorCarrier1.setBSChannelBwDL(baseBSChannelBwDL);
        }
        if (isInterFreq) {
            nRSectorCarrier1.setArfcnDL(altBaseArfcnDL);
        } else {
            nRSectorCarrier1.setArfcnDL(baseArfcnDL);
        }
        NRSectorCarrier nRSectorCarrier2 = new NRSectorCarrier(cellDU.getObjectId().toFdn());

        List<NRSectorCarrier> nRSectorCarrierList = new ArrayList<>();
        nRSectorCarrierList.add(nRSectorCarrier1);
        nRSectorCarrierList.add(nRSectorCarrier2);
        cellDU.setNRSectorCarriers(nRSectorCarrierList);
        return cellDU;
    }

    static NRCellCU buildNrCellCu(NRCellCU cellCU) {
        String fdn = cellCU.getObjectId().toFdn();
        cellCU.setNCI(buildNciFromFdn(fdn));
        cellCU.setNRFrequency(new NRFrequency(fdn));
        return cellCU;
    }

    private long buildNciFromFdn(String fdn) {
        int s1 = fdn.lastIndexOf('_');
        String fdnIndexStr = fdn.substring(s1 + 1);
        int fdnIndex = Integer.parseInt(fdnIndexStr);
        long nci = BASE_NCI + fdnIndex;
        log.trace("Building NrCellCu with fdn = '{}' and nci of '{}'", fdn, nci);
        return nci;
    }

    /**
     * Builds the expected neighbor map for the 'Fdn of victim Cell to fdn of NrCellDU or NrCellCU 'neighbor' cell.
     * Designed to be used along with 'buildCellRelationsMap' to test the contents of List<NRCellCU> neighborNrCellCu
     * and List<NRCellCU> neighborNrCellDu in tests for selecting neighbors for Mitigation (victim) Cells 'FtRopNRCellDU'
     *
     * @param startIndexFdn       the start index fdn.
     * @param endIndexFdn         the end index fdn.
     * @param numRelations        the num relations to build.
     * @param gNBFunctionVictim   the gNBFunction (DU/CU) of victim cell.
     * @param nRCellVictim        the NRCell (DU/CU) of victim cell.
     * @param gNBFunctionNeighbor the gNBfunction (DU/CU) of neighbor cell.
     * @param nRCellNeighbor      the NRCell (DU/CU) of neighbor cell.
     * @param excludeThese        the exclude these.
     * @return the fdn To CellRelationMap
     */
    static Map<String, List<String>> buildExpectedNeighborMap(int startIndexFdn, int endIndexFdn, int numRelations, String gNBFunctionVictim,
                                                              String nRCellVictim, String gNBFunctionNeighbor, String nRCellNeighbor,
                                                              List<Integer> excludeThese) {
        Map<String, List<String>> fdnToCellRelationMap = new HashMap<>();

        for (int index = startIndexFdn; index <= endIndexFdn; index++) {
            List<String> cellRelationList = new ArrayList<>();
            String victimFdn = String.format(FDN_TEMPLATE, "testo_" + index, "testo_" + index, gNBFunctionVictim, nRCellVictim, "vesto_" + index);

            for (int r = 1; r <= numRelations; r++) {
                int rIndex = (index * 1000) + r;
                if (!excludeThese.contains(rIndex)) {
                    String neighborFdn = String.format(FDN_TEMPLATE, "testo_" + rIndex, "testo_" + rIndex, gNBFunctionNeighbor, nRCellNeighbor,
                            "vesto_" + rIndex);
                    cellRelationList.add(neighborFdn);
                }
            }
            fdnToCellRelationMap.put(victimFdn, cellRelationList);
        }
        return fdnToCellRelationMap;
    }

    /**
     * Builds the cell relations neighbor map for the MoID for NrCellCu to NrCellDu (1:1).
     * For use to mock ' when(relationService.getCellDURefByCellCURef(cellCuMoId)).thenReturn(cellDuMoId);'
     *
     * @param startIndexFdn the start index fdn
     * @param endIndexFdn   the end index fdn
     * @param numRelations  the num relations
     * @return the map
     */
    static Map<ManagedObjectId, ManagedObjectId> buildCellRelationsNeighborCuDuMap(int startIndexFdn, int endIndexFdn, int numRelations) {
        Map<ManagedObjectId, ManagedObjectId> cellRelationMap = new HashMap<>();
        for (int index = startIndexFdn; index <= endIndexFdn; index++) {
            for (int r = 1; r <= numRelations; r++) {
                int rIndex = (index * 1000) + r;
                String neighborFdnCu = String.format(FDN_TEMPLATE, "testo_" + rIndex, "testo_" + rIndex, "GNBCUCPFunction", "NRCellCU",
                        "vesto_" + rIndex);
                String neighborFdnDu = String.format(FDN_TEMPLATE, "testo_" + rIndex, "testo_" + rIndex, "GNBDUFunction", "NRCellDU",
                        "vesto_" + rIndex);
                ManagedObjectId moIdNeighborCu = ManagedObjectId.of(neighborFdnCu);
                ManagedObjectId moIdNeighborDu = ManagedObjectId.of(neighborFdnDu);
                cellRelationMap.put(moIdNeighborCu, moIdNeighborDu);
            }
        }
        return cellRelationMap;
    }

    public static void testCellsForMitigation(FeatureContext featureContext, Map<String, FtRopNRCellDU> expectedVictimFtRopNrCellDU) {
        testCellsForMitigation(featureContext, expectedVictimFtRopNrCellDU, x -> true);
    }

    // Assert Methods
    public static void testCellsForMitigation(FeatureContext featureContext, Map<String, FtRopNRCellDU> expectedVictimFtRopNrCellDU, Function<String, Boolean> acceptor) {
        log.info("\n");
        log.info("-------------------- CellsForMitigation FDN -------------------------------");
        List<String> actualMitigationCellsFdn = featureContext.getFtRopNRCellDUCellsForMitigation()
                .stream()
                .map(mitCell -> mitCell.getMoRopId().getFdn())
                .collect(Collectors.toList());
        actualMitigationCellsFdn.forEach(log::info);

        Map<String, String> expectedMitigationCellsMap = expectedVictimFtRopNrCellDU.entrySet()
                .stream()
                .filter(entry -> acceptor.apply(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getMoRopId().getFdn()));

        assertEquals(expectedMitigationCellsMap.keySet(), new HashSet<>(actualMitigationCellsFdn), "Expected List Mitigation Cells to be equal");
        log.info("\n\n\n");
    }

    public static void testCellRelationMap(FeatureContext featureContext,
                                           Map<String, Map<NRCellRelation, NRCellCU>> expectedFdnToCellRelationMap) {
        log.info("\n");
        log.info("-------------------- Neighbor CellRelationsMap FDN -------------------------------");
        Map<String, Map<NRCellRelation, NRCellCU>> actualCellRelationMaps = new HashMap<>();
        featureContext.getFtRopNRCellDUCellsForMitigation().forEach(mitCell -> {
            actualCellRelationMaps.put(mitCell.getMoRopId().getFdn(), mitCell.getCellRelationMap());
        });
        actualCellRelationMaps.forEach((key, value) -> log.info("{}: {}", key, value));
        assertEquals(expectedFdnToCellRelationMap, actualCellRelationMaps, "Expected Cell Relation Maps for Victim Cells to be equal");
        log.info("\n\n\n");
    }

    public static void testNeighborNrCellDuMap(FeatureContext featureContext, Map<String, List<String>> expectedNeighborCellsDuMap) {
        log.info("\n");
        log.info("-------------------- Neighbor NrCellDu List FDN -------------------------------");
        Map<String, List<String>> actualNeighborNrCellDuMap = new HashMap<>();
        featureContext.getFtRopNRCellDUCellsForMitigation().forEach(mitCell -> {
            List<String> neighborNrCellDuList = mitCell.getNeighborNrCell().values()
                    .stream()
                    .map(neighborCell -> neighborCell.getObjectId().toFdn())
                    .sorted()
                    .collect(Collectors.toList());
            actualNeighborNrCellDuMap.put(mitCell.getMoRopId().getFdn(), neighborNrCellDuList);
        });
        actualNeighborNrCellDuMap.forEach((key, value) -> log.info("{}: {}", key, value));

        assertEquals(expectedNeighborCellsDuMap, actualNeighborNrCellDuMap, "Expected List NR Cell DU for Neighbor Cells to be equal");
        log.info("\n\n\n");
    }

    public static void testNeighborNrCellCuMap(FeatureContext featureContext, Map<String, List<String>> expectedNeighborCellsCuMap) {
        log.info("\n");
        log.info("-------------------- Neighbor NrCellCu List FDN -------------------------------");
        Map<String, List<String>> actualNeighborNrCellCuMap = new HashMap<>();
        featureContext.getFtRopNRCellDUCellsForMitigation().forEach(mitCell -> {
            List<String> neighborNrCellCuList = mitCell.getNeighborNrCell().keySet()
                    .stream()
                    .map(neighborCell -> neighborCell.getObjectId().toFdn())
                    .sorted()
                    .collect(Collectors.toList());
            actualNeighborNrCellCuMap.put(mitCell.getMoRopId().getFdn(), neighborNrCellCuList);
        });
        actualNeighborNrCellCuMap.forEach((key, value) -> log.info("{}: {}", key, value));

        assertEquals(expectedNeighborCellsCuMap, actualNeighborNrCellCuMap, "Expected List NR Cell CU for Neighbor Cells to be equal");
        log.info("\n\n\n");
    }

    public static void testNeighborCells(FeatureContext featureContext, Map<String, List<String>> expectedNeighborCellsMap) {
        log.info("\n");
        log.info("-------------------- Neighbor Cells List FDN -------------------------------");
        Map<String, List<String>> actualNeighborCellsMap = new HashMap<>();
        featureContext.getFtRopNRCellDUCellsForMitigation().forEach(mitCell -> {
            List<String> neighborCellsList = mitCell.getNeighborFtRopNRCellDUFdns();
            Collections.sort(neighborCellsList);
            actualNeighborCellsMap.put(mitCell.getMoRopId().getFdn(), neighborCellsList);
        });
        actualNeighborCellsMap.forEach((key, value) -> log.info("{}: {}", key, value));

        assertEquals(expectedNeighborCellsMap, actualNeighborCellsMap, "Expected List FtRopNRCellDU for Neighbor Cells to be equal");
        log.info("\n\n\n");
    }

    public static CellRelationChange getCellRelationChange() {
        return getCellRelationChange(0, 0, 0);
    }

    public static CellRelationChange getCellRelationChange(int originalCio) {
        return getCellRelationChange(originalCio, originalCio, originalCio);
    }

    public static CellRelationChange getCellRelationChange(int originalCio, int requestedCio) {
        return getCellRelationChange(originalCio, originalCio, requestedCio);
    }

    public static CellRelationChange getCellRelationChange(int originalCio, int currentCio, int requestedCio) {
        NRCellRelation nrCellTargetRelation = new NRCellRelation(FDN2);
        nrCellTargetRelation.setCellIndividualOffsetNR(-originalCio);
        nrCellTargetRelation.setCell(new NRCellCU(FDN2));
        NRCellRelation nrCellSourceRelation = new NRCellRelation(FDN1);
        nrCellSourceRelation.setCellIndividualOffsetNR(originalCio);
        CellRelationChange cellRelationChange = new CellRelationChange(nrCellSourceRelation, nrCellTargetRelation);

        nrCellTargetRelation.setCellIndividualOffsetNR(-currentCio);
        nrCellSourceRelation.setCellIndividualOffsetNR(currentCio);

        cellRelationChange.setRequiredValue(requestedCio);
        return cellRelationChange;
    }
}
