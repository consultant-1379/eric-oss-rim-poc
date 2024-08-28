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

import com.ericsson.oss.apps.api.ApiClient;
import com.ericsson.oss.apps.api.cts.CtsClientApi;
import com.ericsson.oss.apps.api.ncmp.CpsQueryApi;
import com.ericsson.oss.apps.api.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.ericsson.oss.apps.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Slf4j
@Aspect
@Configuration
@RequiredArgsConstructor
public class ClientConfig {

    private final RestTemplate restTemplate;
    private final OkHttpClient okHttpClient;

    @Data
    public static class Credential {
        private String username;
        private String password;
    }

    @Data
    public static class ClientProperties {
        private String basePath;
        private Credential login;
        private Map<String, String> cookies = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();
    }

    @Bean
    @Scope("prototype")
    @ConfigurationProperties(prefix = "client")
    ClientProperties globalClientProperties() {
        return new ClientProperties();
    }

    @Bean
    @Primary
    @Scope("prototype")
    ApiClient apiClient() {
        return new ApiClient(restTemplate);
    }

    @Bean
    @ConfigurationProperties(prefix = "client.cts")
    public ClientProperties ctsClientProperties() {
        return globalClientProperties();
    }

    @Bean
    @Primary
    public CtsClientApi ctsClientApi() {
        return new CtsClientApi(configureClient(apiClient(), ctsClientProperties()));
    }

    @Bean
    @Scope("prototype")
    @ConfigurationProperties(prefix = "client.ncmp")
    public ClientProperties ncmpClientProperties() {
        return globalClientProperties();
    }

    @Bean
    @Primary
    public NetworkCmProxyApi networkCmProxyApi() {
        ClientProperties properties = ncmpClientProperties();
        properties.setBasePath(properties.getBasePath() + "/ncmp");
        return new NetworkCmProxyApi(configureClient(apiClient(), properties));
    }

    @Bean
    @Primary
    public CpsQueryApi cpsQueryApi() {
        ClientProperties properties = ncmpClientProperties();
        properties.setBasePath(properties.getBasePath() + "/cps/api");
        return new CpsQueryApi(configureClient(apiClient(), properties));
    }

    @Bean
    @Scope("prototype")
    public Authenticator authenticator(ApiClient apiClient, ClientProperties clientProperties) {
        try {
            log.debug("ClientConfig: Creating Authenticator for apiClient with base path {} ", apiClient.getBasePath());
            URL aURL = new URL(clientProperties.basePath);
            URL newUrl = new URL(aURL.getProtocol(), aURL.getHost(), aURL.getPort(), "/auth/v1/login");
            return new Authenticator(okHttpClient, apiClient, newUrl.toString(), clientProperties.login.username, clientProperties.login.password);
        } catch (MalformedURLException e) {
            throw new RimHandlerException("ClientConfig: Failure to create Authenticator; MalformedURLException for apiClient with base path " + apiClient.getBasePath() + ", MalformedURLException :" + e);
        }
    }

    private ApiClient configureClient(ApiClient apiClient, ClientProperties clientProperties) {
        apiClient.setBasePath(clientProperties.basePath);
        clientProperties.headers.forEach(apiClient::addDefaultHeader);
        clientProperties.cookies.forEach(apiClient::addDefaultCookie);
        log.trace("ClientConfig:configureClient;  apiClient with base path = {}, clientProperties; basepath = {}, username = {}, valid pasword? {}",
            apiClient.getBasePath(), clientProperties.getBasePath(), clientProperties.getLogin().getUsername(),Utils.of().isValidUsernamePwd(clientProperties.getLogin().getPassword()));
        if (clientProperties.getLogin() != null) {
            authenticator(apiClient, clientProperties);
        }
        return apiClient;
    }

    // Todo: It can be removed after NCMP starts handling nulls properly
    @Pointcut("execution(public com.fasterxml.jackson.databind.JsonNode com.ericsson.oss.apps.api.ncmp.NetworkCmProxyApi.*(..))")
    private void jsonReturningNcmpEndpoint(){ throw new UnsupportedOperationException(); }

    @AfterReturning(pointcut = "jsonReturningNcmpEndpoint()", returning = "jsonNode")
    public void removeNullStrings(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            List<String> propertyNames = new LinkedList<>();

            while(fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode childNode = field.getValue();

                if (childNode.isTextual() && "null".equals(childNode.asText())) {
                    propertyNames.add(field.getKey());
                } else if (childNode.isContainerNode()) {
                    removeNullStrings(childNode);
                }
            }

            ((ObjectNode) jsonNode).remove(propertyNames);

        } else if (jsonNode.isArray()) {
            for (JsonNode childNode : jsonNode) {
                removeNullStrings(childNode);
            }
        }
    }
}
