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
package com.ericsson.oss.apps.loader.schema;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

@Data
public class Geo {
    @CsvBindByName
    private String fdn;
    @CsvBindByName
    private Float bearing;
    @CsvBindByName
    private Double lat;
    @CsvBindByName
    private Double lon;
    @CsvBindByName
    private Double e_dtilts;
    @CsvBindByName
    private Double m_dtilts;
}
