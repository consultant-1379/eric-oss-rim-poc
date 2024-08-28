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

import static com.ericsson.oss.apps.utils.PmConstants.MODIFIED_NRECLLDU_SCHEMA;
import static com.ericsson.oss.apps.utils.PmConstants.SCHEMA_SUBJECT_RIM;
import static com.ericsson.oss.apps.utils.TestUtils.EP_XnU_GNBCUUP_1_SCHEMA;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;

import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.kafka.KafkaProducerRim;
import com.ericsson.oss.apps.utils.PmRopNrCellDuCreator;
import com.ericsson.oss.apps.utils.TestUtils;
import com.ericsson.oss.apps.utils.TimeConverter;
import com.ericsson.oss.apps.utils.Utils;

import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class KafkaRimConsumerBasicTest.
 * Just simple test without the need to load spring.
 */
@Slf4j
class KafkaRimConsumerBasicTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(KafkaProducerRim.class);
    private String schemaRegistryMockUrl = "mock://testurl";

    @BeforeEach
    public void init() {
        logger.setLevel(Level.TRACE);
    }

    @AfterEach
    public void cleanup() {
        logger.setLevel(Level.INFO);
    }

    @Test
    void testGrLoaderWithNrCellDuSchemaIsCreatedSuccessfully() {
        KafkaProducerRim kafkaProducerRim = new KafkaProducerRim();
        PmRopNRCellDU pmRopNRCellDU = TestUtils.of().createPmRopNrCellDU(1);
        Optional<GenericRecord> grOpt = kafkaProducerRim.createProducerMessagesGenericRecord(pmRopNRCellDU,
            Utils.of().getSchema(MODIFIED_NRECLLDU_SCHEMA).get());
        NRCellDU_GNBDU_1 pmCounterAvro = (NRCellDU_GNBDU_1) TestUtils.of()
            .convertViaAvro(grOpt.get(), schemaRegistryMockUrl, SCHEMA_SUBJECT_RIM + MODIFIED_NRECLLDU_SCHEMA, MODIFIED_NRECLLDU_SCHEMA);
        long ropEndTimeEpoch = TimeConverter.of().convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
        PmRopNRCellDU actualPmRopNRCellDU = PmRopNrCellDuCreator.of().createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch);
        assertTrue(TestUtils.of().isPmRopNrCellDUEqual(actualPmRopNRCellDU, pmRopNRCellDU));
    }

    /**
     * Create a generic record from EP_XnU_GNBCUUP_1 schema and assert that the fields are created correctly.
     */
    @Test
    void testGrLoaderWithNonNrCellDuSchemaIsCreatedSuccessfully() {
        logger.setLevel(Level.INFO);
        KafkaProducerRim kafkaProducerRim = new KafkaProducerRim();
        PmRopNRCellDU pmRopNRCellDU = TestUtils.of().createPmRopNrCellDU(1);
        Optional<GenericRecord> grOpt = kafkaProducerRim
            .createProducerMessagesGenericRecord(pmRopNRCellDU, Utils.of().getSchema(EP_XnU_GNBCUUP_1_SCHEMA).get());

        if (grOpt.isEmpty()) {
            fail("No Generic Record Created");
        }
        log.info("-------------------------------------------------------");
        StringBuilder sb = new StringBuilder();
        grOpt.get()
            .getSchema()
            .getFields()
            .forEach(field -> sb.append("EP_XnU_GNBCUUP - ").append(field.name()).append(":").append(grOpt.get().get(field.name())).append("\n"));
        log.info("\n{}", sb.toString());

        log.info("-------------------------------------------------------");
        sb.setLength(0);

        assertEquals("fdn-1", grOpt.get().get("nodeFDN").toString());
        assertEquals("XML", grOpt.get().get("elementType").toString());
        assertEquals("2021-02-24T03:30:00Z", grOpt.get().get("ropBeginTime").toString());
        assertEquals("2021-02-24T03:45:00Z", grOpt.get().get("ropEndTime").toString());
        assertEquals("noValue", grOpt.get().get("moFdn").toString());
        assertFalse(((boolean) grOpt.get().get("suspect")));
        assertEquals("{\"pmPdcpPktFwdTransDlQos\": {\"counterType\": \"null\", \"counterValue\": [], \"isValuePresent\": false}, \"pmPdcpPktFwdTransDlDiscQos\": {\"counterType\": \"null\", \"counterValue\": [], \"isValuePresent\": false}}", grOpt.get().get("pmCounters").toString());
    }
}

