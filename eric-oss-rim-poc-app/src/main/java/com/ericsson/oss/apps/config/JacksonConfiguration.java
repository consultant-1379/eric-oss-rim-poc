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
package com.ericsson.oss.apps.config;

import com.ericsson.oss.apps.model.mom.*;
import com.ericsson.oss.apps.model.mom.deserializer.ManagedObjectDeserializer;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class JacksonConfiguration {

    protected final ObjectMapper objectMapper;
    private final ApplicationContext context;

    @PostConstruct
    private void init() {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer)
            {
                if (beanDesc.getBeanClass() == ManagedElement.class) {
                    return deserializer;
                } else if (ManagedObject.class.isAssignableFrom(beanDesc.getBeanClass())) {
                   return createManagedObjectDeserializer(beanDesc.getBeanClass().asSubclass(ManagedObject.class), deserializer);
                }
                return deserializer;
            }
        });
        objectMapper.registerModule(module);
    }

    private <T extends ManagedObject> ManagedObjectDeserializer<T> createManagedObjectDeserializer(Class<T> tClass, JsonDeserializer<?> deserializer) {
        ResolvableType type = ResolvableType.forClassWithGenerics(JpaRepository.class, tClass, ManagedObjectId.class);
        ObjectProvider<JpaRepository<T, ManagedObjectId>> beanProvider = context.getBeanProvider(type);
        return new ManagedObjectDeserializer<>(tClass, deserializer, beanProvider.getObject());
    }
}
