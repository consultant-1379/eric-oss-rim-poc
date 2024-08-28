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
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineNRCellDU;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.repositories.PmBaselineNrCellDuRepo;
import com.ericsson.oss.apps.utils.LockByKey;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class FilterVictimCellsAndSelectGoodNeighborsTest {

    @Mock
    private PmBaselineNrCellDuRepo pmBaselineNrCellDuRepo;

    @Mock
    private AllowedCellService allowedCellService;
    @Mock
    private CellSelectionConfig thresholdConfig;
    @Mock
    private ThreadingConfig threadingConfig;

    @Mock(lenient = true)
    private CellRelationService relationService;

    @Mock(lenient = true)
    private Counter counter;

    @InjectMocks
    private FilterVictimCells filterVictimCells;

    private double baseUeUlTp = 12000.0;
    Map<String, FtRopNRCellDU> expectedVictimFtRopNrCellDU = null;
    Map<String, Map<NRCellRelation, NRCellCU>> expectedFdnToCellRelationMap = null;
    FeatureContext fc = null;

    @BeforeEach
    void setup() {
        lenient().when(threadingConfig.getPoolSizeForRelationSyncQuery()).thenReturn(2);
    }

    @Nested
    @Order(8)
    class FilterVictimCellsTest {

        @BeforeEach
        void setup() {
            fc = setupForTest();
        }

        @Test
        @Order(1)
        void filterVictimCells_test() {
            filterVictimCells.handle(fc);
            printInfo(fc, "(AFTER FILTER VICTIM CELLS)");
            // This exclude list used to build up the 'expected' maps for each of the tests. These cell will be filtered out.
            List<Integer> excludeList = new ArrayList<>();
            excludeList.add(5001);  //exclude; isHoAllowed = false
            testCellsForMitigation(fc, expectedVictimFtRopNrCellDU, (fdn -> !fdn.equals(getIndexedFdn(6))));
            testNeighborCells(fc, excludeList);
            testNeighborNrCellCuMap(fc, excludeList);
            testNeighborNrCellDuMap(fc, excludeList);
            testCellRelationMap(fc, expectedFdnToCellRelationMap);
        }

    }


    @Test
    @Order(4)
    void isValidFeatureContextFtRopNRCellDUMap_test() {
        boolean result = filterVictimCells.isValidFeatureContextFtRopNRCellDUMap(new FeatureContext(0L), null);
        assertFalse(result, "Expected isValidFeatureContextFtRopNRCellDUMap to return false for null fdn");

        FeatureContext fcTest = new FeatureContext(0L);
        result = filterVictimCells.isValidFeatureContextFtRopNRCellDUMap(fcTest, "testFdn");
        assertFalse(result, "Expected isValidFeatureContextFtRopNRCellDUMap to return false for 'fdn'not in FtRopNRCellDUMap  ");

    }

    @Test
    @Order(6)
    void isNeighborFtRopNRCellDuValid_test() {
        boolean result = filterVictimCells.isNeighborFtRopNRCellDuValid("testFdn", null);
        assertFalse(result, "Expected isNeighborFtRopNRCellDuValid to return false for null neighbor FtRop NRCell DU");
    }

    @Test
    @Order(7)
    void updateNeighborCellList_test() {
        AtomicInteger numberNeighbor = new AtomicInteger(0);
        FtRopNRCellDU ftRopNRCellDU = new FtRopNRCellDU();
        NRCellCU cellCu = new NRCellCU("testFdn,Cu");
        NRCellDU cellDU = new NRCellDU("testFdn,Du");

        filterVictimCells.updateNeighborCellList(numberNeighbor, ftRopNRCellDU, cellCu, cellDU);

        assertEquals(1, numberNeighbor.get());
    }

    @Nested
    @Order(8)
    class SelectNeighborCellsTest {

        private final NRCellDU nRcellDU = buildNRCellDU(new NRCellDU("testFdn,fdn"), BASE_NCI, false, false);
        private final NRCellDU nRCellDuNeighbor = buildNRCellDU(new NRCellDU("testFdn,Du2,testo_99"), BASE_NCI, false, false);
        private final NRCellCU nRCellCuNeighbor = buildNrCellCu(new NRCellCU("testFdn,Cu2,testo_99"));

        private final NRCellRelation relation = new NRCellRelation("testFdn,Cu,testo_99");

        private FeatureContext fcTest;
        private FtRopNRCellDU ftRopNRCellDU;
        private List<FtRopNRCellDU> cellsForMitigationList;
        private FtRopNRCellDU ftRopNRCellDuNeighbor;

        @BeforeEach
        void beforeEach() {
            ftRopNRCellDU = new FtRopNRCellDU();
            cellsForMitigationList = List.of(ftRopNRCellDU);

            ftRopNRCellDuNeighbor = new FtRopNRCellDU();

            fcTest = new FeatureContext(0L);
            fcTest.setFtRopNRCellDUCellsForMitigation(cellsForMitigationList);
            ftRopNRCellDuNeighbor.setAvgSw8AvgDeltaIpN(0.8);
            ftRopNRCellDuNeighbor.setMoRopId(new MoRopId("testFdn,Du2,testo_99", 0L));
            fcTest.getFdnToFtRopNRCellDU().put(ftRopNRCellDuNeighbor.getMoRopId().getFdn(), ftRopNRCellDuNeighbor);
            relation.isHoAllowed(true);
        }

        @Test
        @Order(1)
        void selectNeighborCells_test() {
            ftRopNRCellDU.setMoRopId(new MoRopId("testFdn,fdn", 0L));
            fcTest.getFdnToNRCellDUMap().put("testFdn,fdn", nRcellDU);

            Map<NRCellRelation, NRCellCU> cellRelationMap = Map.of(relation, nRCellCuNeighbor);

            lenient().when(relationService.getAllowedCellRelationMap(eq(nRcellDU), any(LockByKey.class))).thenReturn(cellRelationMap);
            when(relationService.getCellDUByCellCU(nRCellCuNeighbor)).thenReturn(Optional.of(nRCellDuNeighbor));

            filterVictimCells.selectNeighborCells(fcTest, cellsForMitigationList);
            testAssertSelectNeighborCellsHappyCase(fcTest, ftRopNRCellDU, nRCellDuNeighbor, nRCellCuNeighbor, ftRopNRCellDuNeighbor, cellRelationMap);
        }

        @Test
        @Order(2)
        void selectNeighborCells_noValidFeatureContextFtRopNRCellDUMap_test() {
            ftRopNRCellDU.setMoRopId(new MoRopId(null, 0L));

            filterVictimCells.selectNeighborCells(fcTest, cellsForMitigationList);

            testAssertEmptyResults(fcTest);
        }

        @Test
        @Order(3)
        void selectNeighborCells_isCellRelationMapNotValid_test() {
            ftRopNRCellDU.setMoRopId(new MoRopId("testFdn,fdn", 0L));
            fcTest.getFdnToNRCellDUMap().put("testFdn,fdn", nRcellDU);

            lenient().when(relationService.getAllowedCellRelationMap(eq(nRcellDU), any(LockByKey.class))).thenReturn(Collections.emptyMap());

            filterVictimCells.selectNeighborCells(fcTest, cellsForMitigationList);

            testAssertEmptyResults(fcTest);
        }

        @Test
        @Order(4)
        void selectNeighborCells_isNeighborFtRopNRCellDuNotValid_test() {
            ftRopNRCellDU.setMoRopId(new MoRopId("testFdn,fdn", 0L));
            fcTest.getFdnToNRCellDUMap().put("testFdn,fdn", nRcellDU);

            NRCellDU cellDuNeighbor = new NRCellDU("testFdn,Du2,notExist,testo_99");
            Map<NRCellRelation, NRCellCU> cellRelationMap = Map.of(relation, nRCellCuNeighbor);

            lenient().when(relationService.getAllowedCellRelationMap(eq(nRcellDU), any(LockByKey.class))).thenReturn(cellRelationMap);
            when(relationService.getCellDUByCellCU(nRCellCuNeighbor)).thenReturn(Optional.of(cellDuNeighbor));

            filterVictimCells.selectNeighborCells(fcTest, cellsForMitigationList);

            FtRopNRCellDU ftRopNRCellDUResult = fcTest.getFtRopNRCellDUCellsForMitigation().get(0);

            assertEquals(cellRelationMap, ftRopNRCellDUResult.getCellRelationMap(), "Expected Cell Relation Maps for Victim Cells to be empty");

            assertEquals(Collections.emptyMap(), ftRopNRCellDUResult.getNeighborNrCell(), "Expected Map for Neighbor Cells to be empty");

            assertEquals(Collections.emptyList(), ftRopNRCellDUResult.getNeighborFtRopNRCellDUFdns(),
                    "Expected List FtRopNRCellDU for Neighbor Cells to be empty");
        }

        @Test
        @Order(5)
        void selectNeighborCellsRimHandlerException_test() {
            ftRopNRCellDU.setMoRopId(new MoRopId("testFdn,fdn", 0L));
            fcTest.getFdnToNRCellDUMap().put("testFdn,fdn", nRcellDU);
            fcTest.getFdnToNRCellDUMap().put("testFdn2,fdn", nRcellDU);

            NRCellRelation cellCuMoIdNeCellRelation1 = new NRCellRelation(
                    "testFdn,Cu,ManagedElement=testo_99,GNBDUFunction=1,NRCellDU=vesto_2,NRCellRelation=1");
            cellCuMoIdNeCellRelation1.isHoAllowed(true);
            NRCellRelation cellCuMoIdNeCellRelation2 = new NRCellRelation(
                    "testFdn2,Cu,ManagedElement=testo_99,GNBDUFunction=1,NRCellDU=vesto_2,NRCellRelation=2"); //Different parent as cellCuMoIdNeCellRelation1
            cellCuMoIdNeCellRelation2.isHoAllowed(true);

            FtRopNRCellDU ftRopNRCellDuNeighbor2 = new FtRopNRCellDU();
            ftRopNRCellDuNeighbor2.setAvgSw8AvgDeltaIpN(0.8);
            ftRopNRCellDuNeighbor2.setMoRopId(new MoRopId("testFdn2,Du2,testo_99", 0L));
            fcTest.getFdnToFtRopNRCellDU().put(ftRopNRCellDuNeighbor2.getMoRopId().getFdn(), ftRopNRCellDuNeighbor2);

            NRCellCU nRCellCuNeighbor2 = buildNrCellCu(new NRCellCU("testFdn2,Cu2,testo_99"));
            NRCellDU nRCellDuNeighbor2 = buildNRCellDU(new NRCellDU("testFdn2,Du2,testo_99"), BASE_NCI, false, false);

            Map<NRCellRelation, NRCellCU> cellRelationMap = Map.of(cellCuMoIdNeCellRelation1, nRCellCuNeighbor,
                    cellCuMoIdNeCellRelation2, nRCellCuNeighbor2);

            lenient().when(relationService.getAllowedCellRelationMap(eq(nRcellDU), any(LockByKey.class))).thenReturn(cellRelationMap);

            Map<NRCellCU, NRCellDU> cells = new HashMap<>();
            cells.put(nRCellCuNeighbor, nRCellDuNeighbor);
            cells.put(nRCellCuNeighbor2, nRCellDuNeighbor2);
            ftRopNRCellDU.setNeighborNrCell(cells);
            when(relationService.getCellDUByCellCU(nRCellCuNeighbor)).thenReturn(Optional.of(nRCellDuNeighbor));
            when(relationService.getCellDUByCellCU(nRCellCuNeighbor2)).thenReturn(Optional.of(nRCellDuNeighbor2));

            assertFalse(filterVictimCells.selectNeighborCells(fcTest, cellsForMitigationList));
        }
    }


    @Test
    @Order(18)
    void selectNeighborCells_getPmBaselineUlUeTpVictimCells_test() {
        Set<String> nonExistantFdns = new HashSet<>();
        nonExistantFdns.add("fdn1");
        nonExistantFdns.add("fdn2");
        nonExistantFdns.add(null);

        Map<String, Double> resultMap = filterVictimCells.getPmBaselineUlUeTpVictimCells(nonExistantFdns);
        assertEquals(Collections.emptyMap(), resultMap);
    }

    private void testNeighborNrCellDuMap(FeatureContext featureContext, List<Integer> excludeList) {
        Map<String, List<String>> expectedNeighborCellsDuMap = buildExpectedNeighborMap(1, 5, 2, "GNBDUFunction", "NRCellDU", "GNBDUFunction",
                "NRCellDU", excludeList);
        HandlerTestUtils.testNeighborNrCellDuMap(featureContext, expectedNeighborCellsDuMap);
    }

    private void testNeighborNrCellCuMap(FeatureContext featureContext, List<Integer> excludeList) {
        Map<String, List<String>> expectedNeighborCellsCuMap = buildExpectedNeighborMap(1, 5, 2, "GNBDUFunction", "NRCellDU", "GNBCUCPFunction",
                "NRCellCU", excludeList);
        HandlerTestUtils.testNeighborNrCellCuMap(featureContext, expectedNeighborCellsCuMap);
    }

    private void testNeighborCells(FeatureContext featureContext, List<Integer> excludeList) {

        Map<String, List<String>> expectedNeighborCellsMap = buildExpectedNeighborMap(1, 5, 2, "GNBDUFunction", "NRCellDU", "GNBDUFunction",
                "NRCellDU", excludeList);
        HandlerTestUtils.testNeighborCells(featureContext, expectedNeighborCellsMap);
    }


    private void testAssertSelectNeighborCellsHappyCase(FeatureContext fcTest, FtRopNRCellDU ftRopNRCellDU, NRCellDU nRCellDuNeighbor,
                                                        NRCellCU nRCellCuNeighbor, FtRopNRCellDU ftRopNRCellDuNeighbor,
                                                        Map<NRCellRelation, NRCellCU> cellRelationMap) {
        FtRopNRCellDU ftRopNRCellDUResult = fcTest.getFtRopNRCellDUCellsForMitigation().get(0);

        Map<String, FtRopNRCellDU> expectedCellsForMitigationMap = new HashMap<>();
        expectedCellsForMitigationMap.put(ftRopNRCellDU.getMoRopId().getFdn(), ftRopNRCellDUResult);
        testCellsForMitigation(fcTest, expectedCellsForMitigationMap);

        assertEquals(cellRelationMap, ftRopNRCellDUResult.getCellRelationMap(), "Expected Cell Relation Maps for Victim Cells to be equal");

        assertEquals(Set.of(nRCellDuNeighbor), new HashSet<>(ftRopNRCellDUResult.getNeighborNrCell().values()), "Expected List NR Cell DU for Neighbor Cells to be equal");
        assertEquals(Set.of(nRCellCuNeighbor), ftRopNRCellDUResult.getNeighborNrCell().keySet(), "Expected List NR Cell CU for Neighbor Cells to be equal");

        List<String> expectedFtRopNRCellDU = new ArrayList<>();
        expectedFtRopNRCellDU.add(ftRopNRCellDuNeighbor.getMoRopId().getFdn());
        assertEquals(expectedFtRopNRCellDU, ftRopNRCellDUResult.getNeighborFtRopNRCellDUFdns(),
                "Expected List FtRopNRCellDU for Neighbor Cells to be equal");
    }

    private void testAssertEmptyResults(FeatureContext fcTest) {
        FtRopNRCellDU ftRopNRCellDUResult = fcTest.getFtRopNRCellDUCellsForMitigation().get(0);

        assertTrue(ftRopNRCellDUResult.getCellRelationMap().isEmpty(), "Expected Cell Relation Maps for Victim Cells to be empty");

        assertTrue(ftRopNRCellDUResult.getNeighborNrCell().isEmpty(), "Expected List NR Cell DU for Neighbor Cells to be empty");


        assertTrue(ftRopNRCellDUResult.getNeighborFtRopNRCellDUFdns().isEmpty(), "Expected List FtRopNRCellDU for Neighbor Cells to be empty");
    }

    // Setup methods from here
    private FeatureContext setupForTest() {
        FeatureContext featureContext = new FeatureContext(0L);
        //Following will test filterVictimCells().
        when(thresholdConfig.getMinRemoteInterferenceVictimCellDb()).thenReturn(2.0);

        Map<String, FtRopNRCellDU> fdnToFtRopNRCellDUMap = featureContext.getFdnToFtRopNRCellDU();
        expectedVictimFtRopNrCellDU = buildListFdnForTest(1, 5, 1.0, 10000, 10.0, 1L);
        var radioSymbolDeltalIpnCellsMap = buildListFdnForTest(6, 6, 1.0, 10000, 10.0, null);
        radioSymbolDeltalIpnCellsMap.values().forEach(ftRopNRCellDU -> ftRopNRCellDU.setInterferenceType(InterferenceType.REMOTE));
        expectedVictimFtRopNrCellDU.putAll(radioSymbolDeltalIpnCellsMap);
        fdnToFtRopNRCellDUMap.putAll(expectedVictimFtRopNrCellDU);
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(7, 10, 1.0, 15000, 10.0, 2L)); // Will be filtered out due to avgSw8UlUeThroughput < BaseLine
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(11, 12, 1.0, 10000, 1.0, 3L));   // Will be filtered out due to  avgSw8AvgDelta < threshold
        addNullConnectedComponentsCells(fdnToFtRopNRCellDUMap);

        IntStream.rangeClosed(1, 18).forEach(index -> when(allowedCellService.isAllowed(getIndexedFdn(index))).thenReturn(index != 6));

        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(1001, 1002, 1.0, 10000, 0.5, 1L)); // good Neighbors
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(2001, 2002, 1.0, 10000, 0.5, 1L)); // good Neighbors
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(3001, 3002, 1.0, 10000, 0.5, 1L)); // good Neighbors
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(4001, 4001, 1.0, 10000, 0.5, 1L)); // good Neighbors
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(4002, 4002, 1.0, 10000, 1.5, 1L)); // good Neighbors. Will NOT be filtered out due to  avgSw8AvgDelta > RI neighbor threshold
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(5001, 5001, 1.0, 10000, 0.5, 1L)); // will be filtered out as isHoALlowed = false
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(5002, 5002, 1.0, 10000, 1.5, 1L)); // good Neighbors. Will NOT be filtered out due to  avgSw8AvgDelta > RI neighbor threshold
        expectedFdnToCellRelationMap = buildExpectedCellRelationsMap(1, 5, 2);

        IntStream.of(1001, 1002, 2001, 2002, 3001, 3002, 4001, 4002, 5001, 5002).forEach(index -> when(allowedCellService.isAllowed(getIndexedFdn(index))).thenReturn(true));

        setIsHoAllowed(5001, false);

        List<Integer> interFreq = new ArrayList<>();
        interFreq.add(2002); // this one will be interFreq, but BW compliant
        interFreq.add(3002); // this one will be interFreq, but not BW compliant

        List<Integer> nonComplaintBw = new ArrayList<>();
        nonComplaintBw.add(3002); // this one will be interFreq, but not BW compliant

        buildCellRelationsNeighborCuDuMaps(featureContext, expectedFdnToCellRelationMap, nonComplaintBw, interFreq);

        List<PmBaselineNRCellDU> pmBaselineNRCellDUList = new ArrayList<>();
        fdnToFtRopNRCellDUMap.keySet().forEach(fdn -> {
            PmBaselineNRCellDU pmBaselineNRCellDU = new PmBaselineNRCellDU();
            pmBaselineNRCellDU.setFdn(fdn);
            pmBaselineNRCellDU.setUplInkThroughputQuartile50(baseUeUlTp);
            pmBaselineNRCellDUList.add(pmBaselineNRCellDU);
            lenient().when(pmBaselineNrCellDuRepo.findById(fdn)).thenReturn(Optional.of(pmBaselineNRCellDU));
        });

        Set<String> expectedFdnBaselineSet = new HashSet<>(fdnToFtRopNRCellDUMap.keySet());
        expectedFdnBaselineSet.remove(getIndexedFdn(6));
        when(pmBaselineNrCellDuRepo.findAllById(new ArrayList<>(expectedFdnBaselineSet))).thenReturn(pmBaselineNRCellDUList);

        log.trace("---------------------- FdnToFtRopNRCellDU for Test -----------------------------");
        featureContext.getFdnToFtRopNRCellDU().forEach((key, value) -> log.trace("{}: {}", key, value));
        log.trace("\n");
        return featureContext;
    }

    private static void addNullConnectedComponentsCells(Map<String, FtRopNRCellDU> fdnToFtRopNRCellDUMap) {
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(13, 14, 1.0, 10000, 10.0, null)); // Will be filtered out due to conn component = null
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(15, 16, 1.0, 10000, 10.0, null)); // Will be filtered out due to conn component = null
        fdnToFtRopNRCellDUMap.putAll(buildListFdnForTest(17, 18, 1.0, 10000, 10.0, null)); // Will be filtered out due to conn component = null
    }

    private static String getIndexedFdn(int index) {
        return String.format(FDN_TEMPLATE, "testo_" + index, "testo_" + index, "GNBDUFunction", "NRCellDU", "vesto_" + index);
    }

    private void setIsHoAllowed(int fdnIndex, boolean isHoAllowed) {
        int victimIndex = fdnIndex / 1000;
        expectedFdnToCellRelationMap.forEach((fdn, cellRelationMap) -> {
            if (fdn.endsWith("_" + victimIndex)) {
                cellRelationMap.forEach((nrCellRelation, nrCellCu) -> {
                    if (nrCellRelation.toFdn().endsWith("NRCellRelation=" + fdnIndex)) {
                        nrCellRelation.isHoAllowed(isHoAllowed);
                    }
                });
            }
        });
    }


    // Cell Builders and other misc. methods
    private void buildCellRelationsNeighborCuDuMaps(FeatureContext featureContext, Map<String, Map<NRCellRelation, NRCellCU>> fdnToCellRelationMap,
                                                    List<Integer> nonCompliantBw, List<Integer> interFreq) {
        Set<String> fdnFilter = buildCellRelationsNeighborCuDuMap(1, 5, 2).values()
                .stream().map(ManagedObjectId::toFdn).collect(Collectors.toSet());
        featureContext.getFdnToFtRopNRCellDU().keySet().forEach(fdn -> {
            int s1 = fdn.lastIndexOf('_');
            String fdnIndexStr = fdn.substring(s1 + 1);
            int fdnIndex = Integer.parseInt(fdnIndexStr);
            long nci = BASE_NCI + fdnIndex;

            log.trace("Building NrCellDu with fdn = '{}' and nci of '{}', isNonCompliantBw '{}',  isInterFreq '{}'", fdn, nci,
                    nonCompliantBw.contains(fdnIndex), interFreq.contains(fdnIndex));

            NRCellDU nRcellDU = buildNRCellDU(new NRCellDU(fdn), nci, nonCompliantBw.contains(fdnIndex), interFreq.contains(fdnIndex));
            featureContext.getFdnToNRCellDUMap().put(fdn, nRcellDU);

            if (fdnFilter.contains(fdn)) {
                when(relationService.getCellDUByCellCU(buildNrCellCu(
                        new NRCellCU(fdn.replace("GNBDUFunction", "GNBCUCPFunction").replace("NRCellDU", "NRCellCU")))))
                        .thenReturn(Optional.of(nRcellDU));
            }

            if (fdnToCellRelationMap.get(fdn) != null) {
                lenient().when(relationService.getAllowedCellRelationMap(eq(nRcellDU), any(LockByKey.class))).thenReturn(fdnToCellRelationMap.get(fdn));
            }
        });
    }

    private void printInfo(FeatureContext featureContext, String message) {
        log.info("\n");
        log.info("-------------------- CellsForMitigation {} -------------------------------", message);
        featureContext.getFtRopNRCellDUCellsForMitigation().forEach(mitCell -> {
            log.info("----- Victim FDN -------");
            log.info("{}", mitCell.getMoRopId().getFdn());
            log.info(" ----- NR Cell Relation Map -------");
            mitCell.getCellRelationMap().forEach((viCuMoID, neighBorCuMoId) -> log.info("\n   {} \n:    {}", viCuMoID, neighBorCuMoId));
            log.info(" ----- ftRop Neighbor DuCell -------");
            mitCell.getNeighborFtRopNRCellDUFdns().forEach(ftRopNeighborDuCellFdn -> log.info("{}", ftRopNeighborDuCellFdn));
            log.info(" ----- Neighbor CuCell -------");
            mitCell.getNeighborNrCell().keySet().forEach(neighborCuCell -> log.info("{}", neighborCuCell));
            log.info(" ----- Neighbor DuCell -------");
            mitCell.getNeighborNrCell().values().forEach(neighborDuCell -> log.info("{}", neighborDuCell));
            log.info(" ----- Neighbors CuCell (Intra) -------");
            mitCell.getIntraFneighborNrCellCu().forEach(neighborIntraF -> log.info("{}", neighborIntraF));
            log.info(" ----- Neighbors CuCell (Inter) -------");
            mitCell.getInterFneighborNrCellCu().forEach(neighborIntraF -> log.info("{}", neighborIntraF));
            log.info("\n\n\n ");
        });
    }
}
