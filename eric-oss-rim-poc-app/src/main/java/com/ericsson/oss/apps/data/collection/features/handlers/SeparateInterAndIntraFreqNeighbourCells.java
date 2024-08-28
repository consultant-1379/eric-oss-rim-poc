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

import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.data.collection.HandOvers;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRSectorCarrier;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

//@formatter:off
/**
 * The Class SeparateInterAndIntraFreqNeighbourCells.
 * For each CELL in the resultant filtered Cell list ( where mitigation is required),
 *     victimCellArfcnDL equals NRCellDU.NRSectorCarrier.arfcnDL
 *     victimCellBSChannelBwDL equals NRCellDU.NRSectorCarrier.bSChannelBwDL
 *
 *     Create 'Handover Map' of fdn to number of handovers for each neighbor for this victim cell
 *
 *     for each cell in 'eligibleNeighborCellsList'
 *         Get neighborCellArfcnValueNRDl which equals NRCellCU.NRFrequency.arfcnValueNRDl from Neighbor Cell
 *
 *         Check 'fdn' of neighbor in HO Map, if # Handovers to this neighbor is zero, then exclude this neighbor
 *
 *         if victimCellArfcnDL is same as neighborCellArfcnValueNRDl
 *             then its Intra Frequency cell and add it to the list of INTRA neighbor Cells
 *
 *         if victimCellArfcnDL not equal neighborCellArfcnValueNRDl
 *             then this is inter Frequency Cell. So check if BW is compliant
 *
 *             neighborCellBSChannelBwDL equals NRCellDU.NRSectorCarrier.bSChannelBwDL from Neighbor Cell
 *
 *             if neighborCellBSChannelBwDL greater than or equal to victimCellBSChannelBwDL
 *                 then its Inter Frequency and BW compliant, so add it to list of INTER neighbor Cells.
 *
 * OUTPUT:
 *         Each Victim (Mitigation) Cell (FtRopNRCellDU) holds
 *              The (Master) list of good neighbor cells
 *              The List of intra frequency neighbor cells
 *              The List of inter frequency BW compliant neighbor cells
 *
 *         Feature context holds the List Victim (Mitigation) Cells
 */
//@formatter:on

@Component
@RequiredArgsConstructor
@Slf4j
public class SeparateInterAndIntraFreqNeighbourCells implements FeatureHandler<FeatureContext> {

    private final Counter numVictimCellsSelectedForMitigation;
    private final Counter numNeighborCellsIntraFrequency;
    private final Counter numNeighborCellsInterFrequencyBwCompliant;
    private final Counter numNeighborCellsDroppedInterFrequencyNonBwCompliant;

    private final CellSelectionConfig cellSelectionConfig;

    AtomicInteger numberCompliantInter;
    AtomicInteger numberInter;
    AtomicInteger numberIntra;
    AtomicInteger numberNoHandovers;
    AtomicInteger numberHandoverNonZero;
    AtomicInteger numberCannotFilterAndAddNoNrSectorCarrierInfo;

    @Override
    @Timed
    public void handle(FeatureContext featureContext) {
        numberCompliantInter = new AtomicInteger(0);
        numberInter = new AtomicInteger(0);
        numberIntra = new AtomicInteger(0);
        numberNoHandovers = new AtomicInteger(0);
        numberHandoverNonZero = new AtomicInteger(0);
        numberCannotFilterAndAddNoNrSectorCarrierInfo = new AtomicInteger(0);

        List<FtRopNRCellDU> ftRopNRCellDUCellsForMitigationList = featureContext.getFtRopNRCellDUCellsForMitigation();
        log.info("Processing {} Mitigation cells for Inter and Intra Frequency separation", ftRopNRCellDUCellsForMitigationList.size());
        ftRopNRCellDUCellsForMitigationList.forEach(mitigationCell -> {
            String fdn = mitigationCell.getMoRopId().getFdn();
            log.trace("Processing Mitigation cell with fdn '{}'", fdn);
            if (featureContext.getFdnToNRCellDUMap().get(fdn) == null) {
                log.error("Error Processing Mitigation cell with fdn '{}' for inter & intra frequency selection, "
                        + "Cannot find NrCellDU information", fdn);
                return;
            }
            NRCellDU nrCellDU = featureContext.getFdnToNRCellDUMap().get(fdn);
            log.trace("Processing NrCellDU Object for cell with fdn '{}'", nrCellDU.toFdn());
            // TODO: Clarify what to do as List of NRSectorCarriers returned. Is it all Match or any Match for Freq & BW
            if (nrCellDU.getNRSectorCarriers() == null || nrCellDU.getNRSectorCarriers().isEmpty()) {
                log.error("Error Processing Mitigation cell with fdn '{}' for inter & intra frequency selection, "
                        + "Cannot find NrCellDU NRSectorCarriers information", fdn);
                return;
            }
            NRSectorCarrier nRSectorCarrier = nrCellDU.getNRSectorCarriers().get(0);

            if (nRSectorCarrier == null) {
                log.error("Error Processing Mitigation cell with fdn '{}' for inter & intra frequency selection, "
                        + "Cannot find NrCellDU NrSectorCarrier information for {}", fdn, nrCellDU.toFdn());
                return;
            }
            int victimCellArfcnDL = nRSectorCarrier.getArfcnDL();
            int victimCellBSChannelBwDL = nRSectorCarrier.getBSChannelBwDL();

            //Get Map with fdn that are within the HO CDF threshold
            Map<String, HandOvers> hoMap = mitigationCell.getCuFdnToHandovers();
            hoMap.forEach((key, value) -> log.trace("HO MAP: fdn {}, # HO = {}", key, value));
            mitigationCell.getNeighborNrCell().forEach((neighborNrCellCu, neighborNRCellDU) -> filterAndAddNeighbors(mitigationCell, victimCellArfcnDL, victimCellBSChannelBwDL, hoMap, neighborNrCellCu, neighborNRCellDU));
        });
        if(numberCannotFilterAndAddNoNrSectorCarrierInfo.get() > 0) {
            log.error("Error Processing Mitigation cell for inter & intra frequency selection, "
                + "Cannot find NrCellDU or NrSectorCarrier information for {} Neighbor Cells of {} victims",numberCannotFilterAndAddNoNrSectorCarrierInfo.get(), ftRopNRCellDUCellsForMitigationList.size());
        }
        numVictimCellsSelectedForMitigation.increment(ftRopNRCellDUCellsForMitigationList.size());
        numNeighborCellsIntraFrequency.increment(numberIntra.get());
        numNeighborCellsInterFrequencyBwCompliant.increment(numberCompliantInter.get());
        numNeighborCellsDroppedInterFrequencyNonBwCompliant.increment((double) numberInter.get() - (double) numberCompliantInter.get());

        log.info("Processed {} Mitigation cells, found {} Intra Frequency cell, {} BW Compliant Inter frequency cell, "
                        + "dropped {} non BW compliant Inter Frequency cell, "
                        + "dropped {} neighnbor cells where there was no handover between victim and neighbor",
                ftRopNRCellDUCellsForMitigationList.size(), numberIntra.get(), numberCompliantInter.get(),
                (numberInter.get() - numberCompliantInter.get()), numberNoHandovers.get());
    }

