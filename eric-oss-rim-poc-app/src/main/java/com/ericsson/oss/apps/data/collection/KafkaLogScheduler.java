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


package com.ericsson.oss.apps.data.collection;

import com.ericsson.oss.apps.kafka.CounterParserTopicListenerRim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "spring.kafka.mode", name = "enabled", havingValue = "true")
public class KafkaLogScheduler {

    @Autowired
    CounterParserTopicListenerRim counterParserTopicListenerRim;

    @Scheduled(fixedDelay = 60000)
    public void getCounterParserMetrics() {
        counterParserTopicListenerRim.getLogs();
    }

}