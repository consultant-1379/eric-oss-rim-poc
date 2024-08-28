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
package com.ericsson.oss.apps.classification;

import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineNRCellDU;
import com.ericsson.oss.apps.repositories.PmBaselineNrCellDuRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CellMitigationService {

    private final PmBaselineNrCellDuRepo pmBaselineNrCellDuRepo;

    public Set<String> getCellsAboveDeltaIpnThresholdAndBelowUETPBaselineFdns(double deltaIpThreshold, Collection<FtRopNRCellDU> ftRopNRCellDUS) {
        return ftRopNRCellDUS.stream()
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getAvgSw8AvgDeltaIpN() > deltaIpThreshold)
                .filter(ftRopNRCellDU -> !Double.isNaN(ftRopNRCellDU.getUeTpBaseline()))
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getAvgSw2UlUeThroughput() < ftRopNRCellDU.getUeTpBaseline())
                .map(ftRopNRCellDU -> ftRopNRCellDU.getMoRopId().getFdn())
                .collect(Collectors.toUnmodifiableSet());
    }


    public Map<String, Double> getFdnToUetpMap() {
        return pmBaselineNrCellDuRepo.findByUplInkThroughputQuartile50IsNotNull().stream()
                .collect(Collectors.toMap(PmBaselineNRCellDU::getFdn, PmBaselineNRCellDU::getUplInkThroughputQuartile50));
    }

}
