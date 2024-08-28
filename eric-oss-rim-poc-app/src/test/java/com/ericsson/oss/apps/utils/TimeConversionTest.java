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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class TimeConversionTest {

    @Test
    void timeConversionTest() {
        TimeConverter tc = new TimeConverter();
        long epochMs1 = 1675350177000L;
        String ts = tc.convertEpochToTimestamp(epochMs1);
        long epochMs2 = tc.convertTimestampToEpoch(ts);
        log.info("EPOCH (ms): '{}' , TIMESTAMP: '{}' EPOCH (ms): '{}'", epochMs1, ts, epochMs2);
        assertThat(epochMs1).isEqualTo(epochMs2);
        assertThat(ts).isEqualTo("2023-02-02T15:02:57Z");
    }

    @Test
    void timeConversionGerFileFormatTest() {
        TimeConverter tc = new TimeConverter();
        long epochMs1 = 1684724400000L;
        String geoFileName1 = tc.convertEpochToGeoFileFormat(epochMs1);
        String expectedGeoFileName1 = "ww22-03.tif";
        log.info("EPOCH (ms): '{}' , geoFileName: '{}'", epochMs1, geoFileName1);
        assertThat(geoFileName1).isEqualTo(expectedGeoFileName1);

        long epochMs2 = 1684162800000L;
        String geoFileName2 = tc.convertEpochToGeoFileFormat(epochMs2);
        String expectedGeoFileName2 = "ww15-15.tif";
        log.info("EPOCH (ms): '{}' , geoFileName: '{}'", epochMs2, geoFileName2);
        assertThat(geoFileName2).isEqualTo(expectedGeoFileName2);
    }

    @Test
    void convertTimestampToEpochSuccessTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvro, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    void convertTimestampToEpochPmCounterAvroNullTest() {
        TimeConverter tc = new TimeConverter();
        Optional<Long> result = tc.convertTimestampToEpoch(null, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void convertTimestampToEpochOldRopTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvro, 0L);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void convertTimestampToEpochRopEndTimeNullTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.setRopEndTime(null);
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvro, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void convertTimestampToEpochRopEndTimeZeroTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.setRopEndTime("");
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvro, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void convertTimestampToEpochRopEndTimeInvalidFormatTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.setRopEndTime("2023-02-02T15:02:57");
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvro, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void convertTimestampToEpochRopEndTimeMinusOffsetSuccessTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.setRopEndTime("2023-02-02T15:02:57-00:00");
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvro, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    void convertTimestampToEpochRopEndTimeNegativeEpochTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.setRopEndTime("1900-02-02T15:02:57+00:00");
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvro, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void convertTimestampToEpochRopEndTimeZSuccessTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvro = TestUtils.of().getOnePmAvroCountersRecord();
        pmCounterAvro.setRopEndTime("2023-02-02T15:02:57Z");
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvro, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    void convertTimestampToEpochThrowExceptionTest() {
        TimeConverter tc = new TimeConverter();
        NRCellDU_GNBDU_1 pmCounterAvroMock = Mockito.mock(NRCellDU_GNBDU_1.class);
        Mockito.when(pmCounterAvroMock.getRopEndTime()).thenThrow(new RimHandlerException("Test TimeConverter Exception"));
        Optional<Long> result = tc.convertTimestampToEpoch(pmCounterAvroMock, PmConstants.OLD_ROP_100_YEARS);
        assertThat(result.isEmpty()).isTrue();
    }
}
