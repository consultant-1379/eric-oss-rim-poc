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


package com.ericsson.oss.apps.data.collection.features.handlers;

import com.ericsson.oss.apps.classification.CellMitigationService;
import com.ericsson.oss.apps.config.RopProcessingConfig;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.aggregation.PmSWNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.aggregation.SlidingWindowAggregationService;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("kpiCalculator")
@Slf4j
@RequiredArgsConstructor
public class KpiCalculator implements FeatureHandler<FeatureContext> {

    private final PmRopNrCellDuRepo pmRopNrCellDuRepo;
    private final Counter numKpiRopRecordsProcessed;
    private final CellMitigationService cellMitigationService;
    private final SlidingWindowAggregationService slidingWindowAggregationService;
    private final RopProcessingConfig ropProcessingConfig;

    private void calculateKPIs(FeatureContext featureContext) {
        log.info("Calculating KPIs");
        long ropTimeStamp = featureContext.getRopTimeStamp();
        Map<String, Double> fdnToPmBaselineMap = cellMitigationService.getFdnToUetpMap();
        List<PmSWNRCellDU> fdnToSlidingWindowCountersMap = slidingWindowAggregationService
                .calculateSlidingWindowCounters(ropTimeStamp);
        Map<String, PmRopNRCellDU> fdnToPmRopNRCellDUMap = pmRopNrCellDuRepo.findByMoRopId_RopTime(featureContext.getRopTimeStamp()).stream()
                .collect(Collectors.toMap(pmRopNRCellDU -> pmRopNRCellDU.getMoRopId().getFdn(), Function.identity()));

        fdnToSlidingWindowCountersMap.parallelStream().forEach(pmSWNRCellDU -> {
            MoRopId moRopId = new MoRopId(pmSWNRCellDU.getFdn(), ropTimeStamp);
            PmRopNRCellDU pmRopNRCellDU = fdnToPmRopNRCellDUMap.getOrDefault(pmSWNRCellDU.getFdn(), new PmRopNRCellDU());
            FtRopNRCellDU ftRopNRCellDU = new FtRopNRCellDU(moRopId);
            // fill various KPIs
            ftRopNRCellDU.setPmRadioMaxDeltaIpNAvg(getNaForNull(pmRopNRCellDU.getAvgDeltaIpN()));
            ftRopNRCellDU.setDlRBSymUtil(calcDLRBSymUtil(pmRopNRCellDU));
            ftRopNRCellDU.setPmMacVolDl(getNaForNull(pmRopNRCellDU.getPmMacVolDl()));
            ftRopNRCellDU.setPmMacVolUl(pmRopNRCellDU.getPmMacVolUl() == null ? Double.NaN : pmRopNRCellDU.getPmMacVolUl());
            ftRopNRCellDU.setAvgUlUe(getNaForNull(pmRopNRCellDU.getAvgUlUeTp()));
            ftRopNRCellDU.setPmMacVolUlResUe(getNaForNull(pmRopNRCellDU.getPmMacVolUlResUe()));
            ftRopNRCellDU.setPmMacTimeUlResUe(getNaForNull(pmRopNRCellDU.getPmMacTimeUlResUe()));
            ftRopNRCellDU.setAvgSymbolDeltaIpn(getNaForNull(pmRopNRCellDU.getAvgSymbolDeltaIpn()));
            ftRopNRCellDU.setTotalBinSumSymbolDeltaIpn(getNaForNull(pmRopNRCellDU.getTotalBinSumSymbolDeltaIpn()));
            ftRopNRCellDU.setTotalBinSumMaxDeltaIpn(getNaForNull(pmRopNRCellDU.getTotalBinSumMaxDeltaIpn()));
            ftRopNRCellDU.setPositiveBinSumSymbolDeltaIpn(getNaForNull(pmRopNRCellDU.getPositiveBinSumSymbolDeltaIpn()));

            setNRCellDUPrecalculatedCounters(pmSWNRCellDU, ftRopNRCellDU);
            setInterferenceType(ftRopNRCellDU);
            ftRopNRCellDU.setUeTpBaseline(fdnToPmBaselineMap.getOrDefault(moRopId.getFdn(), Double.NaN));
            numKpiRopRecordsProcessed.increment();
            featureContext.getFdnToFtRopNRCellDU().put(ftRopNRCellDU.getMoRopId().getFdn(), ftRopNRCellDU);
        });
    }

