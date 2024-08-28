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
package com.ericsson.oss.apps.data.collection.pmrop.counters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.utils.TestUtils;

import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;

class PmRopNRCellDUTest {

    private static final double EXPECTED_AVG_DELTA_IPN = 6.290251971822517;
    @BeforeAll
    static void setUpBeforeClass() throws Exception {
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
    }

    @BeforeEach
    void setUp() throws Exception {
    }

    @AfterEach
    void tearDown() throws Exception {
    }

    @Test
    void calculateAvgDeltaIpnUsingDefaultValuesTest() {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();
        assertEquals(EXPECTED_AVG_DELTA_IPN, pmRopNRCellDU.getAvgDeltaIpN(), 0.000001);
    }

    @Test
    void calculateAvgDeltaIpnArrayOfNullsTest() {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();
        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(new ArrayList<>(Arrays.asList(null, null, null, null, null)));
        assertEquals(Double.NaN, pmRopNRCellDU.getAvgDeltaIpN());
    }

    @Test
    void calculateAvgDeltaIpnNullTest() {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();
        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(null);
        assertEquals(Double.NaN, pmRopNRCellDU.getAvgDeltaIpN());
    }

    @Test
    void calculateAvgDeltaIpnCounterEmptyTest() {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();
        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(new ArrayList<>());
        assertEquals(Double.NaN, pmRopNRCellDU.getAvgDeltaIpN());
    }

    @Test
    void calculateAvgDeltaIpnZeroTest() {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();
        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(new ArrayList<>(Arrays.asList(0L, 0L, 0L, 0L, 0L)));
        assertEquals(Double.NaN, pmRopNRCellDU.getAvgDeltaIpN());
    }

    @Test
    void calculateAvgDeltaIpnInvalidTest() {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();
        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(new ArrayList<>(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L)));
        assertEquals(Double.NaN, pmRopNRCellDU.getAvgDeltaIpN());
    }

    @ParameterizedTest
    @CsvSource(value = { "10 , 5, 3, 2, 10, 8.163929" }, nullValues = { "null" })
    void calculateAvgDeltaIpnTest(Long deltaIpn1, Long deltaIpn2, Long deltaIpn3, Long deltaIpn4, Long deltaIpn5, double expectedResult) {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();

        assertEquals(EXPECTED_AVG_DELTA_IPN, pmRopNRCellDU.getAvgDeltaIpN(), 0.000001);

        pmRopNRCellDU.setPmRadioMaxDeltaIpNDistr(new ArrayList<>(Arrays.asList(deltaIpn1, deltaIpn2, deltaIpn3, deltaIpn4, deltaIpn5)));
        assertEquals(expectedResult, pmRopNRCellDU.getAvgDeltaIpN(), 0.000001);
    }

    @Test
    void calculateAvgUlUeTpUsingDefaultValuesTest() {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();
        Double pmMacVolUlResUe = pmRopNRCellDU.getPmMacVolUlResUe();
        Double pmMacTimeUlResUe = pmRopNRCellDU.getPmMacTimeUlResUe();

        Double avgUlUeTp = 64 * (pmMacVolUlResUe / pmMacTimeUlResUe);

        assertEquals(avgUlUeTp, pmRopNRCellDU.getAvgUlUeTp(), 0.000001);
    }

    @Test
    void calculateAvgUlUeTpNullTest() {
        PmRopNRCellDU pmRopNRCellDU = getPmRopNRCellDU();
        pmRopNRCellDU.setPmMacVolUlResUe(null);

        assertEquals(Double.NaN, pmRopNRCellDU.getAvgUlUeTp());

        pmRopNRCellDU.setPmMacVolUlResUe(1.0);
        pmRopNRCellDU.setPmMacTimeUlResUe(null);
        assertEquals(Double.NaN, pmRopNRCellDU.getAvgUlUeTp());

        pmRopNRCellDU.setPmMacTimeUlResUe(0.0);
        assertEquals(Double.NaN, pmRopNRCellDU.getAvgUlUeTp());
    }

    //Basic Sonar Coverage
    @Test
    void pmRopNRCellDUBasicSonartests() {
        PmRopNRCellDU pmRopNRCellDU1 = getPmRopNRCellDU();
        PmRopNRCellDU pmRopNRCellDU2 = getPmRopNRCellDU();
        assertThat(pmRopNRCellDU1).isEqualTo(pmRopNRCellDU1);
        assertThat(pmRopNRCellDU1).isEqualTo(pmRopNRCellDU2);
        assertThat(pmRopNRCellDU1).isNotEqualTo(null);
        assertThat(pmRopNRCellDU1).isNotEqualTo(new NRCellDU_GNBDU_1());

        MoRopId moRopId = new MoRopId("fdn", 0L);
        pmRopNRCellDU1.setMoRopId(moRopId);
        assertThat(pmRopNRCellDU1).isNotEqualTo(pmRopNRCellDU2);
    }

    private PmRopNRCellDU getPmRopNRCellDU() {
        return TestUtils.of().createPmRopNrCellDU(0);
    }

}
