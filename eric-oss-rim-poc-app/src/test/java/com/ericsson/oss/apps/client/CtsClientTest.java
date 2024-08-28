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

import static com.ericsson.oss.apps.client.CtsClient.GEO_CACHE;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.cloud.contract.spec.internal.MediaTypes.APPLICATION_JSON;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import com.ericsson.oss.apps.api.cts.CtsClientApi;
import com.ericsson.oss.apps.model.CGI;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "client.cts.base-path=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(port = 0)
class CtsClientTest {

    private static final String FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR01gNodeBRadio00002,ManagedElement=NR01gNodeBRadio00002,GNBDUFunction=1,NRCellDU=NR01gNodeBRadio00002-1";
    private static final Coordinate COORDINATE = new Coordinate(47.4710509F, 19.0632047F);
    private static final ResponseDefinition RESPONSE_TEMPLATE = WireMock.aResponse()
            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
            .withTransformers("response-template")
            .build();

    @Autowired
    private CtsClient ctsClient;

    @Autowired
    CtsClientApi ctsClientApi;

    @Autowired
    private CacheManager cacheManager;

    CGI CGI = new CGI(128, 49, 4848, 22, 0);

    @BeforeEach
    void resetCache() {
        Optional<Cache> geoDataCache = Optional.ofNullable(cacheManager.getCache(GEO_CACHE));
        geoDataCache.ifPresent(Cache::invalidate);
        ctsClientApi.getApiClient().addDefaultCookie("JSESSIONID", "253204e8-793b-4060-9bb3-78c078423afd");
    }

    @Test
    void getNrCellGeoData() {
        stubJsonReply("cts/nrcell.json");
        ctsClient.getNrCellGeoData(FDN).ifPresentOrElse(geoData -> {
            assertEquals(COORDINATE.x, geoData.getCoordinate().x, 0.0001);
            assertEquals(COORDINATE.y, geoData.getCoordinate().y, 0.0001);
            assertEquals(2372, geoData.getBearing());
            assertEquals(FDN, geoData.getFdn());
        }, () -> Assertions.fail("No geo data returned"));
    }

    @Test
    void getNrCellNoGeoData() {
        stubJsonReply("cts/nrcell_nogeo.json");
        assertTrue(ctsClient.getNrCellGeoData(FDN).isEmpty());
    }

    @Test
    void getNrCellNoBearing() {
        stubJsonReply("cts/nrcell_nobearing.json");
        assertTrue(ctsClient.getNrCellGeoData(FDN).isEmpty());
    }

    @Test
    void getNrCellBadFdn() {
        List.of("", " ", "sdjkfhsdljfh").forEach(fdn ->assertTrue(ctsClient.getNrCellGeoData(fdn).isEmpty()));
        assertTrue(ctsClient.getNrCellGeoData(null).isEmpty());
    }

    private static void stubJsonReply(String jsonFile) {
        stubFor(get(urlPathEqualTo("/ctw/nrcell"))
                .withQueryParams(CtsClient.CELL_GEO_ASSOCIATIONS.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> equalTo(e.getValue()))))
                .withQueryParam("name", equalTo(CtsClient.generateCtsFriendlyFdn(FDN)))
                .willReturn(ResponseDefinitionBuilder.like(RESPONSE_TEMPLATE)
                        .withBodyFile(jsonFile)));
    }

    @Test
    void failedGetNrCellGeoData() {
        stubFor(get(urlPathEqualTo("/ctw/nrcell"))
                .withQueryParams(CtsClient.CELL_GEO_ASSOCIATIONS.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> equalTo(e.getValue()))))
                .withQueryParam("name", equalTo(CtsClient.generateCtsFriendlyFdn(FDN)))
                .willReturn(ResponseDefinitionBuilder.like(RESPONSE_TEMPLATE)
                        .withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
        ctsClient.getNrCellGeoData(FDN).ifPresent(geoData -> Assertions.fail("expected an empty Optional"));
    }

    @Test
    void getGNBCmHandleByCGI() {
        stubFor(get(urlPathEqualTo("/ctw/gnbcucp"))
                .withQueryParams(Collections.singletonMap("wirelessNFConnections.name", equalTo("128-49-4848-22")))
                .willReturn(ResponseDefinitionBuilder.like(RESPONSE_TEMPLATE)
                        .withBodyFile("cts/gnbcucp.json")));
        ctsClient.getGNBCmHandleByCGI(CGI)
                .ifPresentOrElse(cmHandle -> assertEquals("DD9220ED04FB3E84D94344E6256E22F0", cmHandle), () -> Assertions.fail("No cmhandle returned"));
    }

    @Test
    void getGNBCmHandleByNullCGI() {
        assertTrue(ctsClient.getGNBCmHandleByCGI(null).isEmpty());
    }

    @Test
    void failedGNBCmHandleByCGI() {
        stubFor(get(urlPathEqualTo("/ctw/gnbcucp"))
                .withQueryParams(Collections.singletonMap("wirelessNFConnections.name", equalTo("128-49-4848-22")))
                .willReturn(ResponseDefinitionBuilder.like(RESPONSE_TEMPLATE)
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));
        ctsClient.getGNBCmHandleByCGI(CGI).ifPresent(cgi -> Assertions.fail("expected an empty Optional"));
    }
}