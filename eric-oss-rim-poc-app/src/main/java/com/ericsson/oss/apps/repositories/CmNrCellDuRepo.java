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

import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CmNrCellDuRepo extends JpaRepository<NRCellDU, ManagedObjectId> {

    @Query("select n from NRCellDU n left join n.parametersChanges pc where pc is not null")
    List<NRCellDU> findByParametersChangesIsNotNull();

}
