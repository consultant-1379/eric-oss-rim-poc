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

import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.config.ClusteringConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.mobility.MobilityMitigationReporter;
import com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.UplinkPowerMitigationReporter;
import com.ericsson.oss.apps.data.collection.features.report.nrcelldu.NRCellDUReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportCreatorTest {
    @InjectMocks
    private ReportCreator reportCreator;
    @Mock
    private CellSelectionConfig cellSelectionConfig;
    @Mock
    private ReportSaver reportSaver;
    @Mock
    private ClusteringConfig clusteringConfig;
    @Mock
    private NRCellDUReporter nrCellDUReporter;
    @Mock
    private UplinkPowerMitigationReporter uplinkPowerMitigationReporter;
    @Mock
    private MobilityMitigationReporter mobilityMitigationReporter;
    static Long ROP_TIME = 123456789L;
    static FeatureContext featureContext;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(clusteringConfig.getMinimumConnectedEdgeWeight()).thenReturn(Double.NEGATIVE_INFINITY);
        featureContext = new FeatureContext(ROP_TIME);
        featureContext.getFdnToFtRopNRCellDU().put(FDN1, new FtRopNRCellDU(new MoRopId(FDN1, ROP_TIME)) {
            private static final long serialVersionUID = 8675031521560423091L;
        });
    }

    @Test
    void createTestWithFeatureContext() {
        reportCreator.handle(featureContext);
        verify(reportSaver, times(4))
                .createReport(any(), eq(ROP_TIME), any());
        verify(nrCellDUReporter, times(1)).handle(featureContext);
        verify(uplinkPowerMitigationReporter, times(1)).handle(featureContext);
        verify(mobilityMitigationReporter, times(1)).handle(featureContext);
    }

    @Test
    void getFtNRCellDUPairsAboveMin_happCaseTest() {
        List<FtRopNRCellDUPair> ftRopNRCellDUPairList = getFtRopNRCellDuPairList(5, 1.1);
        ftRopNRCellDUPairList.addAll(getFtRopNRCellDuPairList(5, 0.9));
        featureContext.setFtRopNRCellDUPairs(ftRopNRCellDUPairList);

        List<FtRopNRCellDUPair> actualFtRopNRCellDUPairList = reportCreator.getFtNRCellDUPairsAboveMin(featureContext, 1.0);
        assertEquals(5, actualFtRopNRCellDUPairList.size());
    }

    @Test
    void getFtNRCellDUPairsAboveMin_nullTest() {
        featureContext.setFtRopNRCellDUPairs(null);

        List<FtRopNRCellDUPair> actualFtRopNRCellDUPairList = reportCreator.getFtNRCellDUPairsAboveMin(featureContext, 1.0);
        assertEquals(Collections.emptyList(), actualFtRopNRCellDUPairList);
    }

    @Test
    void getFtNRCellDUPairsAboveMin_emptyListTest() {
        featureContext.setFtRopNRCellDUPairs(Collections.emptyList());

        List<FtRopNRCellDUPair> actualFtRopNRCellDUPairList = reportCreator.getFtNRCellDUPairsAboveMin(featureContext, 1.0);
        assertEquals(Collections.emptyList(), actualFtRopNRCellDUPairList);
    }

    private List<FtRopNRCellDUPair> getFtRopNRCellDuPairList(int maxNumberCellPairs, double agressorScore) {
        List<FtRopNRCellDUPair> ftRopNRCellDUPairList = new ArrayList<>();
        IntStream.range(0, maxNumberCellPairs).forEach(i -> {
            FtRopNRCellDUPair ftRopNRCellDUPair = new FtRopNRCellDUPair();
            ftRopNRCellDUPair.setFdn1("fdn1");
            ftRopNRCellDUPair.setFdn2("fdn2");
            ftRopNRCellDUPair.setRopTime(ROP_TIME);
            ftRopNRCellDUPair.setAggressorScore(agressorScore);
            ftRopNRCellDUPairList.add(ftRopNRCellDUPair);
        });
        return ftRopNRCellDUPairList;
    }
}
