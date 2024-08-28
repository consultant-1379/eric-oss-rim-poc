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
package com.ericsson.oss.apps.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class CGI {

    private int mcc;
    private int mnc;
    private long gNBId;
    private int gNBIdLength;
    private int cellLocalId;

    public long getAutonNci() {
        return gNBId * (1L << (36 - gNBIdLength)) + cellLocalId;
    }

    /**
     * builds a string representation of the node global id for this CGI
     * in the form of dash separated mcc, mnc, gNBId, gNBIdLength
     *
     * @return the NetFunctionCon string for the node this CGI belongs to
     */
    public String getNetFunctionCon() {
        return Stream.of(mcc, mnc, gNBId, gNBIdLength)
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
    }
}
