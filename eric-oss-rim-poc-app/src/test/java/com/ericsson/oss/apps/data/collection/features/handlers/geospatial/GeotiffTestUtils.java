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
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class GeotiffTestUtils {

    public static final String GEO_TIFF_FILE_PATH = "src/test/resources/geospatial/ducting/ww-1655218800000.tif";

    public static GridCoverage2D loadGridCoverage2D(String
                                                            filePath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        GeoTiffLoader geoTiffLoader = new GeoTiffLoader();
        return geoTiffLoader.loadCoverage(fileInputStream);
    }

    public static DuctDetectionConfig getDuctDetectionConfig() {
        DuctDetectionConfig ductDetectionConfig = new DuctDetectionConfig();
        ductDetectionConfig.setDuctStrengthRanges(Arrays.asList(35, 43, 55, 65, 75, 95, 110, 150));
        ductDetectionConfig.setMaxLon(8);
        ductDetectionConfig.setMinLon(-5);
        ductDetectionConfig.setMaxLat(51);
        ductDetectionConfig.setMinLat(42);
        ductDetectionConfig.setMinDetectedDuctStrength(43);
        ductDetectionConfig.setImageSize(1600);
        return ductDetectionConfig;
    }
}
