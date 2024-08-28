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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ericsson.oss.apps.client.BdrClient;

@ExtendWith(MockitoExtension.class)
class FileTrackerTest {
    private static final String ETAG = "etag1";
    private final FileTracker fileTracker = new FileTracker();

    @Mock
    BdrClient bdrClient;

    @Test
    void testAddEtagToMap() {
        String filename = "testFile";
        fileTracker.addEtagToMap(ETAG, filename);
        assertEquals(ETAG, fileTracker.getEtagsMap().get(filename));
    }

    @Test
    void testRemoveEtagfromMap() {
        String filename = "pm/testFile";
        fileTracker.getEtagsMap().put(filename, ETAG);
        fileTracker.removeEtagfromMap(filename);
        assertFalse(fileTracker.getEtagsMap().containsKey(filename));
    }

    @Test
    void testRemoveEtagfromMapNoEntry() {
        try {
            fileTracker.getEtagsMap().clear();
            String filename = "pm/testFile";
            fileTracker.removeEtagfromMap(filename);
            assertFalse(fileTracker.getEtagsMap().containsKey(filename));
        } catch (Exception e) {
            fail("Did not expect exception");
        }
    }

    @Test
    void testFileAlreadyLoaded() {
        String filename = "pm/testFile";
        fileTracker.getEtagsMap().put(filename, ETAG);
        boolean result = fileTracker.fileAlreadyLoaded(ETAG, filename);
        assertTrue(result);
    }

    @Test
    void testFileNotAlreadyLoaded() {
        String filename = "pm/testFile";
        boolean result = fileTracker.fileAlreadyLoaded(ETAG, filename);
        assertFalse(result);
    }
}
