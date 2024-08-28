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
package com.ericsson.oss.apps.config;

import static com.ericsson.oss.apps.utils.PmConstants.MODIFIED_NRECLLDU_SCHEMA;
import static com.ericsson.oss.apps.utils.PmConstants.SCHEMA_SUBJECT_RIM;

import java.util.HashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.ericsson.oss.apps.utils.Utils;

/**
 * The Class KafkaConfig.
 * This holds the Producer and Consumer and other related beans for Kafka.
 *
 */
@Slf4j
@Lazy
@Configuration
@Component
@ToString
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.kafka.mode", name = "enabled", havingValue = "true")
public class KafkaConfigRim {

    private final MeterRegistry meterRegistry;
    private final KafkaConfigRimProducerData pp;
    private final KafkaConfigRimConsumerData cp;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapServer;

    @Value("${spring.kafka.schema-registry.url}")
    private String schemaRegistryUrl;

    @Value(value = "${spring.kafka.backoff.interval-ms}")
    private int retryInterval;

    @Value(value = "${spring.kafka.backoff.max-attempts}")
    private int retryMax;

    /**
     * Concurrent kafka listener container factory.
     *
     * @return the kafka listener container factory
     */
    @java.lang.SuppressWarnings("squid:S4449")
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, byte[]>> concurrentKafkaListenerContainerFactory() {
        logWithBanner("Initializing kafka Listener Container Factory");
        final ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(AckMode.BATCH); // Requested commit mode; Accept or reject entire batch.
        factory.setCommonErrorHandler(errorHandler());
        factory.setBatchListener(true);
        return factory;
    }

    /**
     * Initialize the kafka avro Deserializer used in the consumer.
     *
     * @return an instance of the kafka avro deserializer
     *
     * @throws RimHandlerException
     */
    @Bean(destroyMethod = "close")
    @Scope("singleton")
    public KafkaAvroDeserializer kad() throws RimHandlerException {
        logWithBanner("Initializing kafka Avro Deserializer");
        return Utils.of().getKafkaAvroDeserializer(schemaRegistryUrl, SCHEMA_SUBJECT_RIM + MODIFIED_NRECLLDU_SCHEMA, MODIFIED_NRECLLDU_SCHEMA);
    }
    /**
     * Error handler.
     *
     * @return the default error handler
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        BackOff fixedBackOff = new FixedBackOff(retryInterval, retryMax);
        return new DefaultErrorHandler(
            (consumerRecord, exception) -> log.error("Failed to consume record {}, Cause {} ", consumerRecord, exception.getMessage(), exception),
            fixedBackOff);
    }

    /**
     * Kafka admin.
     *
     * @return the kafka admin
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        logWithBanner("Initializing kafka Admin - 1");
        final Map<String, Object> adminConfig = new HashMap<>();
        adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServer());
        return new KafkaAdmin(adminConfig);
    }

    /**
     * Create a consumer factory with customized configuration.
     *
     * @return the consumer factory.
     */
    @Bean
    @Scope("singleton")
    public ConsumerFactory<String, byte[]> consumerFactory() {
        logWithBanner("Initializing Consumer Factory");
        logWithBanner(this.toString());
        final DefaultKafkaConsumerFactory<String, byte[]> factory = new DefaultKafkaConsumerFactory<>(consumerConfigs(getBootstrapServer()));
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return factory;
    }

    // https://docs.spring.io/spring-kafka/reference/html/#committing-offsets
    // The default AckMode is BATCH. Starting with version 2.3, the framework sets enable.auto.commit to false.
    // So these parameters not set here.
    // Discussion on Kafka consumer modes: https://jira-oss.seli.wh.rnd.internal.ericsson.com/browse/IDUN-55797

    //TODO: Some of these parameters can directly be set in application.yaml and directly overwrite spring-kafka. Format of how to do this need to be investigated.
    private Map<String, Object> consumerConfigs(final String requestedBootStrapServer) {
        logWithBanner(cp.toString());
        final Map<String, Object> props = new HashMap<>(12);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, requestedBootStrapServer);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, cp.getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, cp.getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // batch mode commit; false by default in version >= 2.3
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, cp.getSessionTimeoutMs());
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, cp.getMaxPollReconnectTimeoutMs());

        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, cp.getPartitionAssignmentStrategy());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, cp.getMaxPollRecords());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, cp.getMaxPollIntervalMs());
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // Using a value of read_committed ensures that we don't read any transactional messages before the transaction completes.

        addConsumerRobustnessValues(props);
        return props;
    }

    /**
     * TODO: Make a query to Data Catalog to fetch the Kafka Access Endpoints
     *
     * @return bootstrap server url & port.
     */
    @Bean
    protected String getBootstrapServer() {
        logWithBanner("KAFKA BOOTSTRAP SERVER: " + bootStrapServer);
        return bootStrapServer;
    }

    /**
     * TODO: Wait on schema Registry Availability
     *
     * @return schema registry url & port.
     */
    @Bean
    protected String getSchemaRegistryUrl() {
        logWithBanner("SCHEMA-REGISTRY SERVER: " + schemaRegistryUrl);
        return schemaRegistryUrl;
    }

    private void logWithBanner(final String msg) {
        log.info("-----------------------------------------------------------------------");
        log.info("KAFKA_CONFIG: {}", msg);
        log.info("-----------------------------------------------------------------------");
    }

    /**
     * Robustness/Retry values for consumer.
     *
     * @param config
     *            Kafka Config {@link Map}
     */
    private void addConsumerRobustnessValues(final Map<String, Object> config) {
        config.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, cp.getRetryBackoffMs());
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, cp.getReconnectBackoffMs());
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, cp.getReconnectBackoffMaxMs());
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, cp.getRequestTimeoutMs());
    }

    /**
     * Producer factory.
     *
     * @return the producer factory
     */
    @Bean
    @Scope("singleton")
    public ProducerFactory<String, GenericRecord> producerFactory() {
        logWithBanner("Initializing Producer Factory");
        final Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServer());
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        configs.put(ProducerConfig.ACKS_CONFIG, pp.getAcksConfig());
        configs.put(ProducerConfig.RETRIES_CONFIG, pp.getRetriesConfig());  // default to 0; => Idempotence will be disabled because retries is set to 0.  But ensures no duplicate messages
        configs.put(ProducerConfig.BATCH_SIZE_CONFIG, pp.getBatchSizeConfig()); //default 16384
        configs.put(ProducerConfig.LINGER_MS_CONFIG, pp.getLingerMsConfig()); //default 0
        configs.put(ProducerConfig.BUFFER_MEMORY_CONFIG, pp.getBufferMemoryConfig()); //default 33554432
        configs.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, pp.getMaxInFlightRequestsPerConnection());
        // Added on version 5.3.0 for test- use of "mock://testurl". Consumer and producer must be SAME.
        configs.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, getSchemaRegistryUrl());
        configs.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);

        final ProducerFactory<String, GenericRecord> pf = new DefaultKafkaProducerFactory<>(configs);
        pf.addListener(new MicrometerProducerListener<>(meterRegistry));
        return pf;
    }

    /**
     * Kafka output template.
     *
     * @return the kafka template
     */
    @Bean
    public KafkaTemplate<String, GenericRecord> kafkaOutputTemplate() {
        logWithBanner(pp.toString());
        return new KafkaTemplate<>(producerFactory(), false);
    }
}
