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
package com.ericsson.oss.apps.utils;

import static com.ericsson.oss.apps.utils.PmConstants.MODIFIED_NRECLLDU_SCHEMA;
import static com.ericsson.oss.apps.utils.PmConstants.SCHEMA_SUBJECT_RIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;

import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.ericsson.oss.apps.repositories.PmBaselineNrCellDuRepo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class UtilsTest {

    private static final int POOL_SIZE = 4;

    @Mock
    private PmBaselineNrCellDuRepo pmBaselineNrCellDuRepo;

    @Test
    @Order(1)
    @DisplayName("Expected Pass, good sleep.")
    void test_sleep() {
        final int waitTimeMs = 1000;
        final long then = System.currentTimeMillis();
        final boolean result = Utils.of().waitRetryInterval(waitTimeMs);
        final long now = System.currentTimeMillis();
        Assertions.assertTrue(result, "Expected Utils: Wait Retry Interval to sleep and return true, returned false.");
        Assertions.assertTrue((now - then) >= waitTimeMs,
                "Expected Utils: Wait Retry Interval to sleep for " + waitTimeMs + " mS. Actually waited for " + (now - then) + " mS");
    }

    @Test
    @Order(2)
    @DisplayName("Expected Fail, bad sleep.")
    void test_sleep_with_kids() {
        final boolean result = Utils.of().waitRetryInterval(-1);
        Assertions.assertFalse(result, "Expected Utils: Wait Retry Interval to sleep and return false (Interrupted), returned true.");
    }

    @Test
    void testParallelExec() {
        var threadIdSet = new HashSet<>(Utils.of().processInThreadPool(IntStream.range(0, 8).boxed().collect(Collectors.toList()), POOL_SIZE, this::getThreadIds, null));
        Assertions.assertEquals(POOL_SIZE, threadIdSet.size());
    }

    @Test
    void testParallelExecFailing() {
        var threadIdList = Utils.of().processInThreadPool(IntStream.range(0, 8).boxed().collect(Collectors.toList()), POOL_SIZE, this::breakParallelExecution, null);
        Assertions.assertTrue(threadIdList.isEmpty());
    }

    @Test
    @Order(3)
    void test_kafakAvroDeserializer() {
        // Positive test;
        assertDoesNotThrow(() -> Utils.of()
            .getKafkaAvroDeserializer("mock://testurl", SCHEMA_SUBJECT_RIM + MODIFIED_NRECLLDU_SCHEMA, MODIFIED_NRECLLDU_SCHEMA));

        //negative test
        assertThrows(RimHandlerException.class, () -> Utils.of()
            .getKafkaAvroDeserializer(null, SCHEMA_SUBJECT_RIM
                + MODIFIED_NRECLLDU_SCHEMA, MODIFIED_NRECLLDU_SCHEMA), "Expected Kafka Avro Deserializer Exception").getMessage();
    }

    List<Long> getThreadIds(Collection<Integer> inputList, Object o) {
        return inputList.parallelStream()
                .peek(x -> Utils.of().waitRetryInterval(100))
                .map(x -> Thread.currentThread()
                        .getId()).collect(Collectors.toList());
    }

    List<Long> breakParallelExecution(Collection<Integer> inputList, Object o) {
        throw new NullPointerException();
    }

    @Test
    void findByIdTest() {
        List<String> fdnList = new ArrayList<>(){{add("fnd1");add("fnd2");}};

        Utils.of().findAllById(fdnList, 1, pmBaselineNrCellDuRepo);
        Mockito.verify(pmBaselineNrCellDuRepo, Mockito.times(2)).findAllById(Mockito.anyList());
    }

    @Test
    void testMd5() {
        String cmHandle = Utils.of().calcCmHandle("SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR123gNodeBRadio00001,ManagedElement=NR123gNodeBRadio00001");
        Assertions.assertEquals("2807E2D14A39A9FEE3E04B308AA89844", cmHandle);
    }

    @Test
    void testInvalidSchema() {
        assertThat(Utils.of().getSchema("InvalidPathToShcemaFile")).isEmpty();
    }
}
