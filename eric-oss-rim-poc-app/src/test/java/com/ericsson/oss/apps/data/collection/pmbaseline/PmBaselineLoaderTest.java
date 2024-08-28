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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineHoCoefficient;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineNRCellDU;
import com.ericsson.oss.apps.repositories.PmBaselineHoCoefficientRepo;
import com.ericsson.oss.apps.repositories.PmBaselineNrCellDuRepo;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class PmBaselineLoaderTest {

    private static final String BUCKET = "rim";
    private static final String EXPECTED_FILE_PATH = "pm/baseline/";
    private static final String BASELINE_FILE_PATH = "src/test/resources/pm/baseline";
    private static final String BASELINE_FILE_NAME = "pm-NRCellDU-200238.csv.gz";
    private static final String BASELINE_HO_COEFFICIENT_FILE_NAME = "pm-HoCoefficient-200238.csv.gz";
    private static final String BASELINE_FILE_NAME_MALFORMED_LINE = "pm-NRCellDU-000001.csv.gz";
    private static final String BASELINE_FILE_NAME_MALFORMED_FILE = "pm-NRCellDU-000002.csv.gz";
    private final static String CUSTOMER_ID = "200238";
    private static final String[] FDN_HO_EXPECTED = new String[]{
            "ManagedElement=M9AT0163A3,GNBCUCPFunction=1,NRCellCU=A9AT0163A21,NRCellRelation=auto6580224301",
            "SubNetwork=ONRM_ROOT_MO,MeContext=M9AT0163A3,ManagedElement=M9AT0163A3,GNBCUCPFunction=1,NRCellCU=A9AT0163A21,NRCellRelation=auto6580224301",
            "SubNetwork=ONRM_ROOT_MO,SubNetwork=Atlanta,MeContext=M9AT0163A3,ManagedElement=M9AT0163A3,GNBCUCPFunction=1,NRCellCU=A9AT0163A21,NRCellRelation=auto6580224301"
    };
    private static final int[] N_HO_EXPECTED = new int[]{1, 2, 3};
    private static final String E_TAG = "d41d8cd98f00b204e9800998ecf8427e-2";
    private static final String E_TAG2 = "d41d8cd98f00b204e9800998ecf8427e-22";

    private final FileTracker fileTracker = new FileTracker();

    @Mock
    BdrConfiguration bdrConfiguration;

    @Mock
    BdrClient bdrClient;

    @Mock
    PmBaselineNrCellDuRepo pmBaselineNrCellDuRepo;

    @Mock
    PmBaselineHoCoefficientRepo pmBaselineHoCoefficientRepo;

    private PmBaselineLoader<PmBaselineNRCellDU> pmBaselineLoader;

    @Captor
    private ArgumentCaptor<PmBaselineNRCellDU> pmBaselineNRCellDUArgumentCaptor;

    @Captor
    private ArgumentCaptor<PmBaselineHoCoefficient> pmBaselineHoCoefficientArgumentCaptor;

    <T> void setup(Class<T> clazz, JpaRepository repo) {
        pmBaselineLoader = new PmBaselineLoader<>(clazz, repo, bdrClient, bdrConfiguration, fileTracker);
    }

    private void setupForFile(String fileName) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(Path.of(BASELINE_FILE_PATH, fileName).toString());
        Mockito.when(bdrClient.getObjectInputStream(BUCKET, String.format("pm/baseline/%s", fileName))).thenReturn(inputStream);
        Mockito.when(bdrConfiguration.getBucket()).thenReturn(BUCKET);
        Mockito.when(bdrClient.getETag(BUCKET, String.format("pm/baseline/%s", fileName))).thenReturn(E_TAG);

    }

    @Test
    void loadPmBaseline() throws FileNotFoundException {
        setup(PmBaselineNRCellDU.class, pmBaselineNrCellDuRepo);
        setupForFile(BASELINE_FILE_NAME);
        long numPmRopRecordsReceived = pmBaselineLoader.loadPmBaseline(CUSTOMER_ID);
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(EXPECTED_FILE_PATH + BASELINE_FILE_NAME), "Expected ETAG not present in eTagMap");
        assertEquals(2, numPmRopRecordsReceived, "Expected number of records not loaded");
        verify(pmBaselineNrCellDuRepo, times(2)).save(pmBaselineNRCellDUArgumentCaptor.capture());
        val pmBaselineNRCellDU = pmBaselineNRCellDUArgumentCaptor.getAllValues().get(1);
        verifyLine(pmBaselineNRCellDU);

        numPmRopRecordsReceived += pmBaselineLoader.loadPmBaseline(CUSTOMER_ID);
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(EXPECTED_FILE_PATH + BASELINE_FILE_NAME), "Expected ETAG not present in eTagMap");
        assertEquals(2, numPmRopRecordsReceived, "Expected number of records not loaded");
    }

    @Test
    void loadPmHoBaseline() throws FileNotFoundException {
        fileTracker.getEtagsMap().clear();
        PmHoBaselineLoader pmHoBaselineLoader = new PmHoBaselineLoader(PmBaselineHoCoefficient.class,
                pmBaselineHoCoefficientRepo,
                bdrClient,
                bdrConfiguration,
                fileTracker);
        ReflectionTestUtils.setField(pmHoBaselineLoader, "gNBIdLength", 24);
        setupForFile(BASELINE_HO_COEFFICIENT_FILE_NAME);
        long numPmRopRecordsReceived = pmHoBaselineLoader.loadPmBaseline(CUSTOMER_ID);
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(EXPECTED_FILE_PATH + BASELINE_HO_COEFFICIENT_FILE_NAME),
                "Expected ETAG not present in eTagMap");
        assertEquals(6, numPmRopRecordsReceived, "Expected number of records not loaded");
        verify(pmBaselineHoCoefficientRepo, times(3)).save(pmBaselineHoCoefficientArgumentCaptor.capture());
        List<PmBaselineHoCoefficient> pmBaselineHoCoefficientList = pmBaselineHoCoefficientArgumentCaptor.getAllValues();
        IntStream.range(0, 3).forEach(index -> verifyHoLine(pmBaselineHoCoefficientList.get(index), FDN_HO_EXPECTED[index], N_HO_EXPECTED[index]));

        // reload and check file not re-loaded
        numPmRopRecordsReceived += pmHoBaselineLoader.loadPmBaseline(CUSTOMER_ID);
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(EXPECTED_FILE_PATH + BASELINE_HO_COEFFICIENT_FILE_NAME),
                "Expected ETAG not present in eTagMap");
        assertEquals(6, numPmRopRecordsReceived, "Expected number of records not loaded");
    }

    @Test
    void loadTwoDifferentFilesEnsureTheyLoad() throws FileNotFoundException {
        fileTracker.getEtagsMap().clear();
        loadTwoPmBaseLineFiles();
    }

    @Test
    void loadSameFileDifferentETagsEnsureTheyLoad() throws FileNotFoundException {
        fileTracker.getEtagsMap().clear();
        this.loadPmBaseline();
        setup(PmBaselineNRCellDU.class, pmBaselineNrCellDuRepo);
        setupForFile(BASELINE_FILE_NAME);
        Mockito.when(bdrClient.getETag(BUCKET, String.format("pm/baseline/%s", BASELINE_FILE_NAME))).thenReturn(E_TAG2);
        long numPmRopRecordsReceived = pmBaselineLoader.loadPmBaseline(CUSTOMER_ID);
        assertEquals(2, numPmRopRecordsReceived, "Expected number of records not loaded");

        assertEquals(1, fileTracker.getEtagsMap().size(), "Expected one entries in eTagMap");
        assertEquals(E_TAG2, fileTracker.getEtagsMap().get(EXPECTED_FILE_PATH + BASELINE_FILE_NAME), "Expected ETAG not present in eTagMap");
    }

    /**
     * File with etag - 'etag1' get loaded successfully
     * Different version of same file is loaded ('etag2') but fail to load.
     * Ensure entry for this file is removed from map, so that even if the previous
     * good version 'etag1' is re-loaded, it will attempt to load.
     *
     * @throws FileNotFoundException
     */
    @Test
    void loadBadVersionOfPreviousGoodFileEnsureETagMapClear() throws FileNotFoundException {
        fileTracker.getEtagsMap().clear();
        fileTracker.getEtagsMap().put(EXPECTED_FILE_PATH + BASELINE_HO_COEFFICIENT_FILE_NAME, "etag");
        PmHoBaselineLoader pmHoBaselineLoader = new PmHoBaselineLoader(PmBaselineHoCoefficient.class,
                pmBaselineHoCoefficientRepo,
                bdrClient,
                bdrConfiguration,
                fileTracker);
        ReflectionTestUtils.setField(pmHoBaselineLoader, "gNBIdLength", 24);
        setupForFile(BASELINE_HO_COEFFICIENT_FILE_NAME);
        Mockito.when(bdrClient.getObjectInputStream(BUCKET, String.format("pm/baseline/%s", BASELINE_HO_COEFFICIENT_FILE_NAME))).thenReturn(null);
        long numPmRopRecordsReceived = pmHoBaselineLoader.loadPmBaseline(CUSTOMER_ID);

        assertFalse(fileTracker.getEtagsMap().containsKey(EXPECTED_FILE_PATH + BASELINE_HO_COEFFICIENT_FILE_NAME),
                "ETAG Should not be present in eTagMap");
        assertEquals(0, numPmRopRecordsReceived, "Expected number of records not loaded");
    }

    @Test
    void loadPmBaselineMalformedLine() throws FileNotFoundException {
        setup(PmBaselineNRCellDU.class, pmBaselineNrCellDuRepo);
        setupForFile(BASELINE_FILE_NAME_MALFORMED_LINE);
        long numPmRopRecordsReceived = pmBaselineLoader.loadPmBaseline("000001");
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(EXPECTED_FILE_PATH + BASELINE_FILE_NAME_MALFORMED_LINE),
                "Expected ETAG not present in eTagMap");
        assertEquals(1, numPmRopRecordsReceived, "Expected number of records not loaded");
        verify(pmBaselineNrCellDuRepo, times(1)).save(pmBaselineNRCellDUArgumentCaptor.capture());
        val pmBaselineNRCellDU = pmBaselineNRCellDUArgumentCaptor.getAllValues().get(0);
        verifyLine(pmBaselineNRCellDU);
    }

    @Test
    void loadPmBaselineMalformedFile() throws FileNotFoundException {
        fileTracker.getEtagsMap().clear();
        setup(PmBaselineNRCellDU.class, pmBaselineNrCellDuRepo);
        setupForFile(BASELINE_FILE_NAME_MALFORMED_FILE);
        long numPmRopRecordsReceived = pmBaselineLoader.loadPmBaseline("000002");
        assertFalse(fileTracker.getEtagsMap().containsKey(EXPECTED_FILE_PATH + BASELINE_HO_COEFFICIENT_FILE_NAME),
                "ETAG Should not be present in eTagMap");
        assertEquals(0, numPmRopRecordsReceived, "Expected number of records not loaded");
        verify(pmBaselineNrCellDuRepo, times(0)).save(any());
    }

    private void verifyLine(PmBaselineNRCellDU pmBaselineNRCellDU) {
        assertEquals("fdn2", pmBaselineNRCellDU.getFdn());
        assertEquals(1478, pmBaselineNRCellDU.getNRops());
        assertEquals(2004.967773, pmBaselineNRCellDU.getUplInkThroughputQuartile50(), 0.000001);
        assertEquals(1350.989346, pmBaselineNRCellDU.getUplInkThroughputQuartile25(), 0.000001);
        assertEquals(3225.804926, pmBaselineNRCellDU.getUplInkThroughputQuartile75(), 0.000001);
    }

    private void verifyHoLine(PmBaselineHoCoefficient pmBaselineHoCoefficient, String expectedFdn, int hoExpected) {
        assertEquals(expectedFdn, pmBaselineHoCoefficient.getFdnNrCellRelation());
        assertEquals(hoExpected, pmBaselineHoCoefficient.getNumberHandovers());
    }

    private void loadTwoPmBaseLineFiles() throws FileNotFoundException {
        this.loadPmHoBaseline();
        this.loadPmBaseline();
        assertEquals(2, fileTracker.getEtagsMap().size(), "Expected two entries in eTagMap");
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(EXPECTED_FILE_PATH + BASELINE_HO_COEFFICIENT_FILE_NAME),
                "Expected ETAG not present in eTagMap");
        assertEquals(E_TAG, fileTracker.getEtagsMap().get(EXPECTED_FILE_PATH + BASELINE_FILE_NAME), "Expected ETAG not present in eTagMap");
    }
}
