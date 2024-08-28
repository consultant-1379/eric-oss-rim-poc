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

import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

import java.util.*;

import static com.ericsson.oss.apps.model.Constants.*;

@RequiredArgsConstructor
class MoCollector {

    private final ObjectMapper mapper;

    public List<JsonNode> collect(JsonNode node) {
        String dnPrefix = Optional.ofNullable(node.findValue(ATTRIBUTES))
                .map(mo -> mo.findValue(DN_PREFIX))
                .map(JsonNode::asText)
                .orElse(EMPTY);
        return collectMOs(dnPrefix, node);
    }

    private void addReference(final JsonNode moNode, final String reference) {
        JsonNode objectId = mapper.valueToTree(ManagedObjectId.of(reference));
        ((ObjectNode) moNode).putIfAbsent(OBJECT_ID, objectId);
    }

    private List<JsonNode> collectMOs(String path, JsonNode node) {
        List<JsonNode> mos = new LinkedList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            if (fieldValue instanceof ArrayNode) {
                if (fieldName.contains(COLON)) {
                    fieldName = fieldName.substring(fieldName.indexOf(COLON) + 1);
                }
                for (JsonNode element : fieldValue) {
                    String nextPath = String.format("%s,%s=%s", path, fieldName, element.get(ID).asText());
                    mos.addAll(collectMOs(nextPath, element));
                }
            } else if (ATTRIBUTES.equals(fieldName)) {
                addReference(fieldValue, path);
                mos.add(fieldValue);
            }
        }
        return mos;
    }
}
