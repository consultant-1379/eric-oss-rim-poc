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
package com.ericsson.oss.apps.data.collection.deletion;

import com.ericsson.oss.apps.repositories.ExpiredRopDataRemoverRepo;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ExpiredRopDataRemover {

    private final ExpiredRopDataRemoverRepo expiredRopDataRemoverRepo;

    public ExpiredRopDataRemover(
            ExpiredRopDataRemoverRepo expiredRopDataRemoverRepo
    ) {
        this.expiredRopDataRemoverRepo = expiredRopDataRemoverRepo;
    }

    @Timed
    public void deletePmRop(long ropTimestamp) {
        log.info("Cleaning obsolete PM data older than {}", ropTimestamp);
        long numDeletedRecords = expiredRopDataRemoverRepo.deleteByMoRopId_RopTimeLessThan(ropTimestamp);
        log.info("deleted {} records", numDeletedRecords);
    }

}
