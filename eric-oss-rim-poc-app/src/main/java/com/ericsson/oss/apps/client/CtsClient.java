/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.apps.client;

import static com.ericsson.oss.apps.api.cts.model.Resource.JSON_PROPERTY_EXTERNAL_ID;

import com.ericsson.oss.apps.api.cts.CtsClientApi;
import com.ericsson.oss.apps.api.cts.model.WirelessTopology;
import com.ericsson.oss.apps.model.CGI;
import com.ericsson.oss.apps.model.CmHandleCgi;
import com.ericsson.oss.apps.model.Constants;
import com.ericsson.oss.apps.model.GeoData;
import com.ericsson.oss.apps.repositories.CmHandleCgiRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class CtsClient {

    public static final String GEO_CACHE = "cmHandle";

    private static final String NAME = "name";
    private static final String ATTRS = "attrs";
    private static final String VALUE = "value";
    private static final String BEARING = "bearing";
    private static final String COORDINATES = "coordinates";
    private static final String GEOSPATIAL_DATA = "geospatialData";
    private static final List<String> CELL_GEO_ASSOCIATIONS_KEYS = List.of("fs.geographicSite",
            "fs.geographicSite.locatedAt", "fs.geographicSite.locatedAt.antennaModules");
    static final Map<String, String> CELL_GEO_ASSOCIATIONS = CELL_GEO_ASSOCIATIONS_KEYS.stream()
            .collect(Collectors.toUnmodifiableMap(Function.identity(), x -> ATTRS));

    private final CtsClientApi clientApi;

    private final CmHandleCgiRepo cmHandleCgiRepo;

    Pattern pattern = Pattern.compile("^((SubNetwork=[\\-\\w]+,)+MeContext=[\\-\\w]+,)?ManagedElement=[\\-\\w]+,GNBDUFunction=[\\-\\w]+,NRCellDU=[\\-\\w]+$");

    @Timed
    public Optional<String> getGNBCmHandleByCGI(CGI cgi) {
        if (cgi == null) {
            return Optional.empty();
        }
        String netFunctionCon = cgi.getNetFunctionCon();
        return cmHandleCgiRepo.findByNodeCgi(netFunctionCon)
                .map(CmHandleCgi::getHandle)
                // if we cannot find it in DB...
                .or(() -> {
                    try {
                        // hit CTS
                        Optional<JsonNode> node = clientApi.queryTopologyObjects(WirelessTopology.GNBCUCP,
                                Collections.singletonMap("wirelessNFConnections.name", netFunctionCon)).stream().findAny();
                        return node.flatMap(n -> extractCmHandleFromCTSReply(n, cgi))
                                .stream()
                                // cache locally if present
                                .peek(cmHandleString -> cmHandleCgiRepo.save(new CmHandleCgi(cmHandleString, netFunctionCon)))
                                .findAny();
                    } catch (RestClientException restClientException) {
                        log.error("failed to get cm handle for cgi {} from cts", cgi, restClientException);
                        return Optional.empty();
                    }
                });
    }

    @NotNull
    private Optional<String> extractCmHandleFromCTSReply(JsonNode node, CGI cgi) {
        String externalId = node.path(JSON_PROPERTY_EXTERNAL_ID).asText();
        if (externalId.isBlank()) {
            log.warn("Cannot extract cmhandle value from CTS reply for CGI {}", cgi);
            return Optional.empty();
        }
        return Optional.of(externalId.substring(0, externalId.indexOf(Constants.SLASH)));
    }

    private Optional<GeoData> extractGeoDataFromJsonNode(String cellName, JsonNode geospatialData) {
        Coordinate coordinate = new Coordinate();
        GeoData.GeoDataBuilder builder = GeoData.builder().coordinate(coordinate);

        List<Consumer<Double>> setters = List.of(coordinate::setX, coordinate::setY, coordinate::setZ);
        ArrayNode coordinates = (ArrayNode) geospatialData.findValue(COORDINATES);
        IntStream.range(0, Math.min(coordinates.size(), setters.size()))
                .forEach(i -> setters.get(i).accept(coordinates.get(i).asDouble()));

        // Todo: Modify after CTS model is fixed, now we will load azimuth with the same name
        geospatialData.findValues(VALUE).stream()
                .filter(antenna -> cellName.equals(antenna.get(NAME).asText()))
                .map(antenna -> antenna.findPath(BEARING))
                .filter(JsonNode::isInt)
                .map(JsonNode::asInt)
                .findAny().ifPresent(builder::bearing);

        // Todo: Yeah I know, not the nicest solution
        GeoData geoData = builder.build();
        if (geoData.getBearing() == null) {
            return Optional.empty();
        } else {
            return Optional.of(geoData);
        }
    }

    public static String generateCtsFriendlyFdn(String fdn) {
        return Arrays.stream(fdn.split(Constants.COMMA))
                .map(x -> x.split(Constants.EQUAL)[1])
                .collect(Collectors.joining(Constants.SLASH));
    }

    @Cacheable(GEO_CACHE)
    public Optional<GeoData> getNrCellGeoData(String fdn) {
        if (fdn == null || !pattern.matcher(fdn).matches()) {
            log.error("{} is not a valid NRCellDU fdn", fdn);
            return Optional.empty();
        }
        String cellName = generateCtsFriendlyFdn(fdn);
        Map<String, String> params = new HashMap<>(CELL_GEO_ASSOCIATIONS);
        params.put(NAME, cellName);
        try {
            return clientApi.queryTopologyObjects(WirelessTopology.NRCELL, params).stream().findAny()
                    .flatMap(cell -> Optional.ofNullable(cell.findParent(GEOSPATIAL_DATA)))
                    .flatMap(geospatialParent -> extractGeoDataFromJsonNode(cellName, geospatialParent))
                    .stream().peek(geoData -> geoData.setFdn(fdn)).findAny();
        } catch (RestClientException restClientException) {
            log.warn("no geo data could be found for fdn {}", fdn, restClientException);
            return Optional.empty();
        }
    }

}

