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
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.data.collection.features.handlers.geospatial.GeoUtils.getRefractiveIndex;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app", name = "local", havingValue = "true")
@Slf4j
public class RefractiveIndexCalculator implements FeatureHandler<FeatureContext> {

    private FeatureContext featureContext;
    private final CtsClient ctsClient;

    @NotNull
    private List<CellAndGeoInfo> getGeoDataForCells(Collection<FtRopNRCellDU> ftRopNRCellDUList) {
        return ftRopNRCellDUList.parallelStream().map(ftRopNRCellDU -> ftRopNRCellDU.getMoRopId().getFdn())
                .peek(fdn -> log.debug("Extracting geo location information from CTS for cell {}", fdn))
                .flatMap(fdn -> ctsClient.getNrCellGeoData(fdn).stream())
                .filter(geoData -> geoData.getCoordinate() != null)
                .map(geoData -> new CellAndGeoInfo(geoData.getCoordinate(), geoData.getFdn()))
                .collect(Collectors.toList());
    }

    @Override
    public void handle(FeatureContext context) {
        this.featureContext = context;
        List<FtRopNRCellDU> ftRopNRCellDUList = new ArrayList<>(featureContext.getFdnToFtRopNRCellDU().values());
        GridCoverage2D gridCoverage2D = featureContext.getLatestCoverage();
        if (gridCoverage2D == null) {
            log.error("No weather data available, cannot calculate refractive index for rop {}", featureContext.getRopTimeStamp());
            return;
        }
        getGeoDataForCells(ftRopNRCellDUList).parallelStream()
                .forEach(cellAndGeoInfo -> {
                    float refractiveIndex = getRefractiveIndex(gridCoverage2D, cellAndGeoInfo.getCoordinate());
                    featureContext.getFdnToFtRopNRCellDU().get(cellAndGeoInfo.getFdn()).setRefractiveIndex(refractiveIndex);
                });
    }

    @Override
    public int getPriority() {
        return 11;
    }

}
