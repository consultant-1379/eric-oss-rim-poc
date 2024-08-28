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

package com.ericsson.oss.apps.controller.triggers;

import com.ericsson.oss.apps.api.controller.TriggersApi;
import com.ericsson.oss.apps.api.model.controller.PmRopTriggerRequest;
import com.ericsson.oss.apps.data.collection.KafkaRopScheduler;
import com.ericsson.oss.apps.data.collection.RopScheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TriggersApiControllerImpl implements TriggersApi {

    @Autowired
    RopScheduler ropScheduler;

    @Autowired
    KafkaRopScheduler kafkaRopScheduler;

    @Value("${spring.kafka.mode.enabled}")
    private boolean kafkaModeEnabled;

    @Value("${spring.kafka.bootstrap-servers}")
    protected String bootstrapServers;

    @Value("${spring.kafka.topics.input.name}")
    protected String inputTopicName;

    private String[] parserTopics = { "eric-oss-3gpp-pm-xml-ran-parser-nr",
        "eric-oss-3gpp-pm-xml-ran-parser-lte", "eric-oss-3gpp-pm-xml-ran-parser-logbook" };

    @Override
    public ResponseEntity<Void> triggerPmRopLoading(PmRopTriggerRequest ericOssRimPocPmRopTriggerRequest) {
        Long timeStamp = ericOssRimPocPmRopTriggerRequest.getTimeStamp();
        String customerId = ericOssRimPocPmRopTriggerRequest.getCustomerId();
        Integer nRops = ericOssRimPocPmRopTriggerRequest.getnRops();
        log.info(String.format("Request received for timeStamp %d and customer id %s and %s rops, kafkaEnabled = %s, bootstrapServers = %s",
            timeStamp, customerId, nRops, kafkaModeEnabled, bootstrapServers));
        /*
           Following modes are allowable for testing kafka (inject ROPS from CSV files to Kafka topic
            - bootstrapServers = localhost && inputTopicName = < what ever u wish>
            - bootstrapServers = <remote/cluster bootstrap server> ( eric-oss-dmm-data-message-bus-kf-client:9092()
                               && inputTopicName != <topic used by 3gpp-parsers> (eric-oss-3gpp-pm-xml-ran-parser-nr)

            So only way bootstrapServers = <remote/cluster bootstrap server> &&  inputTopicName == <topic used by 3gpp-parsers> is via @scheduled in kafkaRopScheduler
            This is to say, RIM should not INJECT ROPS onto the remote topic used by 3gpp-parsers.
         */
        if (kafkaModeEnabled && bootstrapServers.contains("localhost")) {
            log.info(String.format("TESTING With KAFKA ENABLED - local bootstrap,  bootstrapServer = %s, topic %s", inputTopicName, bootstrapServers));
            kafkaRopScheduler.loadAndProcessRopDataAsync(timeStamp, customerId, nRops);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        if (kafkaModeEnabled && !bootstrapServers.contains("localhost") && Arrays.asList(parserTopics).contains(inputTopicName)) {
            log.error(String.format("TESTING With KAFKA ENABLED, bootstrapServers must be 'localhost' or if not local, then topic name %s  must not be one of %s,  bootstrapServers = %s",
                inputTopicName, Arrays.asList(parserTopics), bootstrapServers));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (kafkaModeEnabled) {
            log.info(String.format("TESTING With KAFKA ENABLED - remote bootstrap,  bootstrapServer = %s, topic %s", inputTopicName, bootstrapServers));
            kafkaRopScheduler.loadAndProcessRopDataAsync(timeStamp, customerId, nRops);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        }
        ropScheduler.loadAndProcessRopDataAsync(timeStamp, customerId, nRops);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
