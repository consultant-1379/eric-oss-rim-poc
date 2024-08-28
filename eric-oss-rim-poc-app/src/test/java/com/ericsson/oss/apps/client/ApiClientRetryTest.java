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

package com.ericsson.oss.apps.client;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.ericsson.oss.apps.api.ApiClient;
import com.ericsson.oss.apps.api.cts.model.NrCell;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "client.cts.base-path=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(port = 0)
class ApiClientRetryTest {

    @Autowired
    @SpyBean
    private ApiClient apiClient;

    @Value("${client.cts.base-path}")
    String basePath;

    @Value("${client.retries.tooManyRequestWaitMs}")
    long tooManyRequestWaitMs;

    @Value("${client.retries.serverErrorWaitMs}")
    long serverErrorWaitMs;

    @Value("${client.retries.apiRetries}")
    int apiRetries;

    @BeforeEach
    void setup() {
        apiClient.setBasePath(basePath);
    }

    /**
     * checks a 429 reply is retried the correct number of times
     * and waits at least the configured wait time before a
     * {@link org.springframework.web.client.HttpClientErrorException}
     * is thrown
     */
    @Test
    void tooManyRequests() {
        stubFor(get(urlPathEqualTo("/ctw/nrcell"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.TOO_MANY_REQUESTS.value())));
        checkExceptionIsThrown(HttpClientErrorException.class, tooManyRequestWaitMs);
    }

    /**
     * checks a server exception is retried the correct number of times
     * and waits at least the configured wait time before a
     * {@link org.springframework.web.client.HttpServerErrorException}
     * is thrown
     */
    @Test
    void serverError() {
        stubFor(get(urlPathEqualTo("/ctw/nrcell"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        checkExceptionIsThrown(HttpServerErrorException.class, serverErrorWaitMs);
    }

    /**
     * checks different faults are retried the correct number of times
     * and waits at least the configured wait time before a
     * {@link org.springframework.web.client.ResourceAccessException}
     * is thrown
     */
    @Test
    void faultError() {
        var faultList = List.of(Fault.values());
        IntStream.range(0, faultList.size()).forEach(index -> {
                    stubFor(get(urlPathEqualTo("/ctw/nrcell"))
                            .willReturn(WireMock.aResponse()
                                    .withFault(faultList.get(index))));
                    checkExceptionIsThrown(ResourceAccessException.class, serverErrorWaitMs, (index + 1) * apiRetries);
                }
        );
    }

    /**
     * checks a client error reply that has a code different from 429 is NOT retried
     * and a
     * {@link org.springframework.web.client.HttpClientErrorException}
     * is thrown
     */
    @Test
    void nonRetryableClientError() {
        stubFor(get(urlPathEqualTo("/ctw/nrcell"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())));
        checkExceptionIsThrown(HttpClientErrorException.class, 0, 1);
    }

    private void checkExceptionIsThrown(Class<? extends RestClientException> exceptionClass, long waitTime) {
        checkExceptionIsThrown(exceptionClass, waitTime, apiRetries);
    }

    private void checkExceptionIsThrown(Class<? extends RestClientException> exceptionClass, long waitTime, int apiRetries) {
        assertThrows(exceptionClass, () -> {
            long beforeInvoking = System.currentTimeMillis();
            apiClient.invokeAPI("/ctw/nrcell", HttpMethod.GET,
                    new HashMap<>(),
                    new LinkedMultiValueMap<>(),
                    null,
                    new HttpHeaders(),
                    new LinkedMultiValueMap<>(),
                    new LinkedMultiValueMap<>(),
                    Collections.emptyList(), MediaType.APPLICATION_JSON,
                    new String[]{},
                    new ParameterizedTypeReference<List<NrCell>>() {
                    });
            assertTrue(System.currentTimeMillis() >= beforeInvoking + waitTime * apiRetries);
        });
        verify(apiClient, times(apiRetries)).invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

}