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

import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.model.GeoData;
import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AzimuthAffinityCalculator implements FeatureHandler<FeatureContext> {
    private final BearingCalculator bearingCalculator;

    @Override
    @Timed
    public void handle(FeatureContext context) {
        Map<String, GeoData> fdnToGeoDataMap = context.getFdnToGeoDataMap();
        List<FtRopNRCellDUPair> nrCEllDUPairs = context.getFtRopNRCellDUPairs();
        log.info("calculating azimuth affinity for {} cell pairs and rop {}",
                nrCEllDUPairs.size(),
                context.getRopTimeStamp());
        nrCEllDUPairs.parallelStream()
                .forEach(ftRopNRCellDUPair -> {
                    GeoData geoData1 = fdnToGeoDataMap.get(ftRopNRCellDUPair.getFdn1());
                    GeoData geoData2 = fdnToGeoDataMap.get(ftRopNRCellDUPair.getFdn2());
                    if (geoData1.getBearing() != null && geoData2.getBearing() != null) {
                        double bearing1 = bearingCalculator.calculateBearing(geoData1.getCoordinate(), geoData2.getCoordinate());
                        double bearing2 = bearingCalculator.calculateBearing(geoData2.getCoordinate(), geoData1.getCoordinate());
                        double azimuthAffinity = getHgain(bearing1, geoData1.getBearing() / 10D) +
                                getHgain(bearing2, geoData2.getBearing() / 10D);
                        ftRopNRCellDUPair.setAzimuthAffinity(azimuthAffinity);
                    }
                });
    }

    @Override
    public int getPriority() {
        return 30;
    }


    public static double getHgain(double bearing, double azimuth) {
        double relativeAzimuth = getRelativeAzimuth(bearing, azimuth);
        return getHgain(relativeAzimuth);
    }

    /**
     * we assume the antenna is symmetrical on the hplane
     * gain lookup is set every 10 degrees and interpolated between
     *
     * @param relativeAzimuth in degrees
     * @return gain in Dbs
     */
    @VisibleForTesting
    static double getHgain(double relativeAzimuth) {
        int angleSlot = ((int) relativeAzimuth / 10);
        return hgain[angleSlot] + (hgain[angleSlot + 1] - hgain[angleSlot]) * (relativeAzimuth % 10) / 10;
    }

    /**
     * Absolute azimuth relative to the given bearing
     *
     * @param bearing in degrees, -180 to + 180
     * @param azimuth in degrees, 0 to 360
     * @return absolute (positive) relative azimuth 0 to 180 degrees
     */
    @VisibleForTesting
    static double getRelativeAzimuth(double bearing, double azimuth) {
        double bearing360 = (bearing >= 0) ? bearing : (360 + bearing);
        double relativeAzimuth = Math.abs(azimuth - bearing360);
        if (relativeAzimuth > 180) {
            relativeAzimuth = 360 - relativeAzimuth;
        }
        return relativeAzimuth;
    }

    // magic numbers of horizontal gain (normalized to forward gain - that means starting from zero)
    // for a generic three sector antennas, by 10 degrees
    // they should be replaced with antenna patterns and proper antenna model
    // if/when available, include tilt (vertical gain), etc.
    static final double[] hgain = new double[]{
            0,
            -0.5,
            -2,
            -3,
            -5,
            -7.5,
            -10,
            -14,
            -18,
            -24,
            -27,
            -25,
            -30,
            -30,
            -30,
            -30,
            -30,
            -30,
            -30,
            -30};
}
