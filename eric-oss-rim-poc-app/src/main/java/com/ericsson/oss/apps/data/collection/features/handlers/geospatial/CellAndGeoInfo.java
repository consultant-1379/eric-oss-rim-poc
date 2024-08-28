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
package com.ericsson.oss.apps.data.collection.features.handlers.geospatial;

import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;

import java.util.List;

@Getter
class CellAndGeoInfo {
    private final Coordinate coordinate;
    @Setter
    private List<Integer> bandList;
    private final ManagedObjectId managedObjectId;
    @Setter
    private double guardDistance;
    @Setter
    private float refractiveIndex = Float.NaN;

    public CellAndGeoInfo(Coordinate coordinate, String fdn) {
        this.coordinate = coordinate;
        this.managedObjectId = ManagedObjectId.of(fdn);
    }

    String getFdn() {
        return managedObjectId.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CellAndGeoInfo that = (CellAndGeoInfo) o;

        return managedObjectId.equals(that.managedObjectId);
    }

    @Override
    public int hashCode() {
        return managedObjectId.hashCode();
    }
}
