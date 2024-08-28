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
package com.ericsson.oss.apps.config;

import com.ericsson.oss.apps.data.collection.deletion.ExpiredRopDataRemover;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ExpiredDataRemovalConfiguration {

    @Bean
    public ExpiredRopDataRemover nrCellDuExpiredRopDataRemover(final PmRopNrCellDuRepo pmRopNrCellDuRepo) {
        return new ExpiredRopDataRemover(pmRopNrCellDuRepo);
    }

}
