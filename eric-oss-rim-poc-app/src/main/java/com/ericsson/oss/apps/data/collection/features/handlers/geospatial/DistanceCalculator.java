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

import com.ericsson.oss.apps.model.mom.NRCellDU;
import lombok.experimental.UtilityClass;
import org.locationtech.jts.geom.Coordinate;

import static com.ericsson.oss.apps.model.Constants.DEFAULT_SCS;
import static com.ericsson.oss.apps.model.Constants.PER_SYMBOL_DISTANCE;

@UtilityClass
public class DistanceCalculator {

    public static Double calculateGuardDistance(NRCellDU nrCellDU) {
        return nrCellDU.getEffectiveGuardSymbols() * (DEFAULT_SCS / nrCellDU.getSubCarrierSpacing()) *
                PER_SYMBOL_DISTANCE;
    }

    public double haversine(Coordinate c1, Coordinate c2) {
        double lon1 = c1.getX();
        double lat1 = c1.getY();
        double lon2 = c2.getX();
        double lat2 = c2.getY();
        // distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(lat1) *
                        Math.cos(lat2);
        double rad = 6371;
        double c = 2 * Math.asin(Math.sqrt(a));
        return rad * c;
    }
}
