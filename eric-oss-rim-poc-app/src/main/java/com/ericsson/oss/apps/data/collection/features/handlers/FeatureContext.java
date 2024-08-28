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

import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.data.collection.features.report.mobility.MobilityReportingStatus;
import com.ericsson.oss.apps.data.collection.features.report.uplinkpower.UplinkPowerReportingStatus;
import com.ericsson.oss.apps.model.GeoData;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public class FeatureContext {

    private final long ropTimeStamp;
    private final Map<String, GeoData> fdnToGeoDataMap = new ConcurrentHashMap<>();
    private final Map<String, NRCellDU> fdnToNRCellDUMap = new ConcurrentHashMap<>();
    private final Map<String, FtRopNRCellDU> fdnToFtRopNRCellDU = new ConcurrentHashMap<>();

    @Setter
    private GridCoverage2D latestCoverage;

    @Setter
    private List<FtRopNRCellDUPair> ftRopNRCellDUPairs;
    @Setter
    private List<FtRopNRCellDU> ftRopNRCellDUCellsForMitigation;

    public NRCellDU getNrCellDU(String fdn) {
        return fdnToNRCellDUMap.get(fdn);
    }

    public Pair<FtRopNRCellDU, FtRopNRCellDU> getFtRopNRCellDUs(FtRopNRCellDUPair pair) {
        if (pair != null) {
            FtRopNRCellDU sourceCell = getFtRopNRCellDU(pair.getFdn1());
            FtRopNRCellDU targetCell = getFtRopNRCellDU(pair.getFdn2());
            if (sourceCell != null && targetCell != null) {
                return Pair.of(sourceCell, targetCell);
            }
        }
        return null;
    }

    public FtRopNRCellDU getFtRopNRCellDU(String fdn) {
        return fdnToFtRopNRCellDU.get(fdn);
    }

    private final List<UplinkPowerReportingStatus> uplinkPowerReportingStatusList = new ArrayList<>();

    private final List<MobilityReportingStatus> mobilityReportingStatusList = new ArrayList<>();
}
