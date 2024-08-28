/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

import com.ericsson.oss.apps.model.mom.GNBCUCPFunction;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CmGNBCUCPFunctionRepo extends JpaRepository<GNBCUCPFunction, ManagedObjectId> {}
