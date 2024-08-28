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
package com.ericsson.oss.apps.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ericsson.oss.apps.client.CtsClient;
import com.ericsson.oss.apps.client.NcmpClient;
import com.ericsson.oss.apps.data.collection.allowlist.AllowedNrCellDu;
import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.repositories.AllowedNrCellDuRepo;
import com.ericsson.oss.apps.utils.LockByKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

@SpringBootTest
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:relation/setup.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:relation/teardown.sql")
})
class CellRelationServiceItTest {

    private static final String CELL_DU = "SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00002-1";
    private static final String CELL_CU = "SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1";

    @MockBean
    private CtsClient ctsClient;
    @MockBean
    private NcmpClient ncmpClient;

    @Autowired
    private CellRelationService relationService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedNrCellDuRepo allowedNrCellDuRepo;

    @Test
    void testCellRelationMapping() {
        allowedNrCellDuRepo.saveAll(List.of(
                getAllowedNrCellDU("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00002-1"),
                getAllowedNrCellDU("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00002-2"),
                getAllowedNrCellDU("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003,GNBDUFunction=1,NRCellCU=NR03gNodeBRadio00003-1")
        ));
        var mutualRelation = Map.entry(
                ManagedObjectId.of("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1,NRCellRelation=auto105"),
                ManagedObjectId.of("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-2,NRCellRelation=325"));
        var notMutualRelation = Map.entry(
                ManagedObjectId.of("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1,NRCellRelation=297"),
                ManagedObjectId.of("SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00003-1,NRCellRelation=325"));
        var expectedCellRelationMap = Map.ofEntries(mutualRelation, notMutualRelation);
        var expectedMutualRelationMap = Map.ofEntries(mutualRelation);

        testAndVerifyRelationMaps(expectedCellRelationMap, expectedMutualRelationMap);

        allowedNrCellDuRepo.deleteAll();
    }

    @Test
    void testCellRelationMappingNoAllowedCElls() {
        Map<ManagedObjectId, ManagedObjectId> expectedCellRelationMap = Collections.emptyMap();
        Map<ManagedObjectId, ManagedObjectId> expectedMutualRelationMap = Collections.emptyMap();
        testAndVerifyRelationMaps(expectedCellRelationMap, expectedMutualRelationMap);
    }

    private void testAndVerifyRelationMaps(Map<ManagedObjectId, ManagedObjectId> expectedCellRelationMap, Map<ManagedObjectId, ManagedObjectId> expectedMutualRelationMap) {
        String cmHandle = "DUMMY";
        when(ctsClient.getGNBCmHandleByCGI(any())).thenReturn(Optional.of(cmHandle));
        NRCellDU cellDU = new NRCellDU(CELL_DU);
        cellDU.setNCI(1343813L);
        var relationMap = relationService.getAllowedCellRelationMap(cellDU, new LockByKey<>());
        assertEquals(expectedCellRelationMap.keySet(), relationMap.keySet().stream().map(ManagedObject::getObjectId).collect(Collectors.toSet()));
        verify(ncmpClient, times(1)).syncMeContext(cmHandle);
        var mutualMap = relationService.mapMutualRelations(relationMap);
        assertEquals(expectedMutualRelationMap.keySet(), mutualMap.keySet().stream().map(ManagedObject::getObjectId).collect(Collectors.toSet()));
    }

    @Test
    void testGetCellDUByCellCU() {
        Optional<NRCellDU> cell = relationService.getCellDUByCellCU(new NRCellCU(CELL_CU));
        assertTrue(cell.isPresent());
        assertEquals(CELL_DU, cell.get().getObjectId().toFdn());
    }

    private AllowedNrCellDu getAllowedNrCellDU(String fdn) {
        AllowedNrCellDu allowedNrCellDu = new AllowedNrCellDu();
        allowedNrCellDu.setObjectId(ManagedObjectId.of(fdn));
        return allowedNrCellDu;
    }
}
