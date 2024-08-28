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
package com.ericsson.oss.apps.data.collection.features;

import com.ericsson.oss.apps.data.collection.HandOvers;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.features.handlers.InterferenceType;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvRecurse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.data.collection.features.handlers.InterferenceType.NOT_DETECTED;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class FtRopNRCellDU {

    @CsvRecurse
    MoRopId moRopId;
    @CsvBindByName
    private double dlRBSymUtil = Double.NaN;
    @CsvBindByName
    private double pmRadioMaxDeltaIpNAvg = Double.NaN;
    @CsvBindByName
    private Long connectedComponentId;
    @CsvBindByName
    private Double victimScore;
    @CsvBindByName
    private double avgUlUe = Double.NaN;
    @CsvBindByName
    private double avgSw2UlUeThroughput = Double.NaN;
    @CsvBindByName
    private double avgSw8UlUeThroughput = Double.NaN;
    @CsvBindByName
    private double avgSw8AvgDeltaIpN = Double.NaN;
    @CsvBindByName
    private double avgSw8AvgSymbolDeltaIpN = Double.NaN;
    @CsvBindByName
    private double avgSw8PercPositiveSymbolDeltaIpNSamples = Double.NaN;
    @CsvBindByName
    private double ueTpBaseline = Double.NaN;
    @CsvBindByName
    private double pmMacVolUl = Double.NaN;
    @CsvBindByName
    private double pmMacVolDl = Double.NaN;
    @CsvBindByName
    private double pmMacTimeUlResUe = Double.NaN;
    @CsvBindByName
    private double pmMacVolUlResUe = Double.NaN;
    @CsvBindByName
    private double refractiveIndex = Double.NaN;
    @CsvBindByName
    private byte nRopsInLastSeenWindow = 0;
    @CsvBindByName
    private double avgSymbolDeltaIpn = Double.NaN;
    @CsvBindByName
    private double totalBinSumSymbolDeltaIpn = Double.NaN;
    @CsvBindByName
    private double totalBinSumMaxDeltaIpn = Double.NaN;
    @CsvBindByName
    private double positiveBinSumSymbolDeltaIpn = Double.NaN;
    @CsvBindByName
    private InterferenceType interferenceType = NOT_DETECTED;

    private int ropCount;
    // Master Lists of Neighbors, If this is victim Cell.
    private Map<NRCellRelation, NRCellCU> cellRelationMap = new ConcurrentHashMap<>();
    private List<String> neighborFtRopNRCellDUFdns = new ArrayList<>();
    private Map<NRCellCU, NRCellDU> neighborNrCell = new ConcurrentHashMap<>();
    private Map<String, HandOvers> cuFdnToHandovers = new LinkedHashMap<>();


    // List of Intra Frequency Neighbors for this victim cell.
    private Set<NRCellCU> intraFneighborNrCellCu = new HashSet<>();
    // List of Inter Frequency Neighbors ( BW compliant) for this victim cell.
    // Use 'IntraFneighborNrCellCu' && 'InterFneighborNrCellCu' for CIO Mitigation.
    private Set<NRCellCU> interFneighborNrCellCu = new HashSet<>(); //with BW >= victim Cell

    // List of KPO Intra Frequency Neighbors for this victim cell. These are the neighbors for P0 Mitigation.
    private List<NRCellCU> kp0IntraFneighborNrCellCu = new ArrayList<>();

    // List of CIO Intra & Inter Frequency Neighbors for this victim cell. These are the neighbors for CIO Mitigation.
    private List<NRCellCU> kcioNeighborNrCellCu = new ArrayList<>();

    public FtRopNRCellDU(MoRopId moRopId) {
        this.moRopId = moRopId;
    }

    /**
     * Clear out maps and list (for test).
     */
    public void clear() {
        interFneighborNrCellCu.clear();
        intraFneighborNrCellCu.clear();
    }

    public List<String> getKp0IntraFreqNeighborNrCellDuFdns() {
        return getNeighborNrCellDuFdns(kp0IntraFneighborNrCellCu);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FtRopNRCellDU that = (FtRopNRCellDU) o;

        return Objects.equals(moRopId, that.moRopId);
    }

    @Override
    public int hashCode() {
        return moRopId != null ? moRopId.hashCode() : 0;
    }

    private List<String> getNeighborNrCellDuFdns(Collection<NRCellCU> nrCellCuList) {
        List<String> neighborNeCellCuFdns = nrCellCuList.stream().map(nRCellCU -> nRCellCU.getObjectId().toFdn()).collect(Collectors.toList());
        return neighborNrCell.entrySet()
                .stream()
                .filter(entry -> neighborNeCellCuFdns.contains(entry.getKey().getObjectId().toFdn()))
                .map(entry -> entry.getValue().getObjectId().toFdn())
                .collect(Collectors.toList());
    }

    public boolean isRemoteInterference() {
        return (connectedComponentId != null && !interferenceType.equals(InterferenceType.OTHER))
                || interferenceType.equals(InterferenceType.MIXED) || interferenceType.equals(InterferenceType.REMOTE);
    }

}