    private double getNaForNull(Number counter) {
        return counter == null ? Double.NaN : counter.doubleValue();
    }

    private void setNRCellDUPrecalculatedCounters(PmSWNRCellDU pmSWNRCellDU, FtRopNRCellDU ftRopNRCellDU) {
        ftRopNRCellDU.setAvgSw2UlUeThroughput(pmSWNRCellDU.getAvgSw2UlUeThroughput());
        ftRopNRCellDU.setAvgSw8UlUeThroughput(pmSWNRCellDU.getAvgSw8UlUeThroughput());
        ftRopNRCellDU.setAvgSw8AvgSymbolDeltaIpN(pmSWNRCellDU.getAvgSw8AvgSymbolDeltaIpN() * ropProcessingConfig.getPmSWAvgSymbolDeltaIPNScalingFactor());
        ftRopNRCellDU.setAvgSw8PercPositiveSymbolDeltaIpNSamples(pmSWNRCellDU.getAvgSw8PercPositiveSymbolDeltaIpNSamples());
        ftRopNRCellDU.setNRopsInLastSeenWindow(pmSWNRCellDU.getNRopsInLastSeenWindow());
        ftRopNRCellDU.setRopCount(pmSWNRCellDU.getRopCount());
        ftRopNRCellDU.setAvgSw8AvgDeltaIpN(pmSWNRCellDU.getAvgSw8AvgDeltaIpN());
    }

    private void setInterferenceType(FtRopNRCellDU ftRopNRCellDU) {
        if (Double.isNaN(ftRopNRCellDU.getAvgSw8PercPositiveSymbolDeltaIpNSamples())) {
            return;
        }
        if (ftRopNRCellDU.getRopCount() < ropProcessingConfig.getMinRopCountForDetection()) {
            ftRopNRCellDU.setInterferenceType(InterferenceType.NOT_DETECTED);
            return;
        }
        if (ftRopNRCellDU.getAvgSw8PercPositiveSymbolDeltaIpNSamples() < ropProcessingConfig.getOtherInterferencePercThreshold()) {
            ftRopNRCellDU.setInterferenceType(InterferenceType.OTHER);
            return;
        }
        if (ftRopNRCellDU.getAvgSw8PercPositiveSymbolDeltaIpNSamples() < ropProcessingConfig.getMixedInterferencePercThreshold()) {
            ftRopNRCellDU.setInterferenceType(InterferenceType.MIXED);
            return;
        }
        ftRopNRCellDU.setInterferenceType(InterferenceType.REMOTE);
    }

    /**
     * see <a href="https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PA/KPI+Formulas?preview=/510647506/510647449/image2022-7-8_17-58-52.png">kpis in confluence</a>
     *
     * @param pmRopNRCellDU input PM counters for NRCellDU
     * @return the KPI value, or an empty Optional if one of the PIs is null
     */
    @VisibleForTesting
    Double calcDLRBSymUtil(PmRopNRCellDU pmRopNRCellDU) {
        Long[] dividendArray = new Long[]{pmRopNRCellDU.getPmMacRBSymUsedPdcchTypeA(),
                pmRopNRCellDU.getPmMacRBSymUsedPdschTypeA(),
                pmRopNRCellDU.getPmMacRBSymUsedPdschTypeABroadcasting(),
                pmRopNRCellDU.getPmMacRBSymUsedPdcchTypeB(),
                pmRopNRCellDU.getPmMacRBSymCsiRs()};
        long dividend = Arrays.stream(dividendArray).filter(Objects::nonNull).reduce(Long::sum).orElse(0L);
        Long pmMacRBSymAvailDl = pmRopNRCellDU.getPmMacRBSymAvailDl();
        if (pmMacRBSymAvailDl != null && pmMacRBSymAvailDl > 0) {
            return (dividend) / pmMacRBSymAvailDl.doubleValue();
        } else {
            return Double.NaN;
        }
    }

    @Override
    @Timed
    public void handle(FeatureContext context) {
        calculateKPIs(context);
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
