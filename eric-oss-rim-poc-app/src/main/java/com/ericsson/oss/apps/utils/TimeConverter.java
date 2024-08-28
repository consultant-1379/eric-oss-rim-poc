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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class TimeConverter.
 */
@Slf4j
@Component
@NoArgsConstructor
public class TimeConverter {

    //Note: Regex is slow.
    private static final Pattern ROP_DATE_TIME_EXPRESSION = Pattern
        .compile("^(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)T(\\d\\d):(\\d\\d):(\\d\\d)[Z\\+\\-](\\d\\d)*:*(\\d\\d)*");
    private static final String LOG_MESSAGE = "TimeConverter: Error Processing PM Counter (Avro Deserialization Error; %s ) for record with fdn %s, Record = %s";

    public static TimeConverter of() {
        return new TimeConverter();
    }

    /**
     * Convert EPOCH in mS to time stamp similar of format to "2021-02-24T03:30:00Z".
     *
     * @param epocTimeMs
     *
     * @return
     */
    public String convertEpochToTimestamp(long epocTimeMs) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(epocTimeMs), ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
        return date.format(formatter);
    }

    /**
     * Convert EPOCH in mS to file format like  "ww${DAY}-${HOUR}.tif".
     *
     * @param epocTimeMs
     *
     * @return filename for geoData download
     */
    public String convertEpochToGeoFileFormat(long epocTimeMs) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(epocTimeMs), ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'ww'dd'-'HH'.tif'", Locale.ENGLISH);
        return date.format(formatter);
    }

    /**
     * Convert String time stamp of similar format to "2021-02-24T03:30:00Z" to EPOCH in mS.
     *
     * @param timestamp
     *
     * @return
     */
    public long convertTimestampToEpoch(String timestamp) {
        OffsetDateTime date = OffsetDateTime.parse(timestamp);
        return date.toInstant().toEpochMilli();
    }

    /**
     * Convert String time stamp of similar format to "2021-02-24T03:30:00Z" to EPOCH in mS.
     * Perform check on the pmAvro and ropEndTime to ensure its valid before processing.
     *
     * @param pmCounterAvro
     *     the PM Counter Avrop record with the ropEndTime .
     * @param discardRopsOlderThanMs
     *     records with repEndTime older then this will be discarded.
     *
     * @return
     */
    public Optional<Long> convertTimestampToEpoch(final NRCellDU_GNBDU_1 pmCounterAvro, long discardRopsOlderThanMs) {
        try {
            if (pmCounterAvro == null) {
                log.warn("{}", String.format(LOG_MESSAGE, "pmCounterAvro is null", "NO_FDN", "NO_PM_COUNTER"));
                return Optional.empty();
            }
            if (pmCounterAvro.getRopEndTime() == null || pmCounterAvro.getRopEndTime().length() == 0) {
                log.warn("{}", String
                    .format(LOG_MESSAGE, "pmCounterAvro 'ropEndTime' is null or zero length", pmCounterAvro.getNodeFDN(), pmCounterAvro));
                return Optional.empty();
            }
            if (!ROP_DATE_TIME_EXPRESSION.matcher(pmCounterAvro.getRopEndTime()).matches()) {
                String faultMessage = String.format("Rop end time in record does not match required format of "
                    + "'yyy-MM-dd'T'HH:mm:ss'Z', RopEndTime is '%s'", pmCounterAvro.getRopEndTime().toString());
                log.warn("{}", String.format(LOG_MESSAGE, faultMessage, pmCounterAvro.getNodeFDN(), pmCounterAvro));
                return Optional.empty();
            }
            long ropEndTimeEpoch = this.convertTimestampToEpoch(pmCounterAvro.getRopEndTime().toString());
            if (ropEndTimeEpoch <= 0) {
                String faultMessage = String.format("Epoch of Rop end time is negative or zero in record, RopEndTime is '%s' (EPOCH = '%d')",
                    pmCounterAvro.getRopEndTime().toString(), ropEndTimeEpoch);
                log.warn("{}", String.format(LOG_MESSAGE, faultMessage, pmCounterAvro.getNodeFDN(), pmCounterAvro));
                return Optional.empty();
            }
            return isRopEndTimeValid(pmCounterAvro, ropEndTimeEpoch, discardRopsOlderThanMs);

        } catch (final Exception exception) {
            log.error("Counter Listener: Processing error: Failure Deserializing Record : {}, {} ",
                pmCounterAvro == null ? "noValue" : pmCounterAvro.getNodeFDN(),
                    exception.getMessage(), exception);
            return Optional.empty();
        }
    }

    private Optional<Long> isRopEndTimeValid(final NRCellDU_GNBDU_1 pmCounterAvro, long ropEndTimeEpoch, long discardRopsOlderThanMs) {
        long discardRopsOlderThanEpochMs = Instant.now().toEpochMilli() - discardRopsOlderThanMs;

        if (ropEndTimeEpoch < (Instant.now().toEpochMilli() - discardRopsOlderThanMs)) {
            String faultMessage = String.format("Configured to drop records with RopEndTime older than %d mS, this record has RopEndTime of %d mS (%s) < %d",
                discardRopsOlderThanMs, ropEndTimeEpoch, pmCounterAvro.getRopEndTime().toString(), discardRopsOlderThanEpochMs);
            log.warn("{}", String.format(LOG_MESSAGE, faultMessage, pmCounterAvro.getNodeFDN(), pmCounterAvro));
            return Optional.empty();
        }
        return Optional.of(ropEndTimeEpoch);
    }
}
