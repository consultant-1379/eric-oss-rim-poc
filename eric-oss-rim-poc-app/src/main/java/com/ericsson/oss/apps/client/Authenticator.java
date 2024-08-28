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

package com.ericsson.oss.apps.client;

import com.ericsson.oss.apps.api.ApiClient;
import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.ericsson.oss.apps.utils.Utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * The Class Authenticator.
 * Used to login to CTS and NCMP ( or indeed any url) and get JSesssionId and save it as cookie (to be used with each call to URL) and
 * to periodically get a new jsession ID and renew it.
 *
 */
@Slf4j
@Getter
public class Authenticator {

    private final OkHttpClient client;
    private final ApiClient apiClient;
    private final Request request;

    /**
     * Instantiates a new authenticator.
     *
     * @param client
     *     the client
     * @param apiClient
     *     the api client
     * @param url
     *     the url
     * @param username
     *     the username
     * @param password
     *     the password
     */
    public Authenticator(OkHttpClient client, ApiClient apiClient, String url, String username, String password) {
        if (client  == null || apiClient ==null ) {
            String message = String.format("Authenticator: Error, cannot create Authenticator as apiClient or client have null value, cannot login; "
                + "Client Null ?: %s, apiClient null? %s", client == null, apiClient == null);
            throw new RimHandlerException(message);
        }
        log.info("Authenticator: Creating Authenticator to apiClient with base path '{}', url '{}' username '{}' and valid password? :{}",
            apiClient.getBasePath(), url, username, Utils.of().isValidUsernamePwd(password));
        if (!Utils.of().isValidUsernamePwd(username) || !Utils.of().isValidUsernamePwd(password)) {
            String message = String.format("Authenticator: Error, cannot create Authenticator as username or password is not valid, cannot login; "
                + "apiClient with base path '%s', url '%s' username '%s' and valid password?: %s", apiClient.getBasePath(), url, username, Utils.of().isValidUsernamePwd(password));
            throw new RimHandlerException(message);
        }
        this.client = client;
        this.apiClient = apiClient;
        this.request = initRequest(url, username, password);
    }

    private Request initRequest(String url, String username, String password) {
        log.info("Authenticator: Initializing Request to url '{}' with username '{}' and valid password: {}", url, username,
            Utils.of().isValidUsernamePwd(password));
        Headers headers = new Headers.Builder()
                .add("X-Login", username)
                .add("X-password", password)
                .add("X-tenant", "master")
                .build();

        return new Request.Builder()
                .url(url)
                .headers(headers)
                .post(new FormBody.Builder().build())
                .build();
    }

    /**
     * Login to server, if successful, the JSessionID is returned in the response and saved as cookie.
     *
     * @return true, if successful
     *
     * @throws RimHandlerException
     *     the rim handler exception
     */
    @Scheduled(fixedRateString = "${client.auth-refresh-period}000")
    public boolean login() throws RimHandlerException {
        String loginDetails = "url '" + request.url() + "', username '" + request.headers().get("X-Login") + "', validPassword? "
            + Utils.of().isValidUsernamePwd(request.headers().get("X-password")) + "'";

        log.debug("Authenticator: Login attempt to '{}'; request = {}", request.url(), request.toString());
        try (Response response = client.newCall(request).execute()) {
            ResponseBody rb = response.body();
            if (rb == null) {
                throw new RimHandlerException("Authenticator: invalid JSESSIONID in login response to " + loginDetails + ", Response Body is: null'");
            }
            String resp = rb.string();
            if (!isValidJSessionId(resp)) {
                throw new RimHandlerException("Authenticator: invalid JSESSIONID in login response to " + loginDetails + ", Response is: '" + resp + "'");
            }
            String sessionId = resp;
            apiClient.addDefaultCookie("JSESSIONID", sessionId);
            log.debug("Authenticator: Login successful to '{}'; request = {}, Response length = '{}'", request.url(), request.toString(),
                resp.length());
            return true;
        } catch (Exception e) {
            throw new RimHandlerException("Logon to platform: " + loginDetails + ",  has failed with exception " + e);
        }
    }

    private boolean isValidJSessionId(String value) {
        if (!Utils.of().isValidUsernamePwd(value)) {
            return false;
        }
        if (!value.contains("-")) {
            return false;
        }
        String alphaNumValue = value.replace("-", "");

        return StringUtils.isAlphanumeric(alphaNumValue);
    }
}
