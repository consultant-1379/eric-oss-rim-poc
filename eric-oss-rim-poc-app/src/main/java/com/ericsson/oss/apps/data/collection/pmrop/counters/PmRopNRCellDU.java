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

package com.ericsson.oss.apps.data.collection.pmrop.counters;

import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.pmrop.PreProcessToCleanupArrayElements;
import com.ericsson.oss.apps.data.collection.pmrop.PreProcessToCleanupValues;
import com.opencsv.bean.CsvBindAndSplitByName;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvRecurse;
import com.opencsv.bean.processor.PreAssignmentProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PmRopNRCellDU implements Serializable, PmRop {
    private static final List<Double> deltaIpnWeights = Arrays.asList(Math.pow(10, 0.000001D), Math.pow(10, 0.2D), Math.pow(10, 0.4D),
            Math.pow(10, 0.8D), Math.pow(10, 1.2D));

    private static final long serialVersionUID = 1180453496731680519L;

    @EmbeddedId
    @CsvRecurse
    private MoRopId moRopId;

    @CsvBindByName
    private Long pmMacRBSymUsedPdschTypeA;
    @CsvBindByName
    private Long pmMacRBSymUsedPdcchTypeA;
    @CsvBindByName
    private Long pmMacRBSymUsedPdcchTypeB;
    @CsvBindByName
    private Long pmMacRBSymUsedPdschTypeABroadcasting;
    @CsvBindByName
    private Long pmMacRBSymCsiRs;
    @CsvBindByName
    private Long pmMacRBSymAvailDl;
    @PreAssignmentProcessor(processor = PreProcessToCleanupValues.class, paramString = "-NaN")
    @CsvBindByName
    private Double pmMacVolUl;
    @PreAssignmentProcessor(processor = PreProcessToCleanupValues.class, paramString = "-NaN")
    @CsvBindByName
    private Double pmMacVolDl;
    @PreAssignmentProcessor(processor = PreProcessToCleanupValues.class, paramString = "-NaN")
    @CsvBindByName
    private Double pmMacVolUlResUe;
    @PreAssignmentProcessor(processor = PreProcessToCleanupValues.class, paramString = "-NaN")
    @CsvBindByName
    private Double pmMacTimeUlResUe;

    @CsvBindByName(column = "avg_delta")
    private Double avgDeltaIpNPreCalculated = Double.NaN;

    //Only required for the Sliding windows SQL Query. 'SlidingWindowAggregationService'
    @Getter(lombok.AccessLevel.NONE)
    private Double avgDeltaIpN = Double.NaN;
    @Setter(lombok.AccessLevel.NONE)
    private Double avgSymbolDeltaIpn = Double.NaN;
    @Setter(lombok.AccessLevel.NONE)
    private Double totalBinSumSymbolDeltaIpn = Double.NaN;
    @Setter(lombok.AccessLevel.NONE)
    private Double totalBinSumMaxDeltaIpn = Double.NaN;
    @Setter(lombok.AccessLevel.NONE)
    private Double positiveBinSumSymbolDeltaIpn = Double.NaN;

    private boolean usePreCalculatedInCsv;

    @PreAssignmentProcessor(processor = PreProcessToCleanupArrayElements.class, paramString = "5")
    @CsvBindAndSplitByName(elementType = Long.class)
    @Setter(lombok.AccessLevel.NONE)
    @Transient
    private ArrayList<Long> pmRadioMaxDeltaIpNDistr;

    @PreAssignmentProcessor(processor = PreProcessToCleanupArrayElements.class, paramString = "9")
    @CsvBindAndSplitByName(elementType = Long.class)
    @Setter(lombok.AccessLevel.NONE)
    @Transient
    private ArrayList<Long> pmRadioSymbolDeltaIpnDistr = new ArrayList<>();

    public PmRopNRCellDU(MoRopId moRopId) {
        this.moRopId = moRopId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PmRopNRCellDU otherPmRopNRCellDU = (PmRopNRCellDU) o;

        return this.moRopId.equals(otherPmRopNRCellDU.moRopId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moRopId);
    }

    @SuppressWarnings("squid:S1319")
    public void setPmRadioMaxDeltaIpNDistr(ArrayList<Long> pmRadioMaxDeltaIpNDistr) { //NOSONAR
        this.pmRadioMaxDeltaIpNDistr = pmRadioMaxDeltaIpNDistr != null ? new ArrayList<>(pmRadioMaxDeltaIpNDistr) : null;
        if (pmRadioMaxDeltaIpNDistr != null && pmRadioMaxDeltaIpNDistr.size() == 5) {
            avgDeltaIpN = getWeightedAverage(5, pmRadioMaxDeltaIpNDistr, "pmRadioMaxDeltaIpNDistr");
            totalBinSumMaxDeltaIpn = pmRadioMaxDeltaIpNDistr.stream().filter(Objects::nonNull).mapToDouble(x -> x).sum();
        } else {
            totalBinSumMaxDeltaIpn = Double.NaN;
            avgDeltaIpN = Double.NaN;
        }
    }

    @SuppressWarnings("squid:S1319")
    public void setPmRadioSymbolDeltaIpnDistr(ArrayList<Long> pmRadioSymbolDeltaIpnDistr) { //NOSONAR
        this.pmRadioSymbolDeltaIpnDistr = pmRadioSymbolDeltaIpnDistr != null ? new ArrayList<>(pmRadioSymbolDeltaIpnDistr) : null;
        if (pmRadioSymbolDeltaIpnDistr != null && pmRadioSymbolDeltaIpnDistr.size() == 9) {
            avgSymbolDeltaIpn = getWeightedAverage(9, pmRadioSymbolDeltaIpnDistr, "pmRadioSymbolDeltaIpnDistr");
            totalBinSumSymbolDeltaIpn = pmRadioSymbolDeltaIpnDistr.stream().filter(Objects::nonNull).mapToDouble(x -> x).sum();
            positiveBinSumSymbolDeltaIpn = pmRadioSymbolDeltaIpnDistr.subList(4, 9).stream().filter(Objects::nonNull).mapToDouble(x -> x).sum();
        }
        else {
            avgSymbolDeltaIpn = Double.NaN;
            totalBinSumSymbolDeltaIpn = Double.NaN;
            positiveBinSumSymbolDeltaIpn = Double.NaN;
        }
    }

    public Double getAvgDeltaIpN() {
        if (usePreCalculatedInCsv) {
            return avgDeltaIpNPreCalculated;
        }
        return avgDeltaIpN;
    }

    private Double getWeightedAverage(int requiredSize, List<Long> pmRadioIpNDistr, String counterName) {
        try {
            if (pmRadioIpNDistr == null) {
                log.trace("PmRopNRCellDU: Processing error: failed to calculate weighted average. {} is null " + ", PmRopNRCellDU = {}",
                        counterName, this);
                return Double.NaN;
            }
            int arraySize = pmRadioIpNDistr.size();
            if (arraySize != requiredSize) {
                log.debug("PmRopNRCellDU: Processing error: failed to calculate weighted average. {} contains {} elements, should be {}"
                                + ", PmRopNRCellDU = {}",
                        counterName, arraySize, requiredSize, this);
                return Double.NaN;
            }
            return getWeightedAverage(pmRadioIpNDistr.subList(arraySize - 5, arraySize));
        } catch (Exception exception) {
            log.error("PmRopNRCellDU: Processing error: failed to calculate weighted average from {} = {}, "
                    + "Exception {}, PmRopNRCellDU = {}", counterName, getValueIfNull(pmRadioIpNDistr), this, exception.getMessage(), exception);
            return Double.NaN;
        }
    }

    //@formatter:off
    /**
     * Calculates the weighted average in linear space of a distribution counter where the individual bins represent dBs
     * and converts back to dBs. It can be applied to the full array of pmRadioMaxDeltaIpNDistr or to the "positive" bins 4->8 of
     * pmRadioSymbolDeltaIpnDistr, indicative of remote interference.
     * Given that bins are stepped every second it provides an average estimate of power over the interval monitored.
     *
     * <p>
     *     avg_delta_ipn_db = avg_delta_linear/ delta_sum
     * <p>
     *     delta_sum = 'distribution_{i}' for i in range(5)]
     *     delta_sum = sum of the counters in the bins, should add to 900 (== ROP time in seconds) for pmRadioMaxDeltaIpNDistr
     *                 (since one of the bins is stepped every second of the ROP)
     * <p>
     *     Given ‘ipn_bins (db) = {0: 1e-5, 1: 2, 2: 4, 3: 8, 4: 12 }’,
     *     This is converted to linear power and weighted by the distribution of distribution_x as follows
     * <p>
     *         avg_delta_linear = distribution_x * 10 (to power of) (IPN_x(db)/10) ( where x = bin # 0-4, or 4-8)
     * <p>
     *     Then:
     *         avg_delta_ipn_db = 10*log10(avg_delta_linear / delta_sum)
     * <p>
     *
     * pmRadioMaxDeltaIpNDistr
     *     <p>
     *     <a href="https://cpistore.internal.ericsson.com/elex?ID=41196&fn=43_15554-LZA7016014_34-V1Uen.BY.html#pmRadioMaxDeltaIpNDistr">
     *     pmRadioMaxDeltaIpNDistr</a>
     *     <p>
     * <p>
     * pmRadioSymbolDeltaIpnDistr
     *     <p>
     *     <a href="https://cpistore.internal.ericsson.com/elex?ID=41196&fn=43_15554-LZA7016014_34-V1Uen.BY.html#pmRadioSymbolDeltaIpnDistr">
     *     pmRadioSymbolDeltaIpnDistr</a>
     *     <p>
     * @return Double representing average delta IpN
     */
    //@formatter:on
    private Double getWeightedAverage(List<Long> pmRadioIpNDistr) {
        double weightedSumIpnLinear = IntStream.range(0, pmRadioIpNDistr.size()).mapToDouble(i -> pmRadioIpNDistr.get(i) * deltaIpnWeights.get(i)).sum();
        double sumIpnBuckets = pmRadioIpNDistr.stream().mapToLong(aLong -> aLong).sum();
        return sumIpnBuckets > 0 ? (10 * Math.log10(weightedSumIpnLinear / sumIpnBuckets)) : Double.NaN;
    }

    //@formatter:off
    /**
     * Calculate the Average Ue Tp, based on the KPI formula
     * <p>
     *     Average UL MAC UE Throughput = 64 * (pmMacVolResUe/PmMacTimeUlResUe) kbps
     * <p>
     * The 64 comes from converting Bytes to bits ( 8 bits in a byte) and 125uS to Seconds (8*125 = 1000uS)
     * REF: <a href="https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?spaceKey=PA&title=KPI+Formulas&preview=/510647506/510647363/image2022-7-8_17-24-22.png">
     * Average UL MAC UE Throughput</a>
     * <p>
     *
     * pmMacVolUlResUe  Data volume in MAC SDUs successfully received in uplink as part of a burst while being restricted by air interface, excluding data volume received in last slot in burst. Unit: byte
     * pmMacTimeUlResUe  Transfer time of restricted uplink data volume. Unit: 125 microseconds
     * <p>
     * <p>
     * @return double representing the average UL UE TP
     */
  //@formatter:on
    public Double getAvgUlUeTp() {
        return (pmMacTimeUlResUe != null && pmMacVolUlResUe != null && !Double.isNaN(pmMacTimeUlResUe) && !Double.isNaN(pmMacVolUlResUe)
                && pmMacTimeUlResUe > 0) ? (64 * (pmMacVolUlResUe / pmMacTimeUlResUe))
                : Double.NaN;
    }

    private String getValueIfNull(Object value) {
        return value == null ? "noValue" : value.toString();
    }

}
