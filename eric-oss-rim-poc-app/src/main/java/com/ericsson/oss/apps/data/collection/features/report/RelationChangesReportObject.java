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
package com.ericsson.oss.apps.data.collection.features.report;

import lombok.Setter;
import lombok.Getter;
import com.opencsv.bean.CsvBindByName;
import lombok.NoArgsConstructor;

@Setter
@Getter
@NoArgsConstructor
public class RelationChangesReportObject {
    @CsvBindByName
    private String sourceRelation;
    @CsvBindByName
    private String targetRelation;
    @CsvBindByName
    private Integer initialValue;
}