    private void filterAndAddNeighbors(FtRopNRCellDU mitigationCell,
                                       int victimCellArfcnDL,
                                       int victimCellBSChannelBwDL,
                                       Map<String, HandOvers> hoMap,
                                       NRCellCU neighborNrCellCu,
                                       NRCellDU neighborNRCellDU) {
        String fdn = mitigationCell.getMoRopId().getFdn();
        if (!hoMap.containsKey(neighborNrCellCu.toFdn())) {
            log.trace("Cannot Process Neighbor for Mitigation cell with fdn '{}', for inter & intra frequency selection, "
                    + "No Handovers to NrCellCu Neighbor with fdn {}", fdn, neighborNrCellCu.toFdn());
            numberNoHandovers.getAndIncrement();
            return;
        }
        numberHandoverNonZero.getAndIncrement();
        //NRSectorCarrier.bSChannelBwDL & List<NRSectorCarrier>
        // Presumes one DU per CU
        // TODO: What about Multiple DU per CU??

        if (neighborNRCellDU == null || neighborNRCellDU.getNRSectorCarriers().isEmpty()
                || neighborNRCellDU.getNRSectorCarriers().get(0) == null) {
            log.debug("Error Processing Mitigation cell with fdn '{}' for inter & intra frequency selection, "
                            + "Cannot find NrCellDU or NrSectorCarrier information for Neighbor Cell {}",
                    fdn, neighborNRCellDU == null ? "null" : neighborNRCellDU.toFdn());
            numberCannotFilterAndAddNoNrSectorCarrierInfo.getAndIncrement();
            return;
        }
        doPrint(neighborNrCellCu, neighborNRCellDU, victimCellBSChannelBwDL);

        int neighborCellArfcnDL = neighborNRCellDU.getNRSectorCarriers().get(0).getArfcnDL();

        if (victimCellArfcnDL != neighborCellArfcnDL) {
            numberInter.getAndIncrement();
            log.trace("Processing (INTER) Neighbor cell with fdn '{}'", neighborNRCellDU.toFdn());

            // number DU bandwidth compliant
            if (neighborNRCellDU.getNRSectorCarriers().get(0).getBSChannelBwDL() >= cellSelectionConfig.getCioRejectVictimBandwidthRatio() * victimCellBSChannelBwDL) {
                mitigationCell.getInterFneighborNrCellCu().add(neighborNrCellCu);
                numberCompliantInter.getAndIncrement();
                log.trace("Added Neighbor cell with fdn '{}'as good INTER frequency Cell.", neighborNRCellDU.toFdn());
            }
        } else {
            mitigationCell.getIntraFneighborNrCellCu().add(neighborNrCellCu);
            log.trace("Added Neighbor cell with fdn '{}'as good INTRA frequency Cell.", neighborNRCellDU.toFdn());
            numberIntra.getAndIncrement();
        }
    }

    private void doPrint(NRCellCU neighborNrCellCu, NRCellDU neighborNRCellDU, int victimCellBSChannelBwDL) {
        if (log.isTraceEnabled() && neighborNRCellDU.getObjectId().getMeFdn().equals(neighborNrCellCu.getObjectId().getMeFdn())) {
            log.trace("Information for 'neighbor' cell with fdn, {}", neighborNrCellCu.toFdn());
            log.trace("FDN {} : {}", neighborNrCellCu.getObjectId().getMeFdn(), neighborNRCellDU.getObjectId().getMeFdn());
            log.trace("NCI {} : {}", neighborNRCellDU.getNCI(), neighborNRCellDU.getNCI());
            log.trace("BW  {} : {}", neighborNRCellDU.getNRSectorCarriers().get(0).getBSChannelBwDL(), victimCellBSChannelBwDL);
            log.trace("\n\n\n");
        }
    }

    @Override
    public int getPriority() {
        return 164;
    }
}
