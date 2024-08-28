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
package com.ericsson.oss.apps.model.mitigation;

import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import com.ericsson.oss.apps.repositories.CmNrCellRelationRepo;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CellRelationChangeTest {

    public static final String RELATION_FDN1 = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Atlanta,MeContext=M9AT2043A2,ManagedElement=M9AT2043A2,GNBCUCPFunction=1,NRCellCU=K9AT2043A21,NRCellRelation=K9AT2043A11";
    public static final String RELATION_FDN2 = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Atlanta,MeContext=M9AT2043A2,ManagedElement=M9AT2043A2,GNBCUCPFunction=1,NRCellCU=K9AT2043A11,NRCellRelation=K9AT2043A21";

    @Autowired
    private CmNrCellRelationRepo relationRepo;
    @Autowired
    private CellRelationChangeRepo changeRepo;

    public static NRCellRelation buildRelation(String relationFdn) {
        NRCellRelation relation = new NRCellRelation(relationFdn);
        relation.setCellIndividualOffsetNR(0);
        return relation;
    }

    @Test
    @Order(1)
    void testCreateRelationChange() {
        NRCellRelation sourceRelation = buildRelation(RELATION_FDN1);
        NRCellRelation targetRelation = buildRelation(RELATION_FDN2);
        relationRepo.saveAll(List.of(sourceRelation, targetRelation));
        CellRelationChange change = new CellRelationChange(sourceRelation, targetRelation);
        assertEquals(MitigationState.CONFIRMED, change.getMitigationState());
        assertNotNull(changeRepo.save(change));
    }

    @Test
    @Order(2)
    void testGetAndUpdateRelationChange() {
        List<CellRelationChange> optionalChange = changeRepo.findByMitigationState(MitigationState.CONFIRMED);
        assertEquals(1, optionalChange.size());
        CellRelationChange change = optionalChange.get(0);
        change.setRequiredValue(10);
        change.getSourceRelation().setCellIndividualOffsetNR(10);
        change.getTargetRelation().setCellIndividualOffsetNR(-10);
        assertEquals(MitigationState.PENDING, change.getMitigationState());
        changeRepo.save(change);
    }

    @Test
    @Order(3)
    void testCascadedChange() {
        NRCellRelation relation1 = relationRepo.getReferenceById(ManagedObjectId.of(RELATION_FDN1));
        assertEquals(10, relation1.getCellIndividualOffsetNR());
        NRCellRelation relation2 = relationRepo.getReferenceById(ManagedObjectId.of(RELATION_FDN2));
        assertEquals(-10, relation2.getCellIndividualOffsetNR());
    }
}
