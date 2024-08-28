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
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.utils.LockByKey;
import com.ericsson.oss.apps.utils.Utils;
import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.Counter;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DuctStrengthSelector implements FeatureHandler<FeatureContext> {

    private final CtsClient ctsClient;
    private final NcmpClient ncmpClient;
    private final DuctDetectionConfig ductDetectionConfig;
    private final ThreadingConfig threadingConfig;
    private final Counter numCellsAboveMinimumDeltaIPN;
    private final Counter numCellsWithinDuctingConditions;
    private final Counter numCellPairsValid;
    private final Counter numCellPairsOverMinDucting;
    private FeatureContext featureContext;
    private LockByKey<String> lockByKey;

    public List<FtRopNRCellDUPair> calculateDuctStrength(long ropTimestamp) {

        List<FtRopNRCellDU> ftRopNRCellDUList = getCellsAboveDeltaInterferenceLevel();
        log.info("Cells {} detected above minimum average deltaipn level {}", ftRopNRCellDUList.size(), ductDetectionConfig.getMinAvgDeltaIpn());
        if (ftRopNRCellDUList.isEmpty()) {
            return Collections.emptyList();
        }
        ftRopNRCellDUList = cacheAndFilterCMData(ftRopNRCellDUList);

        log.info("Extracted and caching cell CM data from NCMP for {} cells ", ftRopNRCellDUList.size());

        GridCoverage2D gridCoverage2D = featureContext.getLatestCoverage();

        if (ftRopNRCellDUList.isEmpty() || gridCoverage2D == null) {
            log.error("No weather data available, no aggressor inference attempted ROP {}", featureContext.getRopTimeStamp());
            return Collections.emptyList();
        }

        List<CellAndGeoInfo> interferenceCells = getGeoInformationForInterferenceCells(ftRopNRCellDUList, gridCoverage2D);
        log.info("Cells {} detected with geo and configuration information, prioritized on deltaipn up to {} cells", interferenceCells.size(), ductDetectionConfig.getMaxDetectedCells());
        if (interferenceCells.isEmpty()) {
            return Collections.emptyList();
        }

        DuctStrengthCalculator ductStrengthCalculator = new DuctStrengthCalculator(gridCoverage2D, ductDetectionConfig);

        Map<Integer, Set<String>> bandToFdnMap = createBandToFdnMap(interferenceCells);
        Stream<DuctCellPair> filteredCellPairs = filterCellPairsOnGuardDistanceAndBand(interferenceCells, bandToFdnMap);
        List<FtRopNRCellDUPair> ftRopNRCellDUPairList = calculateDuctStrengthForCellPairs(ropTimestamp, filteredCellPairs, ductStrengthCalculator);

        log.info("Cells pairs {} over minimum ducting strength {}", ftRopNRCellDUPairList.size(), ductDetectionConfig.getMinDetectedDuctStrength());
        numCellPairsOverMinDucting.increment(ftRopNRCellDUPairList.size());
        return ftRopNRCellDUPairList;
    }

    private void setBandListAndGuardDistance(CellAndGeoInfo cellAndGeoInfo) {
        NRCellDU nrCellDU = featureContext.getFdnToNRCellDUMap().get(cellAndGeoInfo.getFdn());
        List<Integer> bandList = (nrCellDU.getBandList() == null || nrCellDU.getBandList().isEmpty()) ? nrCellDU.getBandListManual() : nrCellDU.getBandList();
        cellAndGeoInfo.setBandList(bandList);
        cellAndGeoInfo.setGuardDistance(DistanceCalculator.calculateGuardDistance(nrCellDU));
    }

    @NotNull
    Stream<DuctCellPair> filterCellPairsOnGuardDistanceAndBand(List<CellAndGeoInfo> interferenceCells, Map<Integer, Set<String>> bandToFdnMap) {
        // loop over cell pairs
        return interferenceCells.stream().flatMap(cell1 -> interferenceCells.parallelStream()
                .map(cell2 -> DuctCellPair.builder()
                        .cellAndGeoInfo1(cell1)
                        .cellAndGeoInfo2(cell2)
                        .distance(DistanceCalculator.haversine(cell1.getCoordinate(), cell2.getCoordinate()))
                        .build())
                // filter out the pairs under TDD guard distance
                .filter(ductCellPair -> ductCellPair.getDistance() >= ductCellPair.getCellAndGeoInfo1().getGuardDistance())
                // this is very crude, checking if the two cells are on the same band
                // not checking overlapping bands (will work for the demo data set, but may not for others)
                .filter(ductCellPair -> checkIfCellsAreOnSameBand(bandToFdnMap, ductCellPair.getCellAndGeoInfo1().getFdn(), ductCellPair.getCellAndGeoInfo2().getFdn()))
        ).peek(ductCellPair -> numCellPairsValid.increment());
    }

    @NotNull
    @VisibleForTesting
    List<FtRopNRCellDUPair> calculateDuctStrengthForCellPairs(long ropTimestamp, Stream<DuctCellPair> ductCellPairList,
                                                              DuctStrengthCalculator ductStrengthCalculator) {
        Map<GeoPair, Double> geoPairToDuctStrengthMap = new ConcurrentHashMap<>();
        return ductCellPairList.parallel().peek(ductCellPair -> {
                            GeoPair geoPair = new GeoPair(ductCellPair.cellAndGeoInfo1.getCoordinate(), ductCellPair.cellAndGeoInfo2.getCoordinate());
                            double ductStrength = geoPairToDuctStrengthMap.computeIfAbsent(geoPair, x -> ductStrengthCalculator.getDuctStrength(
                                    ductCellPair.getCellAndGeoInfo1().getCoordinate(),
                                    ductCellPair.getCellAndGeoInfo2().getCoordinate(),
                                    Math.min(ductCellPair.getCellAndGeoInfo1().getRefractiveIndex(), ductCellPair.getCellAndGeoInfo2().getRefractiveIndex())).orElse(0D)
                            );
                            ductCellPair.setDuctStrength(ductStrength);
                        }
                )
                .filter(ductCellPair -> ductCellPair.getDuctStrength() >= ductDetectionConfig.getMinDetectedDuctStrength())
                .map(ductCellPair -> getCellPairFT(ductCellPair, ropTimestamp))
                .toList();
    }

    private boolean checkIfCellsAreOnSameBand(Map<Integer, Set<String>> bandToFdnMap, String fdn1, String fdn2) {
        return bandToFdnMap.entrySet().stream().anyMatch(entry -> (entry.getValue().contains(fdn1) && entry.getValue().contains(fdn2)));
    }

    private FtRopNRCellDUPair getCellPairFT(DuctCellPair ductCellPair, long ropTimestamp) {
        FtRopNRCellDUPair ftRopNRCellPair = new FtRopNRCellDUPair();
        ftRopNRCellPair.setFdn1(ductCellPair.getCellAndGeoInfo1().getFdn().intern());
        ftRopNRCellPair.setFdn2(ductCellPair.getCellAndGeoInfo2().getFdn().intern());
        ftRopNRCellPair.setRopTime(ropTimestamp);
        ftRopNRCellPair.setDistance(ductCellPair.getDistance());
        ftRopNRCellPair.setDuctStrength(ductCellPair.getDuctStrength());
        double guardDistance = ductCellPair.getCellAndGeoInfo1().getGuardDistance();
        ftRopNRCellPair.setGuardDistance(guardDistance);
        if (ductCellPair.getDistance() > 0) {
            ftRopNRCellPair.setGuardOverDistance(guardDistance / ductCellPair.getDistance());
        }
        return ftRopNRCellPair;
    }

    @VisibleForTesting
    List<FtRopNRCellDU> getCellsAboveDeltaInterferenceLevel() {
        List<FtRopNRCellDU> ftRopNRCellDUList = featureContext.getFdnToFtRopNRCellDU().values().parallelStream()
                .filter(ftRopNRCellDU -> !Double.isNaN(ftRopNRCellDU.getAvgSw8AvgDeltaIpN()))
                .filter(ftRopNRCellDU -> (ftRopNRCellDU.getAvgSw8AvgDeltaIpN() > ductDetectionConfig.getMinAvgDeltaIpn()))
                .toList();
        log.info("Cells {} detected above minimum average deltaipn level {}", ftRopNRCellDUList.size(), ductDetectionConfig.getMinAvgDeltaIpn());
        numCellsAboveMinimumDeltaIPN.increment(ftRopNRCellDUList.size());
        return ftRopNRCellDUList;
    }

    @NotNull
    @VisibleForTesting
    List<CellAndGeoInfo> getGeoDataForCells(Collection<FtRopNRCellDU> ftRopNRCellDUList, Object ignored) {
        return ftRopNRCellDUList.parallelStream().map(ftRopNRCellDU -> ftRopNRCellDU.getMoRopId().getFdn())
                .peek(fdn -> log.debug("Extracting geo location information from CTS for cell {}", fdn))
                .flatMap(fdn -> ctsClient.getNrCellGeoData(fdn).stream())
                .filter(geoData -> geoData.getCoordinate() != null)
                .peek(geoData -> featureContext.getFdnToGeoDataMap().put(geoData.getFdn(), geoData))
                .map(geoData -> new CellAndGeoInfo(geoData.getCoordinate(), geoData.getFdn()))
                .toList();
    }

    @VisibleForTesting
    Map<Integer, Set<String>> createBandToFdnMap(List<CellAndGeoInfo> interferenceCells) {
        // group by band
        Map<Integer, Set<String>> bandToFdnMap = new HashMap<>();
        interferenceCells.forEach(cellAndGeoInfo -> cellAndGeoInfo.getBandList().forEach(band -> {
            bandToFdnMap.putIfAbsent(band, new HashSet<>());
            bandToFdnMap.get(band).add(cellAndGeoInfo.getFdn());
        }));
        log.info("{} bands found", bandToFdnMap.size());
        return bandToFdnMap;
    }

    @VisibleForTesting
    List<CellAndGeoInfo> getGeoInformationForInterferenceCells(List<FtRopNRCellDU> ftRopNRCellDUList, GridCoverage2D gridCoverage2D) {
        log.info("Extracting geo location information from CTS for {} cells ", ftRopNRCellDUList.size());
        Set<CellAndGeoInfo> geoDataForCellsSet = new HashSet<>(Utils.of().processInThreadPool(ftRopNRCellDUList, threadingConfig.getPoolSizeForCtsGeoQuery(), this::getGeoDataForCells, null));

        log.info("Filtering cell data based on refractive index for {} cells ", geoDataForCellsSet.size());
        List<CellAndGeoInfo> cellAndGeoInfoList = filterOnMinRefractiveIndex(gridCoverage2D, geoDataForCellsSet);

        log.info("Populating band and guard information for {} cells ", cellAndGeoInfoList.size());
        cellAndGeoInfoList = setAndFilterBandAndGuard(cellAndGeoInfoList);

        log.info("Ordering and filtering {} cells based on interference level", cellAndGeoInfoList.size());
        return cellAndGeoInfoList.stream()
                .map(cellAndGeoInfo -> Pair.of(featureContext.getFdnToFtRopNRCellDU().get(cellAndGeoInfo.getFdn()).getAvgSw8AvgDeltaIpN(), cellAndGeoInfo))
                .sorted((a, b) -> Double.compare(b.getFirst(), a.getFirst()))
                .limit(ductDetectionConfig.getMaxDetectedCells())
                .map(Pair::getSecond)
                .toList();
    }

    @NotNull
    private List<CellAndGeoInfo> filterOnMinRefractiveIndex(GridCoverage2D gridCoverage2D, Set<CellAndGeoInfo> geoDataForCellsSet) {
        return geoDataForCellsSet.parallelStream()
                .peek(cellAndGeoMoRopId -> cellAndGeoMoRopId.setRefractiveIndex(GeoUtils.getRefractiveIndex(gridCoverage2D, cellAndGeoMoRopId.getCoordinate())))
                .filter(cellAndGeoMoRopId -> cellAndGeoMoRopId.getRefractiveIndex() >= ductDetectionConfig.getMinDetectedDuctStrength())
                .peek(cellAndGeoInfo -> numCellsWithinDuctingConditions.increment()).toList();
    }

    @NotNull
    private List<CellAndGeoInfo> setAndFilterBandAndGuard(Collection<CellAndGeoInfo> geoDataForCells) {
        return geoDataForCells.parallelStream()
                .peek(this::setBandListAndGuardDistance)
                .filter(cellAndGeoInfo -> cellAndGeoInfo.getBandList() != null && !cellAndGeoInfo.getBandList().isEmpty())
                .toList();
    }

    @NotNull
    private List<FtRopNRCellDU> cacheAndFilterCMData(Collection<FtRopNRCellDU> ftRopNRCellDUs) {
        log.info("Extracting and caching cell CM data from NCMP for {} cells ", ftRopNRCellDUs.size());
        var fdnToNRCellDUMap = featureContext.getFdnToNRCellDUMap();
        List<FdnNRCEllDU> extractedFdnNRCellDU = Utils.of().processInThreadPool(ftRopNRCellDUs, threadingConfig.getPoolSizeForNcmpCmQuery(), this::extractCellCMData, null);
        extractedFdnNRCellDU.forEach(fdnNRCEllDU -> fdnToNRCellDUMap.put(fdnNRCEllDU.fdn, fdnNRCEllDU.nrCellDU));
        // no point passing cells that have no CM info to the rest of the algorithm
        return ftRopNRCellDUs.stream().filter(ftRopNRCellDU -> fdnToNRCellDUMap.containsKey(ftRopNRCellDU.getMoRopId().getFdn())).toList();
    }

    @NotNull
    private List<FdnNRCEllDU> extractCellCMData(Collection<FtRopNRCellDU> ftRopNRCellDUS, Object ignored) {
        return ftRopNRCellDUS.parallelStream()
                .flatMap(ftRopNRCellDU -> Optional.ofNullable(ftRopNRCellDU.getMoRopId().getFdn()).stream())
                .flatMap(ftRopNRCellDUfdn -> extractCellCMDataForCell(ManagedObjectId.of(ftRopNRCellDUfdn)).stream())
                .map(nrCellDU -> new FdnNRCEllDU(nrCellDU.toFdn(), nrCellDU))
                .toList();
    }

    private Optional<NRCellDU> extractCellCMDataForCell(ManagedObjectId managedObjectId) {
        try {
            lockByKey.lock(managedObjectId.getMeFdn());
            return ncmpClient.getCmResource(managedObjectId, NRCellDU.class);
        } finally {
            lockByKey.unlock(managedObjectId.getMeFdn());
        }
    }

    @Override
    public void handle(FeatureContext context) {
        this.featureContext = context;
        lockByKey = new LockByKey<>();
        List<FtRopNRCellDUPair> ftRopNRCellDUPairList = calculateDuctStrength(context.getRopTimeStamp());
        context.setFtRopNRCellDUPairs(ftRopNRCellDUPairList);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Getter
    @Builder
    private static class DuctCellPair {
        private CellAndGeoInfo cellAndGeoInfo1;
        private CellAndGeoInfo cellAndGeoInfo2;

        @Setter
        private double distance;
        @Setter
        private double ductStrength;
    }


    @EqualsAndHashCode
    private static class GeoPair {
        Coordinate coord1;
        Coordinate coord2;

        GeoPair(Coordinate coord1, Coordinate coord2) {
            if (coord1.compareTo(coord2) > 0) {
                this.coord1 = coord1;
                this.coord2 = coord2;
            } else {
                this.coord2 = coord1;
                this.coord1 = coord2;
            }
        }
    }

    private record FdnNRCEllDU(String fdn, NRCellDU nrCellDU) {
    }

}