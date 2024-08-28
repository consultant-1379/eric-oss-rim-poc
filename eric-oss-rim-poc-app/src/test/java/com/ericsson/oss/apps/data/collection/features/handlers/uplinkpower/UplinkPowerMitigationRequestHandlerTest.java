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
import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.repositories.CmNrCellDuRepo;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UplinkPowerMitigationRequestHandlerTest {

    @Mock
    private ParamChangesHelper paramChangesHelper;

    @Mock
    private CmNrCellDuRepo cmNrCellDuRepo;

    @Mock
    private MitigationProcessor mitigationProcessor;

    @Captor
    ArgumentCaptor<Map<NRCellDU, List<NRCellDU>>> mitigationProcessorArgumentCaptor;

    @Captor
    ArgumentCaptor<List<NRCellDU>> repoSaveArgumentCaptor;

    @InjectMocks
    UplinkPowerMitigationRequestHandler uplinkPowerMitigationRequestHandler;

    @Mock
    private CellMitigationService cellMitigationService;

    @Mock
    MitigationConfig mitigationConfig;

    @Mock
    CellSelectionConfig cellSelectionConfig;

    private FeatureContext context;


    @BeforeEach
    void setUp() {
        when(cellSelectionConfig.getMaxDeltaIPNThresholdDb()).thenReturn(1D);
        context = new FeatureContext(0L);
    }

    @Test
    void noNewCellForMitigation() {
        withNoNewCellsForMitigation().withNoChanges();
        uplinkPowerMitigationRequestHandler.handle(context);
        verify(mitigationProcessor, times(2)).processCellsForMitigation(mitigationProcessorArgumentCaptor.capture());
        assertTrue(mitigationProcessorArgumentCaptor.getAllValues().stream().allMatch(Map::isEmpty));
    }

    @Test
    void newCellsForMitigation() {
        withNewCellsForMitigation().withNoNewCellsForSecondRound().withNoChanges().withDedupMapping(5);
        uplinkPowerMitigationRequestHandler.handle(context);
        verify(mitigationProcessor, times(2)).processCellsForMitigation(mitigationProcessorArgumentCaptor.capture());

        Map<NRCellDU, List<NRCellDU>> cellsForMitigation = mitigationProcessorArgumentCaptor.getAllValues().get(0);
        Map<NRCellDU, List<NRCellDU>> cellsForSecondRound = mitigationProcessorArgumentCaptor.getAllValues().get(1);
        assertEquals(managedObjectToFdn(cellsForMitigation.keySet()), Set.of(FDN1 + 0, FDN1 + 2));
        NRCellDU victim1 = cellsForMitigation.keySet().stream().filter(nrCellDU -> nrCellDU.getObjectId().toFdn().equals(FDN1 + 0)).findFirst().orElse(new NRCellDU());
        NRCellDU victim2 = cellsForMitigation.keySet().stream().filter(nrCellDU -> nrCellDU.getObjectId().toFdn().equals(FDN1 + 2)).findFirst().orElse(new NRCellDU());
        assertEquals(managedObjectToFdn(cellsForMitigation.get(victim1)), Set.of(FDN1 + 1, FDN1 + 2));
        assertEquals(managedObjectToFdn(cellsForMitigation.get(victim2)), Set.of(FDN1 + 1, FDN1 + 3));
        assertTrue(cellsForSecondRound.isEmpty());
    }

    @Test
    void cellsForMitigationKSippedByObservation() {
        withNewCellsForMitigation()
        .withNoNewCellsForSecondRound()
        .withDedupMapping(4)
        .withCellsInObservation(Set.of(0, 1, 2, 3), Set.of(2), 4);
        uplinkPowerMitigationRequestHandler.handle(context);

        verify(mitigationProcessor, times(2)).processCellsForMitigation(mitigationProcessorArgumentCaptor.capture());

        Map<NRCellDU, List<NRCellDU>> cellsForMitigation = mitigationProcessorArgumentCaptor.getAllValues().get(0);
        Map<NRCellDU, List<NRCellDU>> cellsForSecondRound = mitigationProcessorArgumentCaptor.getAllValues().get(1);
        assertTrue(cellsForSecondRound.isEmpty());
        assertTrue(cellsForMitigation.isEmpty());
    }

    @Test
    void cellsForSecondRoundOfMitigation() {
        withNewCellsForMitigation()
        .withCellsForSecondRoundOfMitigation()
        .withDedupMapping(4)
        .withCellsInObservation(Set.of(0, 1, 2, 3), Set.of(2), 5);
        uplinkPowerMitigationRequestHandler.handle(context);
        verify(mitigationProcessor, times(2)).processCellsForMitigation(mitigationProcessorArgumentCaptor.capture());

        Map<NRCellDU, List<NRCellDU>> cellsForMitigation = mitigationProcessorArgumentCaptor.getAllValues().get(0);
        Map<NRCellDU, List<NRCellDU>> cellsForSecondRound = mitigationProcessorArgumentCaptor.getAllValues().get(1);
        assertTrue(cellsForSecondRound.keySet().stream()
                .map(NRCellDU::getObjectId).map(ManagedObjectId::toFdn)
                .collect(Collectors.toUnmodifiableSet()).contains(FDN1 + 0));
        assertTrue(cellsForMitigation.isEmpty());
    }

    @Test
    void resultsSaved() {
        withNoNewCellsForMitigation().withNoNewCellsForSecondRound().withMitigationResults().withNoChanges();
        uplinkPowerMitigationRequestHandler.handle(context);
        verify(cmNrCellDuRepo).saveAll(repoSaveArgumentCaptor.capture());
        assertEquals(5, repoSaveArgumentCaptor.getValue().size());
    }

    private <T extends ManagedObject> Set<String> managedObjectToFdn(Collection<T> moSet) {
        return moSet.stream()
                .map(ManagedObject::getObjectId)
                .map(ManagedObjectId::toFdn)
                .collect(Collectors.toUnmodifiableSet());
    }

    private UplinkPowerMitigationRequestHandlerTest withNewCellsForMitigation() {
        List<FtRopNRCellDU> cellList = getFtRopNRCellDUS();
        context.setFtRopNRCellDUCellsForMitigation(cellList);
        return this;
    }

    private UplinkPowerMitigationRequestHandlerTest withCellsForSecondRoundOfMitigation() {
        Collection<FtRopNRCellDU> cellList = getFtRopNRCellDUS();
        context.getFdnToFtRopNRCellDU().putAll(cellList.stream().collect(Collectors.toMap(cell -> cell.getMoRopId().getFdn(), Function.identity())));
        Collection<FtRopNRCellDU> contextCellList = context.getFdnToFtRopNRCellDU().values();
        Set<String> cellFdnSet = cellList.stream()
                .map(FtRopNRCellDU::getMoRopId)
                .map(MoRopId::getFdn)
                .collect(Collectors.toUnmodifiableSet());
        when(cellMitigationService.getCellsAboveDeltaIpnThresholdAndBelowUETPBaselineFdns(1.0, contextCellList)).thenReturn(cellFdnSet);
        return this;
    }

    @NotNull
    private List<FtRopNRCellDU> getFtRopNRCellDUS() {
        LinkedHashMap<NRCellCU, NRCellDU> neighborMap = IntStream.range(0, 4).mapToObj(index -> Map.entry(new NRCellCU(NRCELL_CU_FDN + index), new NRCellDU(FDN1 + index)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
        List<Map.Entry<NRCellCU, NRCellDU>> neighborEntries = new ArrayList<>(neighborMap.entrySet());
        List<NRCellCU> nrCellCUList = new ArrayList<>(neighborMap.keySet());
        FtRopNRCellDU victim1 = buildFtRopNRCellDUInput(FDN1 + 0);
        victim1.getKp0IntraFneighborNrCellCu().addAll(nrCellCUList.subList(1, 3));
        victim1.getNeighborNrCell().putAll(Map.ofEntries(neighborEntries.get(1), neighborEntries.get(2)));
        FtRopNRCellDU victim2 = buildFtRopNRCellDUInput(FDN1 + 2);
        victim2.getKp0IntraFneighborNrCellCu().addAll(List.of(nrCellCUList.get(1), nrCellCUList.get(3)));
        victim2.getNeighborNrCell().putAll(Map.ofEntries(neighborEntries.get(1), neighborEntries.get(3)));
        return List.of(victim1, victim2);
    }

    private UplinkPowerMitigationRequestHandlerTest withDedupMapping(int i) {
        val cellList = IntStream.range(0, i).mapToObj(index -> new NRCellDU(FDN1 + index)).collect(Collectors.toUnmodifiableList());
        lenient().when(cmNrCellDuRepo.findAllById(any())).thenReturn(cellList);
        return this;
    }

    private UplinkPowerMitigationRequestHandlerTest withMitigationResults() {
        val cellList = IntStream.range(0, 5).mapToObj(index -> new NRCellDU(FDN1 + index)).collect(Collectors.toUnmodifiableList());
        val newCellList = cellList.subList(0, 5 - 2);
        val nextRoundCellList = cellList.subList(2, 5);
        when(mitigationProcessor.processCellsForMitigation(ArgumentMatchers.any())).thenReturn(newCellList, nextRoundCellList);
        return this;
    }

    private UplinkPowerMitigationRequestHandlerTest withNoNewCellsForSecondRound() {
        when(cellMitigationService.getCellsAboveDeltaIpnThresholdAndBelowUETPBaselineFdns(eq(1.0), ArgumentMatchers.any())).thenReturn(Collections.emptySet());
        return this;
    }


    private UplinkPowerMitigationRequestHandlerTest withNoNewCellsForMitigation() {
        context.setFtRopNRCellDUCellsForMitigation(Collections.emptyList());
        return this;
    }

    private UplinkPowerMitigationRequestHandlerTest withNoChanges() {
        when(paramChangesHelper.getFdnSetByObservationState(anyLong())).thenReturn(Pair.of(Collections.emptySet(), Collections.emptySet()));
        return this;
    }

    private void withCellsInObservation(Set<Integer> cellsInMitigationIdx, Set<Integer> cellsInObservationIdx, int nCells) {
        val fdnSetInObservation = IntStream.range(0, nCells).filter(cellsInObservationIdx::contains).mapToObj(index -> FDN1 + index).collect(Collectors.toUnmodifiableSet());
        val fdnSetInMitigation = IntStream.range(0, nCells).filter(cellsInMitigationIdx::contains).mapToObj(index -> FDN1 + index).collect(Collectors.toSet());
        fdnSetInMitigation.addAll(fdnSetInObservation);
        when(paramChangesHelper.getFdnSetByObservationState(anyLong())).thenReturn(Pair.of(fdnSetInMitigation, fdnSetInObservation));
    }


}