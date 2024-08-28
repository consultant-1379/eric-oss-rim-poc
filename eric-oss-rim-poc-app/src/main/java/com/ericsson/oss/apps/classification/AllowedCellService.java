/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.repositories.AllowedNrCellDuRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AllowedCellService {

    @Value("${app.data.allow-list.allow-all-fdn}")
    private boolean allowListAllowAllFdn;

    @Value("${app.data.netsim}")
    private boolean barZeroCellId;
    private final AllowedNrCellDuRepo allowedNrCellDuRepo;

    public boolean isAllowed(ManagedObjectId moId) {
        if (!allowListAllowAllFdn) {
            return allowedNrCellDuRepo.existsById(moId);
        }
        return true;
    }

    // this is a hack to deal wit the fact that localCellId is always zero in netsim
    // should never be used on a real network (or real network data)
    public boolean isCellIdBarred(int cellId) {
        return barZeroCellId && cellId==0;
    }

    public boolean isAllowed(String fdn) {
        return isAllowed(ManagedObjectId.of(fdn));
    }

}
