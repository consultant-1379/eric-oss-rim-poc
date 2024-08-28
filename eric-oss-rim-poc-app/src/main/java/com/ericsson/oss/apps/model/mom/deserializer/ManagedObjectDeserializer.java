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
package com.ericsson.oss.apps.model.mom.deserializer;

import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.IOException;

@Slf4j
public class ManagedObjectDeserializer<T extends ManagedObject> extends StdDeserializer<ManagedObject> implements ResolvableDeserializer {

    private static final long serialVersionUID = 1;

    private final transient JsonDeserializer<?> defaultDeserializer;
    private final transient JpaRepository<T, ManagedObjectId> repository;

    public ManagedObjectDeserializer(
            Class<T> typeParameterClass,
            JsonDeserializer<?> defaultDeserializer,
            JpaRepository<T, ManagedObjectId> repository
    ) {
        super(typeParameterClass);
        this.defaultDeserializer = defaultDeserializer;
        this.repository = repository;
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        boolean isReference = JsonToken.VALUE_STRING.equals(parser.currentToken());
        T managedObject = (T) defaultDeserializer.deserialize(parser, context);
        if (isReference) {
            return repository.findById(managedObject.getObjectId())
                    .orElseGet(() -> repository.save(managedObject));
        } else {
            return repository.save(managedObject);
        }
    }

    @Override
    public void resolve(DeserializationContext context) throws JsonMappingException {
        ((ResolvableDeserializer) defaultDeserializer).resolve(context);
    }
}
