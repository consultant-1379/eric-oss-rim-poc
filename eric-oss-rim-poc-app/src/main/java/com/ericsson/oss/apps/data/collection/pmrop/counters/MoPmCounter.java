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
package com.ericsson.oss.apps.data.collection.pmrop.counters;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Class MoPMCounter, used in kafka producer to hold the counter values read in from PmRopNrcellDu via CSV file.
 * These will then be processed into generic records and set to kafka topic for consumption.
 */

@Builder
@Getter
@Setter
@ToString
public class MoPmCounter {
    private String counterName;
    private String counterType;
    @Builder.Default
    private long pmCounterValue = Long.MIN_VALUE;
    private List<Long> pmCounterValues;
    private boolean isValuePresent;
}
