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
package com.ericsson.oss.apps.data.collection.features.handlers;

import com.ericsson.oss.apps.classification.CellMitigationService;
import com.ericsson.oss.apps.config.RopProcessingConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.aggregation.PmSWNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.aggregation.SlidingWindowAggregationService;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import io.micrometer.core.instrument.Counter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class KpiCalculatorTest {

    public static final double DELTA_IPN_SCALING_FACTOR = 2D;
    @Mock
    private PmRopNrCellDuRepo pmRopNrCellDuRepo;
    @Mock
    private Counter numKpiRopRecordsProcessed;
    @Mock
    private CellMitigationService cellMitigationService;

    @Mock
    private SlidingWindowAggregationService slidingWindowAggregationService;

    @Mock
    private RopProcessingConfig ropProcessingConfig;

    @Spy
    @InjectMocks
    private KpiCalculator kpiCalculator;

    @ParameterizedTest
    @CsvSource(value = {
            "1, 2, 3, 4, 5, 30, 0.5D",
            "null, 2, 3, 4, 6, 30, 0.5D",
            "1, null, 3, 5, 6, 30, 0.5D",
            "2, 4, null, 4, 5, 30, 0.5D",
            "1, 2, 2, null, 5, 10, 1D",
            "1, 2, 3, 4, null, 20, 0.5D",
            "1, 2, 3, 4, 5, null, NaN",
            "1, 2, 3, 4, 5, 0, NaN"
    }, nullValues = {"null"})
    void calculateNaDlRBSym(
            Long pmMacRBSymUsedPdcchTypeA,
            Long pmMacRBSymUsedPdschTypeA,
            Long pmMacRBSymUsedPdschTypeABroadcasting,
            Long pmMacRBSymUsedPdcchTypeB,
            Long pmMacRBSymCsiRs,
            Long pmMacRBSymAvailDl,
            double result
    ) {
        Double dlRBSym = calculateDlRBSym(pmMacRBSymUsedPdcchTypeA,
                pmMacRBSymUsedPdschTypeA,
                pmMacRBSymUsedPdschTypeABroadcasting,
                pmMacRBSymUsedPdcchTypeB,
                pmMacRBSymCsiRs,
                pmMacRBSymAvailDl);
        assertEquals(result, dlRBSym);
    }

    private PmRopNRCellDU buildInput() {
        return new PmRopNRCellDU(new MoRopId(FDN1, 123456789L));
    }

    private Double calculateDlRBSym(Long pmMacRBSymUsedPdcchTypeA,
                                    Long pmMacRBSymUsedPdschTypeA,
                                    Long pmMacRBSymUsedPdschTypeABroadcasting,
                                    Long pmMacRBSymUsedPdcchTypeB,
                                    Long pmMacRBSymCsiRs,
                                    Long pmMacRBSymAvailDl) {
        PmRopNRCellDU input = buildInput();
        input.setPmMacRBSymUsedPdcchTypeA(pmMacRBSymUsedPdcchTypeA);
        input.setPmMacRBSymUsedPdschTypeA(pmMacRBSymUsedPdschTypeA);
        input.setPmMacRBSymUsedPdschTypeABroadcasting(pmMacRBSymUsedPdschTypeABroadcasting);
        input.setPmMacRBSymUsedPdcchTypeB(pmMacRBSymUsedPdcchTypeB);
        input.setPmMacRBSymCsiRs(pmMacRBSymCsiRs);
        input.setPmMacRBSymAvailDl(pmMacRBSymAvailDl);
        return kpiCalculator.calcDLRBSymUtil(input);
    }

    @Nested
    class TestKpisMapping {

        FeatureContext featureContext;
        PmRopNRCellDU input;

        @BeforeEach
        void setup() {
            featureContext = new FeatureContext(123456789L);
            input = buildInput();
            when(cellMitigationService.getFdnToUetpMap()).thenReturn(Map.of(FDN1, 42D));
            when(pmRopNrCellDuRepo.findByMoRopId_RopTime(featureContext.getRopTimeStamp())).thenReturn(List.of(input));
        }

        @ParameterizedTest
        @CsvSource(value = {
                "1234 , 345 , 8.16392921680778, 8.085, 45   , 44  , 35  , 1234, 10  , OTHER, 8",
                "NaN  , NaN , null            , 8.085, 45   , 44  , 35  , NaN,  40  , MIXED, 8",
                "1234 , NaN , null            , 8.085, 45   , 44  , 35  , 1234, 80  , REMOTE, 8",
                "NaN  , 1234, null            , 8.085, null , null, null, NaN,  NaN , NOT_DETECTED, 8",
                "null , null, null            , 8.085, NaN  , NaN , NaN , NaN,  null, NOT_DETECTED, 8",
                //test for soft start
                "1234 , 345 , 8.16392921680778, 8.085, 45   , 44  , 35  , 1234, 10  , NOT_DETECTED, 3",
                "NaN  , NaN , null            , 8.085, 45   , 44  , 35  , NaN,  40  , NOT_DETECTED, 3",
                "1234 , NaN , null            , 8.085, 45   , 44  , 35  , 1234, 80  , NOT_DETECTED, 3",
                "NaN  , 1234, null            , 8.085, null , null, null, NaN,  NaN , NOT_DETECTED, 3",
                "null , null, null            , 8.085, NaN  , NaN , NaN , NaN,  null, NOT_DETECTED, 3"
        }, nullValues = {"null"})
        void testCalculateInterferenceKpis(Double avgSw8AvgMaxDeltaIpN,
                                           Double avgSw8AvgSymbolDeltaIpN,
                                           Double expectedAvgDeltaIpn,
                                           Double avgSymbolDeltaIpn,
                                           Double totalBinSumSymbolDeltaIpn,
                                           Double totalBinSumMaxDeltaIpn,
                                           Double positiveBinSumSymbolDeltaIpn,
                                           Double avgSw8AvgDeltaIpN,
                                           Double avgSw8PercPositiveSymbolDeltaIpNSamples,
                                           InterferenceType expectedInterferenceType,
                                           Integer ropCount
        ) {
            when(ropProcessingConfig.getPmSWAvgSymbolDeltaIPNScalingFactor()).thenReturn(DELTA_IPN_SCALING_FACTOR);
            lenient().when(ropProcessingConfig.getOtherInterferencePercThreshold()).thenReturn(30D);
            lenient().when(ropProcessingConfig.getMixedInterferencePercThreshold()).thenReturn(70D);
            lenient().when(ropProcessingConfig.getMinRopCountForDetection()).thenReturn(4);

            // those field should not be populated by setter, they come from the DB via reflection
            ReflectionTestUtils.setField(input, "avgDeltaIpN", expectedAvgDeltaIpn);
            ReflectionTestUtils.setField(input, "avgSymbolDeltaIpn", avgSymbolDeltaIpn);
            ReflectionTestUtils.setField(input, "totalBinSumSymbolDeltaIpn", totalBinSumSymbolDeltaIpn);
            ReflectionTestUtils.setField(input, "totalBinSumMaxDeltaIpn", totalBinSumMaxDeltaIpn);
            ReflectionTestUtils.setField(input, "positiveBinSumSymbolDeltaIpn", positiveBinSumSymbolDeltaIpn);

            PmSWNRCellDU pmSWNRCellDU = new PmSWNRCellDU(FDN1, (byte) 5, avgSw8AvgMaxDeltaIpN, avgSw8AvgSymbolDeltaIpN, Double.NaN, Double.NaN, avgSw8PercPositiveSymbolDeltaIpNSamples, ropCount);
            when(slidingWindowAggregationService.calculateSlidingWindowCounters(123456789L)).thenReturn(List.of(pmSWNRCellDU));

            kpiCalculator.handle(featureContext);

            FtRopNRCellDU ftRopNRCellDU = getAndCheckFtRopNRCellDU();

            assertTrue(compareNullNaN(expectedAvgDeltaIpn, ftRopNRCellDU.getPmRadioMaxDeltaIpNAvg()));
            // direct mapping
            assertTrue(compareNullNaN(avgSw8AvgDeltaIpN, ftRopNRCellDU.getAvgSw8AvgDeltaIpN()));

            assertTrue(compareNullNaN(avgSw8AvgSymbolDeltaIpN, ftRopNRCellDU.getAvgSw8AvgSymbolDeltaIpN() / DELTA_IPN_SCALING_FACTOR));
            assertEquals(5, ftRopNRCellDU.getNRopsInLastSeenWindow());
            assertEquals(avgSymbolDeltaIpn, ftRopNRCellDU.getAvgSymbolDeltaIpn(), 0.001);
            assertTrue(compareNullNaN(totalBinSumSymbolDeltaIpn, ftRopNRCellDU.getTotalBinSumSymbolDeltaIpn()));
            assertTrue(compareNullNaN(totalBinSumMaxDeltaIpn, ftRopNRCellDU.getTotalBinSumMaxDeltaIpn()));
            assertTrue(compareNullNaN(positiveBinSumSymbolDeltaIpn, ftRopNRCellDU.getPositiveBinSumSymbolDeltaIpn()));
            assertTrue(compareNullNaN(avgSw8PercPositiveSymbolDeltaIpNSamples, ftRopNRCellDU.getAvgSw8PercPositiveSymbolDeltaIpNSamples()));
            assertEquals(expectedInterferenceType, ftRopNRCellDU.getInterferenceType());
        }

        @ParameterizedTest
        @CsvSource(value = {
                "1      , 2     , 1908.852812   , 1770.980438, 0.1 , 0.2 , 32  ",
                "null   , null  , null          , null       , null, null, null",
                "1      , 0     , null          , null       , null, null, null",
                "null   , 1     , null          , null       , null, null, null",
                "1      , null  , null          , null       , null, null, null"
        }, nullValues = {"null"})
        void testCalculateThroughputKpis(Double pmMacVolUlResUe,
                                         Double pmMacTimeUlResUe,
                                         Double avgSw2UlUeThroughput,
                                         Double avgSw8UlUeThroughput,
                                         Double pmMacVolDl,
                                         Double pmMacVolUl,
                                         Double expectedAvgUeTp
        ) {
            input.setPmMacVolUlResUe(pmMacVolUlResUe);
            input.setPmMacTimeUlResUe(pmMacTimeUlResUe);
            input.setPmMacVolDl(pmMacVolDl);
            input.setPmMacVolUl(pmMacVolUl);

            PmSWNRCellDU pmSWNRCellDU = new PmSWNRCellDU(FDN1, (byte) 5, Double.NaN, Double.NaN, avgSw8UlUeThroughput, avgSw2UlUeThroughput, Double.NaN, 8);
            when(slidingWindowAggregationService.calculateSlidingWindowCounters(123456789L)).thenReturn(List.of(pmSWNRCellDU));

            kpiCalculator.handle(featureContext);

            FtRopNRCellDU ftRopNRCellDU = getAndCheckFtRopNRCellDU();
            FtRopNRCellDU mappedFtRopNrCellDU = featureContext.getFdnToFtRopNRCellDU().get(ftRopNRCellDU.getMoRopId().getFdn());
            assertEquals(42, mappedFtRopNrCellDU.getUeTpBaseline());

            assertTrue(compareNullNaN(expectedAvgUeTp, ftRopNRCellDU.getAvgUlUe()));
            // direct mapping
            assertTrue(compareNullNaN(avgSw2UlUeThroughput, ftRopNRCellDU.getAvgSw2UlUeThroughput()));
            assertTrue(compareNullNaN(avgSw8UlUeThroughput, ftRopNRCellDU.getAvgSw8UlUeThroughput()));
            assertTrue(compareNullNaN(pmMacVolUl, ftRopNRCellDU.getPmMacVolUl()));
            assertTrue(compareNullNaN(pmMacVolDl, ftRopNRCellDU.getPmMacVolDl()));
            assertEquals(5, ftRopNRCellDU.getNRopsInLastSeenWindow());
        }

        @NotNull
        private FtRopNRCellDU getAndCheckFtRopNRCellDU() {
            FtRopNRCellDU ftRopNRCellDU = featureContext.getFtRopNRCellDU(input.getMoRopId().getFdn());
            assertEquals(input.getMoRopId(), ftRopNRCellDU.getMoRopId());
            assertEquals(input.getMoRopId(), featureContext.getFdnToFtRopNRCellDU().get(ftRopNRCellDU.getMoRopId().getFdn()).getMoRopId());
            return ftRopNRCellDU;
        }

        @AfterEach
        void verifyCalls() {
            verify(kpiCalculator).calcDLRBSymUtil(input);
            verify(numKpiRopRecordsProcessed).increment();
            verify(slidingWindowAggregationService).calculateSlidingWindowCounters(123456789L);
        }
    }

    private boolean compareNullNaN(Double input, double result) {
        return input == null || input.isNaN() ? Double.isNaN(result) : (input == result);
    }

}