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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.ericsson.oss.apps.classification.AllowedCellService;
import com.ericsson.oss.apps.classification.CellRelationService;
import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.MobilityMitigationState;
import com.ericsson.oss.apps.model.mitigation.NeighborDictionary;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import com.ericsson.oss.apps.repositories.NeighborDictionaryRepo;
import com.google.common.collect.Sets;
import io.micrometer.core.instrument.Counter;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MobilityMitigationPolicyTest {

    private static final Long ROP_TIMESTAMP = 1660298400L;
    private static final Double MAX_DELTA_THRESHOLD = 3.0;
    private static final Integer UE_TP_BASE_LINE = 5;

    private static final String ID = "id";
    private static final String ID2 = "id2";
    private static final String CELL_FDN_TPL = "ManagedElement=${id},Cell=${id}";
    private static final String RELATION_FDN_TPL = "ManagedElement=${id},Cell=${id},Relation=${id}_${id2}";

    private static final String CELL0 = StringSubstitutor.replace(CELL_FDN_TPL, Map.of(ID, "0"), "${", "}");
    private static final String CELL2 = StringSubstitutor.replace(CELL_FDN_TPL, Map.of(ID, "2"), "${", "}");
    private static final String CELL4 = StringSubstitutor.replace(CELL_FDN_TPL, Map.of(ID, "4"), "${", "}");
    private static final String CELL6 = StringSubstitutor.replace(CELL_FDN_TPL, Map.of(ID, "6"), "${", "}");
    private static final String CELL8 = StringSubstitutor.replace(CELL_FDN_TPL, Map.of(ID, "8"), "${", "}");

    private Map<NRCellCU, NRCellDU> cellCuDuMap;
    private Map<String, FtRopNRCellDU> ftRopNRCellDUMap;
    private Map<String, NRCellCU> nrCellCUMap;
    private Map<String, NRCellDU> nrCellDUMap;
    private Map<String, List<CellRelationChange>> changesMap;

    @Mock
    private MitigationConfig mitigationConfig;
    @Mock(lenient = true)
    private CellSelectionConfig selectionConfig;
    @Mock(lenient = true)
    private FeatureContext context;
    @Mock(lenient = true)
    private CellRelationService relationService;
    @Mock(lenient = true)
    private CellRelationChangeRepo changeRepo;
    @Mock
    private NeighborDictionaryRepo neighborRepo;
    @Mock
    private AllowedCellService allowedCellService;
    @Mock
    private MobilityMitigationAction mitigationAction;
    @InjectMocks
    private MobilityMitigationPolicy policy;
    @Mock
    private Counter counter;

    @Captor
    private ArgumentCaptor<Collection<CellRelationChange>> changesCaptor;
    @Captor
    private ArgumentCaptor<String> fdnCaptor;

    @BeforeEach
    void setup() {
        when(selectionConfig.getMaxDeltaIPNThresholdDb()).thenReturn(MAX_DELTA_THRESHOLD);
        cellCuDuMap = new HashMap<>();
        when(relationService.getCellDUByCellCU(argThat(cellCuDuMap::containsKey))).thenAnswer(I -> Optional.ofNullable(cellCuDuMap.get((NRCellCU) I.getArgument(0))));
        ftRopNRCellDUMap = new HashMap<>();
        when(context.getRopTimeStamp()).thenReturn(ROP_TIMESTAMP * 1000);
        when(context.getFtRopNRCellDU(argThat(ftRopNRCellDUMap::containsKey))).thenAnswer(I -> ftRopNRCellDUMap.get((String) I.getArgument(0)));
        when(context.getFtRopNRCellDUCellsForMitigation()).thenAnswer(I -> new ArrayList<>(ftRopNRCellDUMap.values()));

        nrCellCUMap = new HashMap<>();
        nrCellDUMap = new HashMap<>();
        changesMap = new HashMap<>();
        when(changeRepo.findByMitigationState(any())).thenAnswer(I -> changesMap.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Test
    void testCheckPreviousMitigationCellsInAllowList() {
        when(allowedCellService.isAllowed(anyString())).thenReturn(true);
        Set<String> expectedRollbackChanges = Set.of(CELL2, CELL4);
        Set<String> expectedStepChanges = Set.of(CELL6);
        Set<String> expectedNoChanges = Set.of(CELL8);
        testCheckPreviousMitigation(expectedRollbackChanges, expectedStepChanges, expectedNoChanges);
    }

    @Test
    void testCheckPreviousMitigationCellsMissingData() {
        when(allowedCellService.isAllowed(anyString())).thenReturn(true);
        Set<String> expectedRollbackChanges = Set.of(CELL2, CELL4);
        Set<String> expectedStepChanges = Set.of(CELL6);
        Set<String> expectedNoChanges = Set.of(CELL8);
        setupPreviousChangeCheck(expectedRollbackChanges, expectedStepChanges, expectedNoChanges);
        this.ftRopNRCellDUMap.get(CELL6).setNRopsInLastSeenWindow((byte) 0);
        policy.checkPreviousMitigations();
        verifyPreviousMitigation(Set.of(CELL2, CELL4, CELL6), Set.of(), Set.of(CELL8));
    }

    @Test
    void testCheckPreviousMitigationCellsNotInAllowList() {
        when(allowedCellService.isAllowed(anyString())).thenReturn(false);
        Set<String> expectedRollbackChanges = Set.of(CELL2, CELL4);
        Set<String> expectedStepChanges = Set.of(CELL6);
        Set<String> expectedNoChanges = Set.of(CELL8);
        setupPreviousChangeCheck(expectedRollbackChanges, expectedStepChanges, expectedNoChanges);
        policy.checkPreviousMitigations();
        verifyPreviousMitigation(Set.of(CELL2, CELL4, CELL6, CELL8), Set.of(), Set.of());
    }

    private void testCheckPreviousMitigation(Set<String> expectedRollbackChanges, Set<String> expectedStepChanges, Set<String> expectedNoChanges) {
        setupPreviousChangeCheck(expectedRollbackChanges, expectedStepChanges, expectedNoChanges);
        policy.checkPreviousMitigations();
        verifyPreviousMitigation(expectedRollbackChanges, expectedStepChanges, expectedNoChanges);
    }

    private void verifyPreviousMitigation(Set<String> expectedRollbackChanges, Set<String> expectedStepChanges, Set<String> expectedNoChanges) {
        MobilityMitigationState state = policy.getMitigationState();
        assertEquals(expectedRollbackChanges, state.getRollbackChanges().keySet());
        assertEquals(expectedStepChanges, state.getStepChanges().keySet());
        assertEquals(expectedNoChanges, state.getNoChanges().keySet());
    }

    @Test
    void testRollbackDeprecatedMitigation() {
        CellRelationChange change1 = buildCellRelationChange(0, 2); // Already RolledBack Change
        CellRelationChange change2 = buildCellRelationChange(2, 0); // Change to Be rolled back

        when(neighborRepo.existsById(any())).thenReturn(true);
        changesMap.get(CELL0).forEach(change -> change.setMitigationState(MitigationState.ROLLBACK_SUCCESSFUL));
        policy.getMitigationState().getRollbackChanges().putAll(changesMap);

        policy.rollbackMitigations();

        verify(mitigationAction, times(2)).rollBackChanges(changesCaptor.capture());
        List<Collection<CellRelationChange>> changes = changesCaptor.getAllValues();
        assertTrue(Stream.of(change1, change2).allMatch(changes.get(0)::contains));
        assertTrue(changes.get(1).isEmpty());
        verify(neighborRepo, times(2)).deleteById(fdnCaptor.capture());
        assertEquals(changesMap.keySet(), new HashSet<>(fdnCaptor.getAllValues()));
    }

    @Test
    void testRegisterNewMitigations() {
        buildCellRelationChange(0, 2); // filtered out in existsById
        CellRelationChange change2_0 = buildCellRelationChange(2, 0); // filtered out in getKcioNeighborNrCellCU
        CellRelationChange change2_4 = buildCellRelationChange(2, 4); // filtered out in mapMutualRelations
        CellRelationChange change2_6 = buildCellRelationChange(2, 6); // new mitigation

        var cellRelationMap = Stream.of(change2_0, change2_4, change2_6)
                .collect(Collectors.toMap(CellRelationChange::getSourceRelation, e -> e.getTargetRelation().getCell()));
        var filteredRelationMap = Stream.of(change2_4, change2_6)
                .collect(Collectors.toMap(CellRelationChange::getSourceRelation, e -> e.getTargetRelation().getCell()));
        var mutualNeighbors = Collections.singletonMap(change2_6.getSourceRelation(), change2_6.getTargetRelation());

        FtRopNRCellDU ftRopNRCellDU = ftRopNRCellDUMap.get(CELL2);
        ftRopNRCellDU.setKcioNeighborNrCellCu(Stream.of(CELL4, CELL6).map(nrCellCUMap::get).collect(Collectors.toList()));
        ftRopNRCellDU.setCellRelationMap(cellRelationMap);

        when(neighborRepo.existsById(CELL0)).thenReturn(true);
        when(relationService.mapMutualRelations(filteredRelationMap)).thenReturn(mutualNeighbors);

        policy.registerNewMitigations();

        ArgumentCaptor<NeighborDictionary> captor = ArgumentCaptor.forClass(NeighborDictionary.class);

        verify(neighborRepo, times(1)).save(captor.capture());
        var actualNewEntry = captor.getValue();
        assertEquals(CELL2, actualNewEntry.getFdn());
        assertEquals(mutualNeighbors, actualNewEntry.getNeighbors());
    }

    @Test
    void testRegisterNewMitigationsNoData() {
        buildCellRelationChange(2, 0); // filtered out in getKcioNeighborNrCellCU
        buildCellRelationChange(2, 4); // filtered out in mapMutualRelations
        ftRopNRCellDUMap.get(CELL2).setNRopsInLastSeenWindow((byte) 0);
        policy.registerNewMitigations();
        verify(neighborRepo, never()).save(any());
    }

    @Nested
    class AuthorizeMitigationsTest {

        @Captor
        ArgumentCaptor<List<CellRelationChange>> stateCaptor;

        @BeforeEach
        void setup() {
            when(neighborRepo.findAll()).thenAnswer(I -> changesMap.entrySet().stream()
                    .map(e -> buildNeighborDictionary(e.getKey(), e.getValue()))
                    .collect(Collectors.toList()));
        }

        @Test
        void testDeleteDeprecatedMitigations() {
            buildCellRelationChange(0, 2);

            policy.authorizeMitigations();

            assertChanges(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            assertTrue(stateCaptor.getValue().isEmpty());
            verify(neighborRepo, times(1)).deleteById(CELL0);
        }

        @Test
        void testRevertNoChanges() {
            CellRelationChange change0_2 = buildCellRelationChange(0, 2);
            policy.getMitigationState().getNoChanges().putAll(changesMap);
            policy.authorizeMitigations();

            assertChanges(Collections.emptyList(), Collections.singletonList(change0_2), Collections.emptyList());
            assertTrue(stateCaptor.getValue().isEmpty());
        }

        @Test
        void testRevertStepChanges() {
            CellRelationChange change0_2 = buildCellRelationChange(0, 2);
            policy.getMitigationState().getStepChanges().putAll(changesMap);
            policy.authorizeMitigations();

            assertChanges(Collections.emptyList(), Collections.singletonList(change0_2), Collections.emptyList());
            assertTrue(stateCaptor.getValue().isEmpty());
        }

        @Test
        void testNewMitigation() {
            buildCellRelationChange(0, 2);
            CellRelationChange change0_4 = buildCellRelationChange(0, 4);
            ftRopNRCellDUMap.get(CELL2).setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD + 1);
            ftRopNRCellDUMap.get(CELL4).setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD);

            policy.authorizeMitigations();

            List<CellRelationChange> expectedStepChanges = Collections.singletonList(change0_4);
            assertChanges(Collections.emptyList(), Collections.emptyList(), expectedStepChanges);
            verify(neighborRepo, never()).deleteById(CELL0);
            assertChangesEquals(expectedStepChanges, stateCaptor.getValue());
        }

        @Test
        void testNoChanges() {
            CellRelationChange change0_2 = buildCellRelationChange(0, 2);
            CellRelationChange change0_4 = buildCellRelationChange(0, 4);
            ftRopNRCellDUMap.get(CELL2).setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD + 1);
            ftRopNRCellDUMap.get(CELL4).setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD);

            policy.getMitigationState().getNoChanges().putAll(changesMap);

            policy.authorizeMitigations();


            List<CellRelationChange> expectedNoChanges = Collections.singletonList(change0_4);
            assertChanges(expectedNoChanges, Collections.singletonList(change0_2), Collections.emptyList());
            verify(neighborRepo, never()).deleteById(CELL0);
            assertTrue(stateCaptor.getValue().isEmpty());
        }

        @Test
        void testStepChanges() {
            CellRelationChange change0_2 = buildCellRelationChange(0, 2);
            CellRelationChange change0_4 = buildCellRelationChange(0, 4);
            ftRopNRCellDUMap.get(CELL2).setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD + 1);
            ftRopNRCellDUMap.get(CELL4).setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD);

            policy.getMitigationState().getStepChanges().putAll(changesMap);

            policy.authorizeMitigations();

            List<CellRelationChange> expectedStepChanges = Collections.singletonList(change0_4);
            assertChanges(Collections.emptyList(), Collections.singletonList(change0_2), expectedStepChanges);
            verify(neighborRepo, never()).deleteById(CELL0);
            assertChangesEquals(expectedStepChanges, stateCaptor.getValue());
        }

        @Test
        void testStepChangesNoData() {
            CellRelationChange change0_2 = buildCellRelationChange(0, 2);
            CellRelationChange change0_4 = buildCellRelationChange(0, 4);
            ftRopNRCellDUMap.get(CELL2).setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD + 1);
            ftRopNRCellDUMap.get(CELL4).setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD);
            ftRopNRCellDUMap.get(CELL4).setNRopsInLastSeenWindow((byte) 0);

            policy.getMitigationState().getStepChanges().putAll(changesMap);

            policy.authorizeMitigations();

            assertChanges(Collections.emptyList(), List.of(change0_2, change0_4), Collections.emptyList());
            assertChangesEquals(Collections.emptyList(), stateCaptor.getValue());
        }

        private void assertChanges(
                List<CellRelationChange> noChanges,
                List<CellRelationChange> rollBackChanges,
                List<CellRelationChange> stepChanges
        ) {
            assertChangesEquals(noChanges,
                    policy.getMitigationState().getNoChanges().getOrDefault(CELL0, Collections.emptyList()));
            assertChangesEquals(rollBackChanges, policy.getMitigationState().getRollbackChanges()
                    .getOrDefault(CELL0, Collections.emptyList()));
            assertChangesEquals(stepChanges, policy.getMitigationState().getStepChanges()
                    .getOrDefault(CELL0, Collections.emptyList()));
            verify(changeRepo, times(1)).saveAll(stateCaptor.capture());
        }
    }

    @Test
    void testApplyMitigations() {
        CellRelationChange change = buildCellRelationChange(0, 2);
        policy.getMitigationState().getStepChanges().putAll(changesMap);
        policy.applyMitigations();
        verify(mitigationAction, times(1)).incrementChanges(Collections.singletonList(change));
    }

    private void setupPreviousChangeCheck(Set<String> expectedRollbackChanges, Set<String> expectedStepChanges, Set<String> expectedNoChanges) {
        Set<String> filterCuDuMapCases = Sets.union(Sets.union(expectedNoChanges, expectedStepChanges), expectedRollbackChanges);

        IntStream.range(0, 1 + filterCuDuMapCases.size()).forEach(i -> buildCellRelationChange(2 * i, 2 * i + 1));

        // Test case when NrCellDU not exists for NrCellCU
        cellCuDuMap = cellCuDuMap.entrySet().stream()
                .filter(e -> filterCuDuMapCases.contains(e.getValue().getObjectId().toFdn()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Test case when FtRopNrCellDu not exists for the given relation
        Set<String> filterFtRopCases = Sets.union(Sets.union(expectedNoChanges, expectedStepChanges), expectedRollbackChanges.stream().limit(1).collect(Collectors.toSet()));
        ftRopNRCellDUMap = ftRopNRCellDUMap.entrySet().stream()
                .filter(e -> filterFtRopCases.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Test case when RI conditions exists
        ftRopNRCellDUMap.entrySet().stream()
                .filter(e -> Sets.union(expectedNoChanges, expectedStepChanges).contains(e.getKey())).forEach(e -> {
                    FtRopNRCellDU ropNRCellDU = e.getValue();
                    ropNRCellDU.setAvgSw8AvgDeltaIpN(MAX_DELTA_THRESHOLD + 1);
                    ropNRCellDU.setAvgSw8UlUeThroughput(1);
                    ropNRCellDU.setUeTpBaseline(UE_TP_BASE_LINE);
                    ropNRCellDU.setAvgSw2UlUeThroughput(UE_TP_BASE_LINE - 2);
                });

        // Test case when we are out of observation window
        changesMap.entrySet().stream().filter(e -> expectedNoChanges.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream()).forEach(change -> change.setLastChangedTimestamp(context.getRopTimeStamp() + 1));
    }

    private CellRelationChange buildCellRelationChange(int victimId, int neighborId) {
        return buildCellRelationChange(String.valueOf(victimId), String.valueOf(neighborId));
    }

    private CellRelationChange buildCellRelationChange(String victimId, String neighborId) {
        String cellFdn = StringSubstitutor.replace(CELL_FDN_TPL, Map.of(ID, victimId), "${", "}");
        String neighborFdn = StringSubstitutor.replace(CELL_FDN_TPL, Map.of(ID, neighborId), "${", "}");
        String relationFdn1 = StringSubstitutor.replace(RELATION_FDN_TPL, Map.of(ID, victimId, ID2, neighborId), "${", "}");
        String relationFdn2 = StringSubstitutor.replace(RELATION_FDN_TPL, Map.of(ID, neighborId, ID2, victimId), "${", "}");

        NRCellDU cellDU = buildNrCellDU(cellFdn);
        NRCellCU cellCU = buildNrCellCU(cellFdn);
        cellCuDuMap.put(cellCU, cellDU);

        NRCellCU neighborCellCU = buildNrCellCU(neighborFdn);
        NRCellDU neighborCellDU = buildNrCellDU(neighborFdn);
        cellCuDuMap.put(neighborCellCU, neighborCellDU);

        NRCellRelation relation1 = new NRCellRelation(relationFdn1);
        relation1.setCell(cellCU);
        NRCellRelation relation2 = new NRCellRelation(relationFdn2);
        relation2.setCell(neighborCellCU);
        var change = new CellRelationChange(relation1, relation2);
        changesMap.computeIfAbsent(cellFdn, fdn -> new ArrayList<>()).add(change);

        return change;
    }

    private NRCellDU buildNrCellDU(String cellFdn) {
        return nrCellDUMap.computeIfAbsent(cellFdn, fdn -> {
            FtRopNRCellDU ropNRCellDU = new FtRopNRCellDU();
            ropNRCellDU.setNRopsInLastSeenWindow((byte) 2);
            ropNRCellDU.setMoRopId(new MoRopId(cellFdn, ROP_TIMESTAMP));
            ftRopNRCellDUMap.put(cellFdn, ropNRCellDU);
            return new NRCellDU(fdn);
        });
    }

    private NRCellCU buildNrCellCU(String cellFdn) {
        return nrCellCUMap.computeIfAbsent(cellFdn, NRCellCU::new);
    }

    private NeighborDictionary buildNeighborDictionary(String cellFdn, List<CellRelationChange> changes) {
        var neighborRecord = new NeighborDictionary();
        neighborRecord.setFdn(cellFdn);
        var neighbors = changes.stream()
                .collect(Collectors.toMap(CellRelationChange::getSourceRelation, CellRelationChange::getTargetRelation));
        neighborRecord.setNeighbors(neighbors);
        return neighborRecord;
    }

    private void assertChangesEquals(List<CellRelationChange> changes1, List<CellRelationChange> changes2) {
        assertTrue(changes1.size() == changes2.size() &&
                IntStream.range(0, changes1.size()).mapToObj(i -> equalChange(changes1.get(i), changes2.get(i)))
                        .allMatch(Boolean.TRUE::equals));
    }

    private boolean equalChange(CellRelationChange change1, CellRelationChange change2) {
        return change1.getSourceRelation().equals(change2.getSourceRelation())
                && change1.getTargetRelation().equals(change2.getTargetRelation())
                && change1.getMitigationState().equals(change2.getMitigationState())
                && change1.getLastChangedTimestamp() == change2.getLastChangedTimestamp();
    }
}
