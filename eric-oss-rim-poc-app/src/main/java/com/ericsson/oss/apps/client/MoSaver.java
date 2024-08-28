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

import static com.ericsson.oss.apps.model.Constants.OBJECT_ID;
import static com.ericsson.oss.apps.model.Constants.REF_ID;
import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.MoType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoSaver {

    private static final String DESERIALIZATION_FAILED = "Resource deserialization failed";

    private final ObjectMapper objectMapper;
    @NotNull
    @Synchronized
    @Transactional
    public List<ManagedObject> parseAndSaveManagedObjects(JsonNode cmResources) {
        return new MoCollector(objectMapper).collect(cmResources).stream()
                .map(this::convertMOToType)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    ManagedObject convertMOToType(JsonNode mo) {
        try {
            String resourcePath = mo.path(OBJECT_ID).path(REF_ID).asText();
            return objectMapper.treeToValue(mo, MoType.getMoType(resourcePath).getType());
        } catch (JsonProcessingException e) {
            log.warn(DESERIALIZATION_FAILED, e);
        }
        return null;
    }
}
