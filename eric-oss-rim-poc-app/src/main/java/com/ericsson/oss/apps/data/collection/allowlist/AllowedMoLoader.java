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
package com.ericsson.oss.apps.data.collection.allowlist;

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.data.collection.CsvLoader;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

@Slf4j
public class AllowedMoLoader<T> extends CsvLoader<T> {

    private static final int PREFIX_LENGTH = "Allowed".length();

    public AllowedMoLoader(
            Class<T> clazz,
            JpaRepository<T, ManagedObjectId> repository,
            BdrClient bdrClient,
            BdrConfiguration bdrConfiguration,
            FileTracker fileTracker
    ) {
        super(clazz, repository, bdrClient, bdrConfiguration, fileTracker);
    }


    /**
     * Load allowed cell list.
     *
     * @param customerId the customer id
     */
    @Timed
    public void load(String customerId) {
        log.info("Cleaning obsolete allowed list data for customer ID {}", customerId);
        repository.deleteAll();
        log.info("Loading allowed list data for CustomerId {}", customerId);
        loadCsvData(getObjectPath(customerId));
    }

    private String getObjectPath(String customerId) {
        return String.format("setup_files/allowed-%s-%s.csv.gz", clazz.getSimpleName().substring(PREFIX_LENGTH), customerId);
    }
}
