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
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.raster.PolygonExtractionProcess;
import org.jaitools.numeric.Range;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;

import java.util.Collections;

@UtilityClass
@Slf4j
public class GeoUtils {

    static Envelope createEnvelope(DuctDetectionConfig ductDetectionConfig, GridCoverage2D gridCoverage2D) {
        return new ReferencedEnvelope(
                ductDetectionConfig.getMinLon(),
                ductDetectionConfig.getMaxLon(),
                ductDetectionConfig.getMinLat(),
                ductDetectionConfig.getMaxLat(),
                gridCoverage2D.getCoordinateReferenceSystem2D());
    }

    static GridCoverage2D cropCoverage(GridCoverage2D gridCoverage, Envelope envelope) {
        CoverageProcessor processor = CoverageProcessor.getInstance();
        final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
        param.parameter("Source").setValue(gridCoverage);
        param.parameter("Envelope").setValue(envelope);
        return (GridCoverage2D) processor.doOperation(param);
    }

    static SimpleFeatureCollection calculatePolygons(Range<Integer> range, GridCoverage2D gridCoverage2D) {
        PolygonExtractionProcess polygonExtractionProcess = new PolygonExtractionProcess();
        return polygonExtractionProcess.execute(
                gridCoverage2D,
                0,
                null,
                null, null,
                Collections.singletonList(range),
                null);
    }


    static float getRefractiveIndex(GridCoverage2D gridCoverage2D, Coordinate coordinate) {
        DirectPosition2D directPosition2D = new DirectPosition2D(gridCoverage2D.getCoordinateReferenceSystem2D(), coordinate.getX(),
                coordinate.getY());
        try {
            return ((float[]) gridCoverage2D.evaluate(directPosition2D))[0];
        } catch (PointOutsideCoverageException pointOutsideCoverageException) {
            log.error("Coordinates {} {} are out coverage", coordinate.getX(), coordinate.getY(),pointOutsideCoverageException);
            return Float.NaN;
        }
    }

}
