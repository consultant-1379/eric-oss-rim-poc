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

import com.ericsson.oss.apps.config.DuctDetectionConfig;
import lombok.Getter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jaitools.numeric.Range;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;
import org.opengis.geometry.Envelope;
import org.springframework.data.util.Pair;
import java.util.*;
import java.util.stream.Collectors;

public class DuctStrengthCalculator {

    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final TreeMap<Integer, List<Polygon>> rangePolygonsMap = new TreeMap<>(Collections.reverseOrder());

    @Getter
    private final GridCoverage2D croppedCoverage;
    @Getter
    private final List<Pair<Integer, SimpleFeatureCollection>> minStrengthAndDuctingPolygonsList;

    public DuctStrengthCalculator(GridCoverage2D gridCoverage2D, DuctDetectionConfig ductDetectionConfig) {

        //crop to required area
        Envelope envelope = GeoUtils.createEnvelope(ductDetectionConfig, gridCoverage2D);
        croppedCoverage = GeoUtils.cropCoverage(gridCoverage2D, envelope);

        // build a list of duct strength ranges
        // over the minimum detectable
        // and calculate the associated polygons
        List<Range<Integer>> ductStrengthLevels = ductDetectionConfig.getDuctStrengthRanges()
                .stream()
                .map(rangeStart -> new Range<>(rangeStart, true, 1000, false))
                .collect(Collectors.toUnmodifiableList());

        minStrengthAndDuctingPolygonsList = ductStrengthLevels.stream()
                .map(range -> Pair.of(range.getMin(), GeoUtils.calculatePolygons(range, croppedCoverage)))
                .collect(Collectors.toUnmodifiableList());

        //calculate polygons and insert in treemap for each range
        minStrengthAndDuctingPolygonsList.stream()
                .filter(minStrengthAndPolygons -> minStrengthAndPolygons.getFirst() >= ductDetectionConfig.getMinDetectedDuctStrength())
                .forEach(strengthAndPolygons -> {
                    try (SimpleFeatureIterator simpleFeatureIterator = strengthAndPolygons.getSecond().features()) {
                        List<Polygon> polygons = new ArrayList<>();
                        while (simpleFeatureIterator.hasNext()) {
                            polygons.add((Polygon) simpleFeatureIterator.next().getAttribute("the_geom"));
                        }
                        rangePolygonsMap.put(strengthAndPolygons.getFirst(), polygons);
                    }
                });
    }

    public Optional<Double> getDuctStrength(Coordinate c1, Coordinate c2, float maxDuctStrength) {
        LineSegment los = new LineSegment(c1, c2);
        return rangePolygonsMap.entrySet().stream()
                .filter(ductStrength -> ductStrength.getKey()<=maxDuctStrength)
                .filter(ductStrength -> ductStrength.getValue().stream()
                        .anyMatch(polygon -> polygon.covers(los.toGeometry(geometryFactory))))
                .map(ductStrength -> ductStrength.getKey().doubleValue())
                .findFirst();
    }
}
