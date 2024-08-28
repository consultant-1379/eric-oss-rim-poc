/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRSectorCarrier;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SeparateInterAndIntraFreqNeighbourCellsTest {

    @InjectMocks
    SeparateInterAndIntraFreqNeighbourCells separateInterAndIntraFreqNeighbourCells;

    @Mock
    Counter counter;

    @Mock
    CellSelectionConfig cellSelectionConfig;

    FeatureContext context;
    NRCellDU nrCellDU;
    NRSectorCarrier nrSectorCarrier;
    NRCellDU neighborNrCellDU;
    NRSectorCarrier neighborNrSectorCarrier;
    NRCellCU neighborNrCellCU;
    FtRopNRCellDU ftRopNRCellDU;

    @BeforeEach
    void setUp() {
        context = new FeatureContext(0);
        ftRopNRCellDU = new FtRopNRCellDU(new MoRopId(FDN1, 0));
        context.setFtRopNRCellDUCellsForMitigation(List.of(ftRopNRCellDU));

        nrCellDU = new NRCellDU(FDN1);
        nrSectorCarrier = new NRSectorCarrier(FDN1 + ",NRSectorCarrier=1");
        nrSectorCarrier.setBSChannelBwDL(20);
        nrCellDU.getNRSectorCarriers().add(nrSectorCarrier);

        context.getFdnToNRCellDUMap().put(FDN1, nrCellDU);

        neighborNrCellDU = new NRCellDU(FDN2);
        ;
        neighborNrSectorCarrier = new NRSectorCarrier(FDN2 + ",NRSectorCarrier=1");
        neighborNrCellDU.getNRSectorCarriers().add(neighborNrSectorCarrier);

        neighborNrCellCU = new NRCellCU(NRCELL_CU_FDN);
        ftRopNRCellDU.getNeighborNrCell().put(neighborNrCellCU, neighborNrCellDU);
    }

    /**
     * Given
     * a cell for mitigation is in context
     * the corresponding NRCEllDU is in context
     * the corresponding NRCEllDU has an NRSectorCarrier with arfcn
     * there is a neighbor cell in context
     * the corresponding nrcelldu is in context
     * the corresponding nrcelldu has an NRSectorCarrier with the same arfcn
     * the corresponding nrcellcu fdn is in CDF
     * when
     * handle is invoked
     * then
     * the nrcellcu is in context intra cell list
     */
    @Test
    void testInfraNeighborCellsInRankingList() {
        setupGoodIntra();
        assertTrue(context.getFtRopNRCellDUCellsForMitigation().get(0).getIntraFneighborNrCellCu().contains(neighborNrCellCU));
    }

    private void setupGoodIntra() {
        ftRopNRCellDU.getCuFdnToHandovers().put(neighborNrCellCU.toFdn(), null);
        nrSectorCarrier.setArfcnDL(1);
        neighborNrSectorCarrier.setArfcnDL(1);
        separateInterAndIntraFreqNeighbourCells.handle(context);
    }

    /**
     * Given
     * a cell for mitigation is in context
     * the corresponding NRCEllDU is in context
     * the corresponding NRCEllDU has an NRSectorCarrier with arfcn
     * there is a neighbor cell in context
     * the corresponding nrcelldu is in context
     * the corresponding nrcelldu has an NRSectorCarrier with the same arfcn
     * the corresponding nrcellcu fdn is not in CDF
     * when
     * handle is invoked
     * then
     * the neighbor nrcellcu is in context intra cell list
     */
    @Test
    void testInfraNeighborCellsNotInRankingList() {
        nrSectorCarrier.setArfcnDL(1);
        neighborNrSectorCarrier.setArfcnDL(1);
        separateInterAndIntraFreqNeighbourCells.handle(context);
        assertFalse(context.getFtRopNRCellDUCellsForMitigation().get(0).getIntraFneighborNrCellCu().contains(neighborNrCellCU));
    }

    /**
     * Given
     * a cell for mitigation is in context
     * the corresponding NRCEllDU is in context
     * the corresponding NRCEllDU has an NRSectorCarrier with arfcn
     * there is a neighbor cell in context
     * the corresponding nrcelldu is in context
     * the corresponding nrcelldu has an NRSectorCarrier with a different arfcn
     * the corresponding nrcelldu has bandwidth equal to the mitigated cell
     * the corresponding nrcellcu fdn is in CDF
     * when
     * handle is invoked
     * then
     * the neighbor nrcellcu is not in context inter cell list
     */
    @Test
    void testGoodInterNeighborCellsInRankingList() {
        setupGoodInter();
        assertTrue(context.getFtRopNRCellDUCellsForMitigation().get(0).getInterFneighborNrCellCu().contains(neighborNrCellCU));
    }

    private void setupGoodInter() {
        setupAndRunInterfreqNeighborWithBandwidth(40);
    }

    /**
     * Given
     * a cell for mitigation is in context
     * the corresponding NRCEllDU is in context
     * the corresponding NRCEllDU has an NRSectorCarrier with arfcn
     * there is a neighbor cell in context
     * the corresponding nrcelldu is in context
     * the corresponding nrcelldu has an NRSectorCarrier with a different arfcn
     * the corresponding nrcelldu has bandwidth smaller than mitigated cell
     * the corresponding nrcellcu fdn is in CDF
     * when
     * handle is invoked
     * then
     * the neighbor nrcellcu is not in context
     */
    @Test
    void testBadInterNeighborCellsInRankingList() {
        Mockito.when(cellSelectionConfig.getCioRejectVictimBandwidthRatio()).thenReturn(1D);
        setupAndRunInterfreqNeighborWithBandwidth(15);
        assertFalse(context.getFtRopNRCellDUCellsForMitigation().get(0).getInterFneighborNrCellCu().contains(neighborNrCellCU));
    }

    /**
     * Given
     * a cell for mitigation is in context
     * the corresponding NRCEllDU is in context
     * the corresponding NRCEllDU has an NRSectorCarrier with arfcn
     * there is a neighbor cell in context
     * the corresponding nrcelldu is in context
     * the corresponding nrcelldu has an NRSectorCarrier with a different arfcn
     * the corresponding nrcelldu has bandwidth smaller than mitigated cell
     * the corresponding nrcellcu fdn is in CDF
     * the bandwidth check is disabled
     * when
     * handle is invoked
     * then
     * the neighbor nrcellcu is not in context
     */
    @Test
    void testInterNeighborCellsInRankingListNoBWCheck() {
        Mockito.when(cellSelectionConfig.getCioRejectVictimBandwidthRatio()).thenReturn(0D);
        setupAndRunInterfreqNeighborWithBandwidth(15);
        assertTrue(context.getFtRopNRCellDUCellsForMitigation().get(0).getInterFneighborNrCellCu().contains(neighborNrCellCU));
    }

    private void setupAndRunInterfreqNeighborWithBandwidth(int bSChannelBwDL) {
        ftRopNRCellDU.getCuFdnToHandovers().put(neighborNrCellCU.toFdn(), null);
        neighborNrSectorCarrier.setBSChannelBwDL(bSChannelBwDL);
        nrSectorCarrier.setArfcnDL(1);
        neighborNrSectorCarrier.setArfcnDL(2);
        separateInterAndIntraFreqNeighbourCells.handle(context);
    }

    //negative test cases (e.g. data missing)
    @Test
    void testNRCellDUMissing() {
        context.getFdnToNRCellDUMap().clear();
        setupGoodInter();
        assertFalse(context.getFtRopNRCellDUCellsForMitigation().get(0).getInterFneighborNrCellCu().contains(neighborNrCellCU));
    }
    @Test
    void testVictimCarrierMissing() {
        nrCellDU.getNRSectorCarriers().clear();
        setupGoodInter();
        assertFalse(context.getFtRopNRCellDUCellsForMitigation().get(0).getInterFneighborNrCellCu().contains(neighborNrCellCU));
    }

    @Test
    void testVictimCarrierNull() {
        nrCellDU.getNRSectorCarriers().set(0,null);
        setupGoodInter();
        assertFalse(context.getFtRopNRCellDUCellsForMitigation().get(0).getInterFneighborNrCellCu().contains(neighborNrCellCU));
    }

    @Test
    void testNeighborCarrierMissing() {
        neighborNrCellDU.getNRSectorCarriers().clear();
        setupGoodInter();
        assertFalse(context.getFtRopNRCellDUCellsForMitigation().get(0).getInterFneighborNrCellCu().contains(neighborNrCellCU));
    }


}