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

import org.apache.commons.io.IOUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class GeoTiffLoader {

    GridCoverage2D loadCoverage(InputStream inputStream) throws IOException {
        // this may be of overkill, however the geotiff uses
        // ehcache, depending on the InputStream passed
        // given the size of the tiff we load, this guarantees everything is in memory
        // and caching is not required (dependency disabled in pom)
        byte[] byteArray = IOUtils.toByteArray(inputStream);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        GeoTiffReader geoTiffReader = new GeoTiffReader(byteArrayInputStream,
                new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));
        GridCoverage2D coverage = geoTiffReader.read(null);
        geoTiffReader.dispose();
        return coverage;
    }
}
