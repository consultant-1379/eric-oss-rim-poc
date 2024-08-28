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
package com.ericsson.oss.apps.repositories;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CmNrCellDuRepoTest {

    @Autowired
    CmNrCellDuRepo cmNrCellDuRepo;

    @Test
    void findByParametersChangesIsNull() {
        NRCellDU nrCellDU = new NRCellDU(FDN1);
        cmNrCellDuRepo.save(nrCellDU);
        val cellList = cmNrCellDuRepo.findByParametersChangesIsNotNull();
        assertTrue(cellList.isEmpty());
    }

    @Test
    void findByParametersChangesIsNotNull() {
        NRCellDU nrCellDU = new NRCellDU(FDN1);
        nrCellDU.setParametersChanges(new ParametersChanges(nrCellDU));
        cmNrCellDuRepo.save(nrCellDU);
        val cellList = cmNrCellDuRepo.findByParametersChangesIsNotNull();
        assertEquals(FDN1, cellList.get(0).getObjectId().toFdn());
    }
}