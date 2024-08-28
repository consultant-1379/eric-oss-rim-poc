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
package com.ericsson.oss.apps.data.collection.features.handlers.geospatial;

import com.ericsson.oss.apps.client.CtsClient;
import com.ericsson.oss.apps.client.NcmpClient;
import com.ericsson.oss.apps.config.DuctDetectionConfig;
import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.model.GeoData;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import io.micrometer.core.instrument.Counter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuctStrengthSelectorTest {

    public static final long ROP_TIME = 123456789L;

    @Mock
    private CtsClient ctsClient;
    @Mock
    private NcmpClient ncmpClient;
    @Mock
    private DuctDetectionConfig ductDetectionConfig;

    @Mock
    private ThreadingConfig threadingConfig;

    FeatureContext featureContext;

    @Mock
    private Counter counter;

    private static final String FDN_TEMPLATE = "SubNetwork=GINO01,MeContext=%s,ManagedElement=%s,GNBDUFunction=1,NRCellDU=%s";

    private static final String FDN1 = String.format(FDN_TEMPLATE, "mino", "mino", "lino");
    private static final String FDN2 = String.format(FDN_TEMPLATE, "pino", "pino", "rino");

    static GridCoverage2D gridCoverage2D;

    GridCoverage2D testGridCoverage2D;

    @Spy
    @InjectMocks
    private DuctStrengthSelector ductStrengthSelector;

    private ManagedObjectId moid2;
    private ManagedObjectId moid1;

    @BeforeAll
    static void loadCoverage() throws IOException {
        gridCoverage2D = GeotiffTestUtils.loadGridCoverage2D(GeotiffTestUtils.GEO_TIFF_FILE_PATH);
    }

    @BeforeEach
    void setUp() {
        when(ductDetectionConfig.getMinAvgDeltaIpn()).thenReturn(3D);
        moid1 = ManagedObjectId.of(FDN1);
        moid2 = ManagedObjectId.of(FDN2);
        featureContext = new FeatureContext(ROP_TIME);
        testGridCoverage2D = gridCoverage2D;
    }

    @Test
    void calculateDuctStrengthNoCellsAboveDeltaIPN() {
        withPmRopCells(2.9, 2.8)
                .withRunDuctStrength()
                .withVerifyInitialInvocations();
        verifyNoMoreInteractions(ductStrengthSelector);
    }

    @Nested
    class TestsWithoutWeatherData {

        @BeforeEach
        void setup() {
            when(threadingConfig.getPoolSizeForNcmpCmQuery()).thenReturn(4);
        }

        @Test
        void calculateDuctStrengthCellsAboveDeltaIPNNoCmData() {
            withPmRopCells(3.1, 3.2)
                    .withRunDuctStrength()
                    .withVerifyInitialInvocations();
        }

        @Test
        void calculateDuctStrengthCellsAboveDeltaIPNNoWeatherData() {
            testGridCoverage2D = null;
            withPmRopCells(3.1, 3.2)
                    .withNRCEllDUBands(moid1, moid2, 78)
                    .withRunDuctStrength()
                    .withVerifyInitialInvocations();
            assertTrue(featureContext.getFdnToNRCellDUMap().containsKey(FDN1));
            assertTrue(featureContext.getFdnToNRCellDUMap().containsKey(FDN2));
        }

        @AfterEach
        void checkResults() {
            verifyNoMoreInteractions(ductStrengthSelector);
            assertTrue(featureContext.getFtRopNRCellDUPairs().isEmpty());
        }
    }


    @Nested
    class TestsWithWeatherData {

        @BeforeEach
        void setup() {
            when(ductDetectionConfig.getMaxDetectedCells()).thenReturn(2);
            when(threadingConfig.getPoolSizeForCtsGeoQuery()).thenReturn(2);
            when(threadingConfig.getPoolSizeForNcmpCmQuery()).thenReturn(4);
        }

        @Test
        void calculateDuctStrengthCellsAboveDeltaIPNNoGeoData() {
            when(ctsClient.getNrCellGeoData(any())).thenReturn(Optional.empty());
            withPmRopCells(3.1, 3.2)
                    .withNRCEllDUBands(moid1, moid2, 78)
                    .withRunDuctStrength()
                    .withVerifyInitialInvocations();
            verifyCtsInteraction();
            verify(ductStrengthSelector).getGeoDataForCells(any(), isNull());
            verify(ductStrengthSelector).getGeoInformationForInterferenceCells(any(), eq(gridCoverage2D));
            verifyNoMoreInteractions(ductStrengthSelector);
            assertTrue(featureContext.getFtRopNRCellDUPairs().isEmpty());
        }

        @Test
        void calculateDuctStrengthCellsAboveDeltaIPNBelowDuctingStrength() {
            withGeoCoordinatesOutOfDuct()
                    .withNRCEllDUBands(moid1, moid2, 78)
                    .withBasicDuctConditions();
            verify(ductStrengthSelector).getGeoInformationForInterferenceCells(any(), eq(gridCoverage2D));
            verifyNoMoreInteractions(ductStrengthSelector);
            assertTrue(featureContext.getFtRopNRCellDUPairs().isEmpty());
        }

        @Test
        void calculateDuctStrengthCellsAboveDeltaIPNSortOrder() {
            when(ductDetectionConfig.getMaxDetectedCells()).thenReturn(1);
            withGeoCoordinatesInDuctBelowGuard()
                    .withNRCEllDUBands(moid1, moid2, 78)
                    .withCoverageCrop()
                    .withBasicDuctConditions()
                    .withGeoInteraction();

            verify(ductStrengthSelector).getGeoInformationForInterferenceCells(eq(List.of(featureContext.getFtRopNRCellDU(FDN2), featureContext.getFtRopNRCellDU(FDN1))), eq(gridCoverage2D));
            verify(ductStrengthSelector).createBandToFdnMap(eq(List.of(new CellAndGeoInfo(null, FDN2))));
            verify(ductStrengthSelector).calculateDuctStrengthForCellPairs(eq(featureContext.getRopTimeStamp()), any(), any());

            verifyNoMoreInteractions(ductStrengthSelector);
            assertTrue(featureContext.getFtRopNRCellDUPairs().isEmpty());
        }

        @Test
        void calculateDuctStrengthCellsAboveDuctingStrengthBelowGuard() {
            withGeoCoordinatesInDuctBelowGuard()
                    .withNRCEllDUBands(moid1, moid2, 78)
                    .withCoverageCrop()
                    .withBasicDuctConditions()
                    .withGeoInteraction();

            verify(ductStrengthSelector).createBandToFdnMap(any());
            verify(ductStrengthSelector).filterCellPairsOnGuardDistanceAndBand(any(), any());
            verify(ductStrengthSelector).calculateDuctStrengthForCellPairs(eq(featureContext.getRopTimeStamp()), any(), any());

            verifyNoMoreInteractions(ductStrengthSelector);
            assertTrue(featureContext.getFtRopNRCellDUPairs().isEmpty());
        }

        @Test
        void calculateDuctStrengthCellsAboveDuctingStrengthAboveGuardDifferentBands() {
            withGeoCoordinatesInDuctAboveGuard()
                    .withNRCEllDUBands(moid1, moid2, 1)
                    .withCoverageCrop()
                    .withBasicDuctConditions()
                    .withGeoInteraction();

            verify(ductStrengthSelector).createBandToFdnMap(any());
            verify(ductStrengthSelector).filterCellPairsOnGuardDistanceAndBand(any(), any());
            verify(ductStrengthSelector).calculateDuctStrengthForCellPairs(eq(featureContext.getRopTimeStamp()), any(), any());

            verifyNoMoreInteractions(ductStrengthSelector);
            assertTrue(featureContext.getFtRopNRCellDUPairs().isEmpty());
        }

        @Test
        void calculateDuctStrengthCellsBandMatchingPairBelowMinDuctStrength() {
            withGeoCoordinatesBelowDuctingThreshold()
                    .withNRCEllDUBands(moid1, moid2, 78)
                    .withCoverageCrop()
                    .withBasicDuctConditions()
                    .withGeoInteraction();

            verify(ductStrengthSelector).createBandToFdnMap(any());
            verify(ductStrengthSelector).filterCellPairsOnGuardDistanceAndBand(any(), any());
            verify(ductStrengthSelector).calculateDuctStrengthForCellPairs(eq(featureContext.getRopTimeStamp()), any(), any());

            verifyNoMoreInteractions(ductStrengthSelector);
            assertTrue(featureContext.getFtRopNRCellDUPairs().isEmpty());
        }

        @Test
        void calculateDuctStrengthCellsBandMatchingPairAboveMinDuctStrength() {
            withGeoCoordinatesAboveThreshold()
                    .withNRCEllDUBands(moid1, moid2, 78)
                    .withCoverageCrop()
                    .withBasicDuctConditions()
                    .withGeoInteraction();
            List<FtRopNRCellDUPair> resultList = featureContext.getFtRopNRCellDUPairs();
            FtRopNRCellDUPair result1 = resultList.get(0);
            FtRopNRCellDUPair result2 = resultList.get(1);
            assertEquals(75, result1.getDuctStrength());
            assertEquals(75, result2.getDuctStrength());
            assertEquals(85.68, result1.getGuardDistance());
            assertEquals(85.68, result2.getGuardDistance());
            assertEquals(0.7145, result1.getGuardOverDistance(), 0.0001);
        }
    }

    private DuctStrengthSelectorTest withBasicDuctConditions() {
        withDuctingThreshold()
                .withPmRopCells(3.1, 3.2)
                .withRunDuctStrength()
                .withVerifyInitialInvocations()
                .withVerifyCtsInteraction();
        return this;
    }

    private void withGeoInteraction() {
        verify(ductStrengthSelector).filterCellPairsOnGuardDistanceAndBand(any(), any());
        verify(ductStrengthSelector).getGeoInformationForInterferenceCells(any(), eq(gridCoverage2D));
        verify(ductStrengthSelector).getGeoDataForCells(any(), isNull());
    }


    private DuctStrengthSelectorTest withNRCEllDUBands(ManagedObjectId moid1, ManagedObjectId moid2, int band1) {
        NRCellDU nRcellDU1 = buildNRCellDU(band1, moid1.toFdn());
        NRCellDU nRcellDU2 = buildNRCellDU(78, moid2.toFdn());
        when(ncmpClient.getCmResource(moid1, NRCellDU.class)).thenReturn(Optional.of(nRcellDU1));
        when(ncmpClient.getCmResource(moid2, NRCellDU.class)).thenReturn(Optional.of(nRcellDU2));
        return this;
    }

    private NRCellDU buildNRCellDU(int band, String fdn) {
        NRCellDU nRcellDU = new NRCellDU(fdn);
        nRcellDU.setBandList(Collections.singletonList(band));
        nRcellDU.setTddSpecialSlotPattern(NRCellDU.TddSpecialSlotPattern.TDD_SPECIAL_SLOT_PATTERN_03);
        nRcellDU.setSubCarrierSpacing(30);
        return nRcellDU;
    }

    private DuctStrengthSelectorTest withGeoCoordinatesInDuctAboveGuard() {
        return withGeoCoordinates(3.4974, 42.6313, 4.7773, 42.8640);
    }

    private DuctStrengthSelectorTest withGeoCoordinatesInDuctBelowGuard() {
        return withGeoCoordinates(4.4864, 42.8640, 4.4863, 42.8641);
    }

    private DuctStrengthSelectorTest withGeoCoordinatesOutOfDuct() {
        return withGeoCoordinates(4.7773, 44.9195, 6.5419, 48.8949);
    }

    private DuctStrengthSelectorTest withGeoCoordinatesBelowDuctingThreshold() {
        return withGeoCoordinates(4.0404, 42.7476, -3.9104, 44.3960);
    }

    private DuctStrengthSelectorTest withGeoCoordinatesAboveThreshold() {
        return withGeoCoordinates(3.4586, 42.5731, 4.7966, 43.0191);
    }

    private DuctStrengthSelectorTest withGeoCoordinates(double lat1, double lon1, double lat2, double lon2) {
        when(ctsClient.getNrCellGeoData(FDN1)).thenReturn(Optional.of(GeoData.builder().coordinate(new Coordinate(lat1, lon1)).fdn(FDN1).build()));
        when(ctsClient.getNrCellGeoData(FDN2)).thenReturn(Optional.of(GeoData.builder().coordinate(new Coordinate(lat2, lon2)).fdn(FDN2).build()));
        return this;
    }


    private void withVerifyCtsInteraction() {
        verifyCtsInteraction();
        verify(ductStrengthSelector).getGeoDataForCells(any(), isNull());
    }

    private void verifyCtsInteraction() {
        verify(ctsClient, times(1)).getNrCellGeoData(FDN1);
        verify(ctsClient, times(1)).getNrCellGeoData(FDN2);
    }

    private DuctStrengthSelectorTest withCoverageCrop() {
        when(ductDetectionConfig.getMaxLon()).thenReturn(8d);
        when(ductDetectionConfig.getMinLon()).thenReturn(-5d);
        when(ductDetectionConfig.getMaxLat()).thenReturn(51d);
        when(ductDetectionConfig.getMinLat()).thenReturn(42d);
        when(ductDetectionConfig.getMinAvgDeltaIpn()).thenReturn(3D);
        when(ductDetectionConfig.getDuctStrengthRanges()).thenReturn(Arrays.asList(35, 43, 55, 65, 75, 95, 110, 150));
        return this;
    }

    private DuctStrengthSelectorTest withDuctingThreshold() {
        when(ductDetectionConfig.getMinDetectedDuctStrength()).thenReturn(43);
        return this;
    }

    private DuctStrengthSelectorTest withVerifyInitialInvocations() {
        verify(ductStrengthSelector).handle(featureContext);
        verify(ductStrengthSelector).calculateDuctStrength(ROP_TIME);
        verify(ductStrengthSelector).getCellsAboveDeltaInterferenceLevel();
        return this;
    }

    private DuctStrengthSelectorTest withRunDuctStrength() {
        featureContext.setLatestCoverage(testGridCoverage2D);
        ductStrengthSelector.handle(featureContext);
        return this;
    }

    private DuctStrengthSelectorTest withPmRopCells(double avgSw8AvgDelta1, double avgSw8AvgDelta2) {
        featureContext.getFdnToFtRopNRCellDU().put(FDN1, getFTRopNrCellDu(FDN1, avgSw8AvgDelta1));
        featureContext.getFdnToFtRopNRCellDU().put(FDN2, getFTRopNrCellDu(FDN2, avgSw8AvgDelta2));
        return this;
    }

    private FtRopNRCellDU getFTRopNrCellDu(String fdn, double avgSw8AvgDelta) {
        FtRopNRCellDU ftRopNRCellDU = new FtRopNRCellDU(new MoRopId(fdn, ROP_TIME));
        ftRopNRCellDU.setAvgSw8AvgDeltaIpN(avgSw8AvgDelta);
        return ftRopNRCellDU;
    }
}