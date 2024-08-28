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

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.contract.spec.internal.MediaTypes.APPLICATION_JSON;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ericsson.oss.apps.api.ApiClient;
import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import lombok.Getter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@AutoConfigureWireMock(port = 0)
class AuthenticatorTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    OkHttpClient okHttpClient;

    @Value("${wiremock.server.port}")
    private String wireMockServerPort;

    @Test
    void authenticatorOkTest() throws IOException {
        log.info("-------------------------------------------------------------------------------------------");
        stubFor(post(urlPathEqualTo("/auth/v1/login"))
            .willReturn(WireMock.aResponse()
            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
            .withBody("e767862e-7761-4931-b9b0-fad46b8a9108")));

        Authenticator auth = doAuthenticator("e767862e-7761-4931-b9b0-fad46b8a9108");
        assertThat(auth.getRequest().headers().get("X-Login")).isEqualTo("un");
        assertThat(auth.getRequest().headers().get("X-password")).isEqualTo("pw");
        assertThat(auth.getRequest().url().toString()).isEqualTo("http://localhost:" + wireMockServerPort + "/auth/v1/login");
        assertThat(auth.login()).isTrue();
        log.info("-------------------------------------------------------------------------------------------");
    }

    @Test
    void authenticatorInvalidSessionIdNoDashTest() throws IOException {
        log.info("-------------------------------------------------------------------------------------------");
        Authenticator auth = doAuthenticator("e767862e77614931b9b0fad46b8a9108");
        assertThat(auth.getRequest().headers().get("X-Login")).isEqualTo("un");
        assertThat(auth.getRequest().headers().get("X-password")).isEqualTo("pw");
        assertThat(auth.getRequest().url().toString()).isEqualTo("http://localhost:" + wireMockServerPort + "/auth/v1/login");
        String exceptionMessage = assertThrows(RimHandlerException.class, () -> auth.login(), "Expected RimHandlerException").getMessage();
        log.info("EXCEPTION_MESSAGE is '{}'", exceptionMessage);
        assertThat(exceptionMessage).contains("Authenticator: invalid JSESSIONID in login response");
        log.info("-------------------------------------------------------------------------------------------");
    }

    @Test
    void authenticatorInvalidSessionIdNullTest() throws IOException {
        log.info("-------------------------------------------------------------------------------------------");
        Authenticator auth = doAuthenticator(null);
        assertThat(auth.getRequest().headers().get("X-Login")).isEqualTo("un");
        assertThat(auth.getRequest().headers().get("X-password")).isEqualTo("pw");
        assertThat(auth.getRequest().url().toString()).isEqualTo("http://localhost:" + wireMockServerPort + "/auth/v1/login");
        String exceptionMessage = assertThrows(RimHandlerException.class, () -> auth.login(), "Expected RimHandlerException").getMessage();
        log.info("EXCEPTION_MESSAGE is '{}'", exceptionMessage);
        assertThat(exceptionMessage).contains("Authenticator: invalid JSESSIONID in login response");
        log.info("-------------------------------------------------------------------------------------------");
    }

    @Test
    void authenticatorInvalidSessionIdBlankTest() throws IOException {
        log.info("-------------------------------------------------------------------------------------------");
        Authenticator auth = doAuthenticator("                  ");
        assertThat(auth.getRequest().headers().get("X-Login")).isEqualTo("un");
        assertThat(auth.getRequest().headers().get("X-password")).isEqualTo("pw");
        assertThat(auth.getRequest().url().toString()).isEqualTo("http://localhost:" + wireMockServerPort + "/auth/v1/login");
        String exceptionMessage = assertThrows(RimHandlerException.class, () -> auth.login(), "Expected RimHandlerException").getMessage();
        log.info("EXCEPTION_MESSAGE is '{}'", exceptionMessage);
        assertThat(exceptionMessage).contains("Authenticator: invalid JSESSIONID in login response");
        log.info("-------------------------------------------------------------------------------------------");
    }

    @Test
    void authenticatorInvalidSessionIdNonAlphaTest() throws IOException {
        log.info("-------------------------------------------------------------------------------------------");
        Authenticator auth = doAuthenticator("e767862e-7761-4931-b9b0-fad46b8a910!");
        assertThat(auth.getRequest().headers().get("X-Login")).isEqualTo("un");
        assertThat(auth.getRequest().headers().get("X-password")).isEqualTo("pw");
        assertThat(auth.getRequest().url().toString()).isEqualTo("http://localhost:" + wireMockServerPort + "/auth/v1/login");
        String exceptionMessage = assertThrows(RimHandlerException.class, () -> auth.login(), "Expected RimHandlerException").getMessage();
        log.info("EXCEPTION_MESSAGE is '{}'", exceptionMessage);
        assertThat(exceptionMessage).contains("Authenticator: invalid JSESSIONID in login response");
        log.info("-------------------------------------------------------------------------------------------");
    }

    @Test
    void authenticatorNullApiClientTest() throws IOException {
        log.info("-------------------------------------------------------------------------------------------");
        Authenticator auth = doAuthenticator("e767862e-7761-4931-b9b0-fad46b8a9108");

        assertThat(auth.getRequest().headers().get("X-Login")).isEqualTo("un");
        assertThat(auth.getRequest().headers().get("X-password")).isEqualTo("pw");
        assertThat(auth.getRequest().url().toString()).isEqualTo("http://localhost:" + wireMockServerPort + "/auth/v1/login");
        ReflectionTestUtils.setField(auth, "apiClient", null);
        String exceptionMessage = assertThrows(RimHandlerException.class, () -> auth.login(), "Expected RimHandlerException").getMessage();
        log.info("EXCEPTION_MESSAGE is '{}'", exceptionMessage);
        assertThat(exceptionMessage).contains("Logon to platform:");
        log.info("-------------------------------------------------------------------------------------------");
    }

    private Authenticator doAuthenticator(String expectedResponse) throws MalformedURLException {
        stubFor(post(urlPathEqualTo("/auth/v1/login"))
            .willReturn(WireMock.aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(expectedResponse)));
        ClientProperties cp = new ClientProperties();
        cp.basePath = "http://localhost:" + wireMockServerPort;
        cp.login=new Credential();
        cp.login.username="un";
        cp.login.password="pw";
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cp.basePath);
        cp.headers.forEach(apiClient::addDefaultHeader);
        cp.cookies.forEach(apiClient::addDefaultCookie);
        log.info("AuthenticatorTest; apiClient with base path = {}, clientProperties; basepath = {}, username = {}, valid pasword? {}",
            apiClient.getBasePath(), cp.getBasePath(), cp.getLogin().getUsername(),
            (cp.getLogin().getPassword() != null && !cp.getLogin().getPassword().isBlank()));
        URL aURL = new URL(cp.basePath);
        URL newUrl = new URL(aURL.getProtocol(), aURL.getHost(), aURL.getPort(), "/auth/v1/login");
        Authenticator auth =  new Authenticator(okHttpClient, apiClient, newUrl.toString(), cp.login.username, cp.login.password);
        return auth;
    }

    @Getter
    static class Credential {
        private String username;
        private String password;
    }

    @Getter
    static class ClientProperties {
        private String basePath;
        private Credential login;
        private Map<String, String> cookies = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();
    }
}