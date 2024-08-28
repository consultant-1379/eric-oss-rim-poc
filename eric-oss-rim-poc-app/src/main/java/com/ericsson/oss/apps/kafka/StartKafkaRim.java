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
package com.ericsson.oss.apps.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Start the Kafka Listener.
 *
 * Purpose of the class is (1/2/3 TODO):
 *
 * 1) Add in dependency checks for Data Catalog (in future). DC will contain the boot strap server address.
 * So RIM will need to wait for DC to be available to get the bootstrap server address.
 * Then do context refresh to re-initialize the kafka beans with the correct ( not default) address.
 *
 * 2) Allow for RIM to wait for parsers and schema Reg and Kafka (future) (dependency availability) & the existence of the counter-parser topic.
 *
 * 3) Allow for RIM to inspect the parser topic and determine the number of partitions and match the number of consumers in kafka listener to the
 * number of partitions for max efficiency
 *
 * 4) To allow for spring profiles to control when kafka listener will be started.
 */
@Lazy
@Slf4j
@Component
@ConditionalOnProperty(prefix = "spring.kafka.mode", name = "enabled", havingValue = "true")
public class StartKafkaRim {

    @Value("${spring.kafka.listenerId}")
    private String listenerId;

    @Value("${spring.kafka.topics.input.name}")
    private String counterParserTopicName;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    Environment environment;

    /**
     * Start kafka listener.
     *
     * @return true, if successful
     */
    @EventListener(value = ApplicationReadyEvent.class, condition = "@environment.getActiveProfiles()[0] == 'production'")
    public boolean startKafkaListener() {
        MessageListenerContainer messageListenerContainer = registry.getListenerContainer(listenerId);
        return startKafkaListener(messageListenerContainer, listenerId);
    }

    /**
     * Start kafka listener.
     *
     * @param messageListenerContainer
     *     the message listener container
     * @param listenerId
     *     the listener id
     *
     * @return true, if successful
     */
    public boolean startKafkaListener(final MessageListenerContainer messageListenerContainer, final String listenerId) {
        // TODO: decide if we need to fail/ wait/ retry if topic does not exist.
        log.info("Is the Parser Topic Created : " + isTopicCreated(counterParserTopicName));
        log.info("Starting Kafka Listener with listenerId '{}'", listenerId);
        if (!messageListenerContainer.isAutoStartup() && !messageListenerContainer.isRunning()) {
            messageListenerContainer.start();
            log.info("Kafka Listener with Id: {} started", listenerId);
        }
        if (messageListenerContainer.isRunning()) {
            log.info("Counter Parser Topic kafka Listener started.");
            return true;
        } else {
            log.error("Failed to start Counter Parser Topic kafka Listener.");
        }
        return false;
    }

    /**
     * Does the parser output topic exist in Kafka bootstrap server.
     *
     * @param outputTopicName
     *     The topic name to check if it has been created on kafka server
     *
     * @return boolean to indicate the creation of topics
     */
    protected boolean isTopicCreated(final String outputTopicName) {
        final AdminClient client = getAdminClient(kafkaAdmin.getConfigurationProperties().get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));

        try {
            final KafkaFuture<Set<String>> kfListTopics = client.listTopics().names();
            final Set<String> existingTopics = getFuture(kfListTopics);
            if (existingTopics == null || !existingTopics.contains(outputTopicName)) {
                log.error("Output Topic was not created: {}", outputTopicName);
                client.close();
                return false;
            }
            displayAllTopicInfo(client, existingTopics);

        } finally {
            client.close();
        }
        log.info("Output Topic was  created: {}", outputTopicName);
        return true;
    }

    private <T> T getFuture(final KafkaFuture<T> kf) {
        try {
            return kf.get();
        } catch (final ExecutionException | InterruptedException exception) {
            log.error("Error checking Output Topic creation: ", exception);
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private AdminClient getAdminClient(final Object bootstrapServers) {
        final Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(properties);
    }

    private void displayAllTopicInfo(final AdminClient client, final Set<String> existingTopics) {
        if (log.isInfoEnabled()) {
            final Map<String, KafkaFuture<TopicDescription>> map = client.describeTopics(existingTopics).topicNameValues();
            if (map != null && !map.isEmpty()) {
                for (final Entry<String, KafkaFuture<TopicDescription>> entry : map.entrySet()) {
                    final String tn = entry.getKey();
                    final KafkaFuture<TopicDescription> kfTopicDescription = entry.getValue();
                    final TopicDescription td = getFuture(kfTopicDescription);
                    log.info("ALL TOPICS:(Output Topic); topic name {} ; Description {} ", tn, td);
                }
            } else {
                log.error("No topics names and descriptions found");
            }
        }
    }
}
