/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class BearingCalculator {

    @Cacheable("bearingCalculation")
    public double calculateBearing(Coordinate c1, Coordinate c2) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
        geodeticCalculator.setStartingGeographicPoint(c1.getX(), c1.getY());
        geodeticCalculator.setDestinationGeographicPoint(c2.getX(), c2.getY());
        return geodeticCalculator.getAzimuth();
    }
}
