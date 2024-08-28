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

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoJsonPolygonReporter {
    private static final String GEOJSON_PATH = "geospatial/polygons/ducting-polygons-%s-%s.json";
    private final BdrClient bdrClient;
    private final BdrConfiguration bdrConfiguration;

    public void reportGeoJsonPolygons(List<Pair<Integer, SimpleFeatureCollection>> strengthAndDuctingPolygonsList, Long ropTimeStamp) {
        strengthAndDuctingPolygonsList.forEach(ductPolygon -> createGeoJson(ductPolygon.getFirst(), ductPolygon.getSecond(), ropTimeStamp));
    }

    private void createGeoJson(Number minDuctingStrength, SimpleFeatureCollection polygons, Long ropTimeStamp) {
        String fileName = String.format(GEOJSON_PATH, minDuctingStrength, ropTimeStamp);
        var collection = polygons.features();
        while (collection.hasNext()) {
            var feature = collection.next();
            feature.setAttribute("value", minDuctingStrength);
        }
        var featureJSON = new FeatureJSON();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            featureJSON.writeFeatureCollection(polygons, byteArrayOutputStream);
            sendInputStreamS3(fileName, byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            log.error("Cannot create the geoJsonPolygons ", e);
        }
    }

    private void sendInputStreamS3(String filename, byte[] inputStream) {
        bdrClient.uploadInputStreamObject(bdrConfiguration.getBucket(), filename, inputStream);
    }

}
