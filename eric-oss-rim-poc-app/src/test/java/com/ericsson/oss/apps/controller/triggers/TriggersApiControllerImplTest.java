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

import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.data.collection.KafkaRopScheduler;
import com.ericsson.oss.apps.data.collection.RopScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {CoreApplication.class, TriggersApiControllerImpl.class},
        properties = {"spring.datasource.exposed=false"})
class TriggersApiControllerImplTest {

    @MockBean
    RopScheduler ropScheduler;

    @MockBean
    KafkaRopScheduler kafkaRopScheduler;

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mvc;
    @Autowired
    private TriggersApiControllerImpl triggersApiController;

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testPmRopsTriggered() throws Exception {
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON).content("{\"timeStamp\" : 2, \"customerId\" : \"200237\", \"nRops\" : 2}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
        checkPmRopSchedulerCalled(2, 2);
    }

    @Test
    void testPmRopTriggered() throws Exception {
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON).content("{\"timeStamp\" : 2, \"customerId\" : \"200237\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
        checkPmRopSchedulerCalled(2);
    }

    @Test
    void testPmKafkaRopTriggeredLocalhostOK() throws Exception {
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", true);
        ReflectionTestUtils.setField(triggersApiController, "bootstrapServers", "localhost");
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON).content("{\"timeStamp\" : 2, \"customerId\" : \"200237\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
        verify(kafkaRopScheduler).loadAndProcessRopDataAsync(eq(2L), eq("200237"), eq(1));
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", false);
    }

    @Test
    void testPmKafkaRopTriggeredRemoteHostBootstrapServerParserInputTopicBadRequest() throws Exception {
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", true);
        ReflectionTestUtils.setField(triggersApiController, "bootstrapServers", "eric-oss-dmm-data-message-bus-kf-client:9092");
        ReflectionTestUtils.setField(triggersApiController, "inputTopicName", "eric-oss-3gpp-pm-xml-ran-parser-nr");
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON).content("{\"timeStamp\" : 2, \"customerId\" : \"200237\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(""));
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", false);
    }

    @Test
    void testPmKafkaRopTriggeredRemoteHostBootstrapServerOK() throws Exception {
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", true);
        ReflectionTestUtils.setField(triggersApiController, "bootstrapServers", "eric-oss-dmm-data-message-bus-kf-client:9092");
        ReflectionTestUtils.setField(triggersApiController, "inputTopicName", "eric-oss-3gpp-pm-xml-ran-parser-TEST-nr");
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON).content("{\"timeStamp\" : 2, \"customerId\" : \"200237\"}"))
            .andExpect(status().isAccepted())
            .andExpect(content().string(""));
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", false);
    }

    @Test
    void testPmKafkaRopsTriggered() throws Exception {
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", true);
        ReflectionTestUtils.setField(triggersApiController, "bootstrapServers", "localhost");
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON)
            .content("{\"timeStamp\" : 2, \"customerId\" : \"200237\", \"nRops\" : 2}")).andExpect(status().isOk()).andExpect(content().string(""));
        verify(kafkaRopScheduler).loadAndProcessRopDataAsync(eq(2L), eq("200237"), eq(2));
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", false);
    }

    @Test
    void testPmKafkaRopTriggeredKafkaDisabled() throws Exception {
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", false);
        ReflectionTestUtils.setField(triggersApiController, "bootstrapServers", "localhost");
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON).content("{\"timeStamp\" : 2, \"customerId\" : \"200237\"}"))
            .andExpect(status().isCreated())
            .andExpect(content().string(""));
        checkPmRopSchedulerCalled(2);
        ReflectionTestUtils.setField(triggersApiController, "kafkaModeEnabled", false);
    }

    @Test
    void testMissingCustomerID() throws Exception {
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON).content("{\"timeStamp\" : 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    @Test
    void testDeafultTimeWarp() throws Exception {
        mvc.perform(post("/v1/trigger/pmrop").contentType(MediaType.APPLICATION_JSON).content("{\"customerId\" : \"200237\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
        checkPmRopSchedulerCalled(0);
    }

    private void checkPmRopSchedulerCalled(long timeStamp) {
        checkPmRopSchedulerCalled(timeStamp, 1);
    }

    private void checkPmRopSchedulerCalled(long timeStamp, int nRops) {
        verify(ropScheduler).loadAndProcessRopDataAsync(eq(timeStamp), eq("200237"), eq(nRops));
    }

}