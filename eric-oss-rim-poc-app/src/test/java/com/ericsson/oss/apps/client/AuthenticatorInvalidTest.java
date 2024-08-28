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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ericsson.oss.apps.api.ApiClient;
import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import lombok.Getter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Slf4j
class AuthenticatorInvalidTest {

    private static final String EXCEPTION_1 = "Authenticator: Error, cannot create Authenticator as username or password is not valid, cannot login";
    private static final String EXCEPTION_2 = "Authenticator: Error, cannot create Authenticator as apiClient or client have null value, cannot login";

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void authenticatorInvalidTest() throws IOException {
        doAuthenticator("", "pass", new OkHttpClient(), new ApiClient(restTemplate), EXCEPTION_1);
        doAuthenticator(null, "pass", new OkHttpClient(), new ApiClient(restTemplate), EXCEPTION_1);
        doAuthenticator("user", "", new OkHttpClient(), new ApiClient(restTemplate), EXCEPTION_1);
        doAuthenticator("user", null, new OkHttpClient(), new ApiClient(restTemplate), EXCEPTION_1);
        doAuthenticator("user", null, null, new ApiClient(restTemplate), EXCEPTION_2);
        doAuthenticator("user", null, new OkHttpClient(), null, EXCEPTION_2);

    }

    private void doAuthenticator(String un, String pw, OkHttpClient okHttpClient, ApiClient apiClient, String expectedException)
        throws MalformedURLException {
        ClientProperties cp = new ClientProperties();
        cp.basePath = "http://localhost:9999";
        cp.login=new Credential();
        cp.login.username = un;
        cp.login.password = pw;
        if (apiClient != null) {
            apiClient.setBasePath(cp.basePath);
            cp.headers.forEach(apiClient::addDefaultHeader);
            cp.cookies.forEach(apiClient::addDefaultCookie);
        }
        log.info("AuthenticatorInvalidTest; apiClient with base path = {}, clientProperties; basepath = {}, username = {}, valid pasword? {}",
            apiClient == null ? "null" : apiClient.getBasePath(), cp.getBasePath(), cp.getLogin().getUsername(),
            (cp.getLogin().getPassword() != null && !cp.getLogin().getPassword().isBlank()));
        URL aURL = new URL(cp.basePath);
        URL newUrl = new URL(aURL.getProtocol(), aURL.getHost(), aURL.getPort(), "/auth/v1/login");
        String exceptionMessage = assertThrows(RimHandlerException.class,
            () -> new Authenticator(okHttpClient, apiClient, newUrl.toString(), cp.login.username, cp.login.password), "Expected RimHandlerException")
                .getMessage();
        log.info("EXCEPTION_MESSAGE is '{}'", exceptionMessage);
        assertThat(exceptionMessage).contains(expectedException);
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