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
package com.ericsson.oss.apps.data.collection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

//@formatter:off
/**
 * The Class FileTracker.
 *
 * Keep track of files previously loaded.
 *
 * Uses 'ETAG' : amazonS3.getObject(bucketName, objectPath).getObjectMetadata().getETag()
 * AWS S3 stores an etag for each object it stored in object store.
 * REF: https://docs.aws.amazon.com/AmazonS3/latest/API/API_Object.html
 *
 * This is unique per file, with some caveats: ref :
 * https://jira-oss.seli.wh.rnd.internal.ericsson.com/browse/IDUN-50800?focusedCommentId=6507140&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-6507140
 *
 * Basic Algorithm:
 *
 * Keep a MAP 'etagsMap' of 'full/path/to/filename to etag, as use it as a reference to see if the file has been previously loaded.
 *
 * If filename not in map -
 *     Load it
 *     Add etag to Map
 *
 * If filename in Map
 *     A: get etag of file to load
 *     B: get etag of file from map
 *     compare.
 *     if A != B
 *         load File
 *         Put new etag in map
 * *
 * This Utility class implements the getting an comparing of the eTags, adding them to the eTagsMap and removal from eTagsMap.
 */
//@formatter:on

@Slf4j
@Component
public class FileTracker {
    @Getter
    private Map<String, String> etagsMap = new ConcurrentHashMap<>();

    /**
     * Check if the file is already loaded, by checking if the etag is in the eTagsMap.
     *
     * Get its current etag corresponding to the filename of the requested object.
     * Look up the 'etagsMap' for the etag of the filename.
     * Compare etags, if same, then file is already loaded and return 'true', false o/w
     *
     * @param currentEtag
     *     the current Etag corresponding to the filename. Cannot be null
     *
     * @param filename
     *     the full/path/to/filename
     *
     * @return true, if file is already loaded.
     */
    public boolean fileAlreadyLoaded(String currentEtag, String filename) {
        boolean doLoad = false;
        if (etagsMap.containsKey(filename)) {
            String lastEtag = etagsMap.get(filename);
            log.trace("Is file Already Loaded? File = '{}', currentEtag = {}, lastEtag = {}, Are they equal: '{}'", filename, currentEtag, lastEtag,
                currentEtag.equals(lastEtag));
            if (currentEtag.equals(lastEtag)) {
                doLoad = true;
            }
        }
        log.trace("Is file '{}' : Already Loaded?'{}'", filename, doLoad);
        return doLoad;
    }

    /**
     * Removes the etag From the eTagsMap.
     *
     * @param filename
     *     the full/path/to/filename
     *
     */
    public void removeEtagfromMap(String filename) {
        log.trace("Removing ETAG for file '{}' from Map", filename);
        etagsMap.remove(filename);
    }

    /**
     * Adds the etag to eTagsMap.
     *
     * @param currentEtag
     *     the current Etag corresponding to the filename.
     *
     * @param filename
     *     the full/path/to/filename
     *
     */
    public void addEtagToMap(String currentEtag, String filename) {
        log.trace("Adding ETAG '{}' for file {} to Map", currentEtag, filename);
        etagsMap.put(filename, currentEtag);
    }
}
