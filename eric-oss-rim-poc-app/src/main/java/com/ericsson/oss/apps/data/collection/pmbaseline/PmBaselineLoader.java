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
package com.ericsson.oss.apps.data.collection.pmbaseline;

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.data.collection.CsvLoader;
import com.ericsson.oss.apps.data.collection.FileTracker;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The Class PmBaselineLoader. PM Loader class for PM Baseline data.
 *
 */
@Slf4j
public class PmBaselineLoader<T> extends CsvLoader<T> {

    private static final int PREFIX_LENGTH = "PmBaseline".length();

    /**
     * Instantiates a new pm baseline loader.
     *
     * @param clazz
     *     the generic clazz
     * @param repository
     *     the repository
     * @param bdrClient
     *     the bdr client
     * @param bdrConfiguration
     *     the bdr configuration
     * @param fileTracker
     *     the fileTracker for keeping track of the etag, to prevent same file being re-loaded.
     */
    public PmBaselineLoader(
            Class<T> clazz,
            JpaRepository<T, String> repository,
            BdrClient bdrClient,
            BdrConfiguration bdrConfiguration,
            FileTracker fileTracker
    ) {
        super(clazz, repository, bdrClient, bdrConfiguration, fileTracker);
    }

    /**
     * Load pm baseline.
     *
     * @param customerId
     *     the customer id, used to find the Baseline Data to load.
     *
     * @return the long, number Records Processed.
     */
    @Timed
    public long loadPmBaseline(String customerId) {
        String objectPath = getObjectPath(customerId);
        String currentEtag = bdrClient.getETag(bdrConfiguration.getBucket(), objectPath);
        if (fileTracker.fileAlreadyLoaded(currentEtag, objectPath)) {
            log.info("PM data for path {} already loaded, will not reload it.", objectPath);
            return 0;
        }
        final long numRecordsProcessed = loadCsvData(objectPath);
        // Done this way so that if exception thrown, then the etag not added to eTagMap
        if (numRecordsProcessed > 0) {
            fileTracker.addEtagToMap(currentEtag, objectPath);
        }
        return numRecordsProcessed;
    }

    private String getObjectPath(String customerId) {
        return String.format("pm/baseline/pm-%s-%s.csv.gz", clazz.getSimpleName().substring(PREFIX_LENGTH), customerId);
    }
}
