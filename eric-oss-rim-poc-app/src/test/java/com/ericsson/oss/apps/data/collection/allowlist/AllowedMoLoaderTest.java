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
package com.ericsson.oss.apps.data.collection.allowlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.repositories.AllowedNrCellDuRepo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AllowedMoLoaderTest {

    private static final String BUCKET = "rim";

    private static final String CUSTOMER_ID_200238 = "200238";

    @Autowired
    private FileTracker fileTracker;

    @Mock
    private BdrConfiguration bdrConfiguration;
    @Mock
    private BdrClient bdrClient;

    @Mock
    private AllowedNrCellDuRepo allowedNrCellDuRepo;
    private AllowedMoLoader<AllowedNrCellDu> allowedMoLoader;

    @Captor
    ArgumentCaptor<AllowedNrCellDu> allowedNrCellDuArgumentCaptor;

    @BeforeEach
    void setup() {
        allowedMoLoader = new AllowedMoLoader<>(AllowedNrCellDu.class, allowedNrCellDuRepo, bdrClient, bdrConfiguration, fileTracker);
    }

    @Test
    void loadCsvPmTestRopSuccess_test() {
        testRop(2);
        var firstRecord = allowedNrCellDuArgumentCaptor.getAllValues().get(0);
        verifyParsing(firstRecord);
    }

    private void testRop(int expectedNumRecords) {
        Mockito.when(bdrConfiguration.getBucket()).thenReturn(BUCKET);
        Mockito.when(bdrClient.getObjectInputStream(Mockito.anyString(), Mockito.anyString())).thenAnswer(i -> {
            File file = getfile((String) i.getArguments()[1]);
            return new FileInputStream(file);
        });

        allowedMoLoader.load(AllowedMoLoaderTest.CUSTOMER_ID_200238);

        verify(allowedNrCellDuRepo, times(1)).deleteAll();
        verify(allowedNrCellDuRepo, times(expectedNumRecords)).save(allowedNrCellDuArgumentCaptor.capture());
    }

    private void verifyParsing(AllowedNrCellDu firstRecord) {
        assertEquals("SubNetwork=Unknown,MeContext=M9AT2772B2,ManagedElement=M9AT2772B2,GNBDUFunction=1,NRCellDU=K9AT2772B11", firstRecord.getObjectId().toString());
    }

    private static File getfile(final String resourceFile) {
        try {
            return new ClassPathResource(resourceFile).getFile();
        } catch (IOException ioException) {
            throw NoSuchKeyException.builder().message(String.format("Object not found: %s", resourceFile)).build();
        }
    }
}

