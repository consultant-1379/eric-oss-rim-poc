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
package com.ericsson.oss.apps.data.collection.features.report;

import com.ericsson.oss.apps.client.CtsClient;
import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.report.nrcelldu.NRCellDUReporter;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRSectorCarrier;
import com.ericsson.oss.apps.repositories.CmNrCellDuRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NRCellDUReportTest {
    @InjectMocks
    private NRCellDUReporter nrCellDUReporter;
    @Mock
    private ReportSaver reportSaver;
    @Mock
    private CmNrCellDuRepo cellDuRepo;
    @Mock
    private CtsClient ctsClient;
    @Mock
    private ThreadingConfig threadingConfig;

    private static Long ROP_TIME = 123456789L;
    private static FeatureContext context;


    @BeforeEach
    void setUp() {
        context = new FeatureContext(ROP_TIME);
        NRCellDU nrCellDU = new NRCellDU("fdn");
        NRSectorCarrier nrSectorCarrier = new NRSectorCarrier("carrier");
        nrSectorCarrier.setArfcnDL(1);
        nrSectorCarrier.setBSChannelBwDL(1);
        nrCellDU.setNRSectorCarriers(new ArrayList<>(){{add(nrSectorCarrier);}});
        Mockito.when(cellDuRepo.findAll()).thenReturn(Arrays.asList(nrCellDU));
        when(threadingConfig.getPoolSizeForCtsGeoQuery()).thenReturn(2);
    }

    @Test
    void createTestWithFeatureContext() {
        nrCellDUReporter.handle(context);
        verify(reportSaver, times(1)).createReport(any(), eq(ROP_TIME), any());
        verify(cellDuRepo, times(1)).findAll();
        verify(ctsClient, times(1)).getNrCellGeoData(any());
    }
}
