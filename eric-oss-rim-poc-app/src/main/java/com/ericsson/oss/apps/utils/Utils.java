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
package com.ericsson.oss.apps.utils;

import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.google.common.collect.Lists;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;

import org.apache.avro.Schema;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * The Class Utils.
 */
@Slf4j
public class Utils {

    /**
     * Of.
     *
     * @return the utils
     */
    public static Utils of() {
        return new Utils();
    }

    /**
     * Wait retry interval.
     *
     * @param waitIntervalMillis
     *     the wait interval in milli seconds.
     *
     * @return true, if successful
     */
    public boolean waitRetryInterval(final int waitIntervalMillis) {
        try {
            log.info("Waiting {} ms", waitIntervalMillis);
            Thread.sleep(waitIntervalMillis);
            return true;
        } catch (final InterruptedException | IllegalArgumentException exception) {
            String messgae = "Unexpected interruption while waiting: " + exception.getMessage() + "\n Exception: " + exception;
            log.error(messgae);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Process in thread pool.
     *
     * @param <T>
     *     the generic type
     * @param <U>
     *     the generic type
     * @param <R>
     *     the generic type
     * @param inputList
     *     the input list
     * @param poolSize
     *     the pool size
     * @param processingFunction
     *     the processing function
     * @param additionalArgument
     *     the additional argument
     *
     * @return the list
     */
    public <T, U, R> List<R> processInThreadPool(Collection<T> inputList, int poolSize, BiFunction<Collection<T>, U, List<R>> processingFunction, U additionalArgument) {
        ForkJoinPool forkJoinPool = null;
        try {
            log.info("Forking pool for parallel processing of size {}", poolSize);
            forkJoinPool = new ForkJoinPool(poolSize);
            return forkJoinPool.submit(() -> processingFunction.apply(inputList,additionalArgument)).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception during parallel processing, no data returned", e);
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } finally {
            if (forkJoinPool != null) {
                forkJoinPool.shutdown();
            }
        }
    }


    /**
     * Find all by id.
     *
     * @param <T>
     *     the generic type
     * @param <K>
     *     the key type
     * @param filterKeysList
     *     the filter keys list
     * @param chunkSize
     *     the chunk size
     * @param repository
     *     the repository
     *
     * @return the list
     */
    public <T, K>List<T> findAllById(List<K> filterKeysList, int chunkSize, JpaRepository<T, K> repository) {
        var listChunks = Lists.partition(filterKeysList, chunkSize);
        return listChunks.parallelStream().flatMap(keys -> repository.findAllById(keys).stream()).collect(Collectors.toList());
    }

    /**
     * Checks if is valid username or password.
     *
     * @param value
     *     the value
     *
     * @return true, if is valid string
     */
    public boolean isValidUsernamePwd(String value) {
        return StringUtils.isNotBlank(value);
    }

    public String calcCmHandle(String meFdn) {
        return DigestUtils.md5Hex(String.format("EricssonENMAdapter-%s", meFdn).getBytes(StandardCharsets.UTF_8)).toUpperCase(Locale.US);
    }

    /**
     * Gets the schema.
     *
     * @param schemaFile
     *     the schema file
     *
     * @return the schema
     */
    public Optional<Schema> getSchema(String schemaFile) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream nrCellDuSchemaFile = classLoader.getResourceAsStream(schemaFile);
            Schema.Parser parser = new Schema.Parser();
            Schema nrCellDuSchema = parser.parse(nrCellDuSchemaFile);
            log.trace("Schema is {}", nrCellDuSchema);
            return Optional.of(nrCellDuSchema);
        } catch (Exception e) {
            log.error("Exception getting schema for '{}', ", schemaFile, e);
        }
        log.error("No Schema found matching file '{}', ", schemaFile);
        return Optional.empty();
    }

    /**
     * Gets the kafka avro deserializer.
     *
     * @param schemaRegistryUrl
     *     the schema registry url
     * @param topicName
     *     the topic name, this is a unique identifier for schema registry, need not be an existing kafka topic, but must be unique for that schema
     *     name
     * @param schemaFilename
     *     the schema filename
     *
     * @return the kafka avro deserializer
     *
     * @throws RimHandlerException
     */
    @SuppressWarnings("deprecation")
    public KafkaAvroDeserializer getKafkaAvroDeserializer(final String schemaRegistryUrl, String topicName, String schemaFilename)
        throws RimHandlerException {
        Map<String, Object> config = new HashMap<>();
        try (KafkaAvroDeserializer kad = new KafkaAvroDeserializer()) {
            config.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
            config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
            config.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
            kad.configure(config, false);
            //TODO: Replace depreciated method.
            kad.register(topicName, getSchema(schemaFilename).orElseThrow());
            return kad;
        } catch (Exception exception) {
            log.error("ERROR ; Cannot Initialize Kafka Avro Deserializer. Failed to register Schema, Configuration properties: {}", config, exception);
            throw new RimHandlerException("Kafka Avro Deserializer Error");
        }
    }
}
