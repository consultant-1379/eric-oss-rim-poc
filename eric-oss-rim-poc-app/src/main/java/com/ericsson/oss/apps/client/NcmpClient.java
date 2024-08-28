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

import com.ericsson.oss.apps.api.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.model.CmHandle;
import com.ericsson.oss.apps.model.Constants;
import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.MoType;
import com.ericsson.oss.apps.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.model.Constants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
@RequiredArgsConstructor
public class NcmpClient {

    static final String OPTION_FIELDS_VALUE = Arrays.stream(MoType.values())
            .map(type -> String.format("%s/%s(*)", type.getFullName(), ATTRIBUTES))
            .collect(Collectors.joining(Constants.SEMICOLON, "fields=", Constants.EMPTY));
    private final NetworkCmProxyApi networkCmProxyApi;
    private final ApplicationContext context;
    private final MoSaver mosaver;
    private final ObjectMapper objectMapper;


    static String toNcmpRefId(String refId) {
        if (refId.isEmpty()) {
            return Constants.SLASH;
        } else {
            return Arrays.stream(refId.split(Constants.COMMA))
                    .map(dn -> Arrays.stream(dn.split(Constants.EQUAL))
                            .collect(Collectors.joining("[@id=",
                                    MoType.DEFAULT_NAMESPACE + Constants.COLON,
                                    Constants.RIGHT_SQUARE_BRACKET))
                    ).collect(Collectors.joining(Constants.SLASH, Constants.SLASH, Constants.EMPTY));
        }
    }

    /**
     * @param resource the resource to patch
     * @return true if the patch was successful, false otherwise
     */
    @Timed
    public boolean patchCmResource(ManagedObject resource) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode objects = objectMapper.createArrayNode();
        body.putIfAbsent(resource.getClass().getSimpleName(), objects);
        ObjectNode object = objectMapper.createObjectNode();
        objects.add(object);
        object.putIfAbsent(ID, objectMapper.valueToTree(resource.getObjectId().fetchDNValue()));
        object.putIfAbsent(ATTRIBUTES, objectMapper.valueToTree(resource));
        ManagedObjectId parentId = resource.getObjectId().fetchParentId();
        log.debug("Patching MO: " + resource.toFdn());

        try {
            return getCmHandleFromMeFDn(parentId.getMeFdn())
                    .map(cmHandle -> {
                        networkCmProxyApi.patchResourceDataRunningForCmHandle(cmHandle.getHandle(), toNcmpRefId(parentId.getRefId()),
                                body, APPLICATION_JSON.toString());
                        return true;
                    }).orElse(false);
        } catch (RestClientException restClientException) {
            log.error("patching of cm resource {} on NCMP failed", resource.getObjectId().toFdn(), restClientException);
            return false;
        }
    }

    /**
     * at the moment this is completely deterministic,so returning an Optional
     * makes little sense. However, with the introduction of identity service we
     * will get the cmhandle from there so leaving the method here for convenience.
     *
     * @param meFdn
     * @return
     */
    private Optional<CmHandle> getCmHandleFromMeFDn(String meFdn) {
        return Optional.of(new CmHandle(Utils.of().calcCmHandle(meFdn), meFdn));
    }

    @Timed
    public <T extends ManagedObject> Optional<T> getCmResource(final ManagedObjectId managedObjectId, Class<T> tClass) {
        return getInstance(managedObjectId, tClass).or(() ->
                getCmHandleFromMeFDn(managedObjectId.getMeFdn()).stream()
                        .peek(cmHandle -> log.debug("Getting CM resources for fdn " + managedObjectId.toFdn() + " and cmhandle " + cmHandle.getHandle()))
                        .flatMap(cmHandle -> syncMeContext(cmHandle.getHandle()).stream())
                        .filter(managedObject -> managedObject.getObjectId().equals(managedObjectId))
                        .filter(tClass::isInstance)
                        .map(tClass::cast)
                        .findAny());
    }

    private <T extends ManagedObject> Optional<T> getInstance(final ManagedObjectId managedObjectId, Class<T> tClass) {
        ResolvableType type = ResolvableType.forClassWithGenerics(JpaRepository.class, tClass, ManagedObjectId.class);
        ObjectProvider<JpaRepository<T, ManagedObjectId>> repositoryProvider = context.getBeanProvider(type);
        return Optional.ofNullable(repositoryProvider.getIfUnique())
                .flatMap(repository -> repository.findById(managedObjectId));
    }

    @Timed
    public List<ManagedObject> syncMeContext(final String cmHandle) {
        try {
            log.debug("syncMeContext for cmhandle " + cmHandle);
            JsonNode cmResources = networkCmProxyApi.getResourceDataRunningForCmHandle(
                    cmHandle, toNcmpRefId(EMPTY), OPTION_FIELDS_VALUE, null);
            return mosaver.parseAndSaveManagedObjects(cmResources);
        } catch (RestClientException e) {
            log.error("Cannot get resources for cmhandle {}", cmHandle, e);
            return Collections.emptyList();
        }
    }

}
