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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeoTiffLoaderTest {

    GeoTiffLoader geoTiffLoader;
    FileInputStream fileInputStream;

    @Test
    void loadCoverage() throws IOException {
        fileInputStream = new FileInputStream("src/test/resources/geospatial/ducting/ww-1655218800000.tif");
        geoTiffLoader = new GeoTiffLoader();
        GridCoverage2D coverage = geoTiffLoader.loadCoverage(fileInputStream);
        DirectPosition2D directPosition2D = new DirectPosition2D(coverage.getCoordinateReferenceSystem2D(), 4.17, 42.7);
        float[] refractiveIndex = (float[]) coverage.evaluate(directPosition2D);
        assertEquals(76.87146759033203, refractiveIndex[0]);
    }

    @Test
    void failToLoadCoverageOnFileNotFound() throws FileNotFoundException {
        fileInputStream = new FileInputStream("src/test/resources/geospatial/ducting/wrong-file.tif");
        geoTiffLoader = new GeoTiffLoader();
        assertThrows(IOException.class, () -> geoTiffLoader.loadCoverage(fileInputStream));
    }
}