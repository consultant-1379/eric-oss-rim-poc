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
package com.ericsson.oss.apps.data.collection.features.report.uplinkpower;

import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.opencsv.bean.CsvBindByName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class UplinkPowerReportingStatus {
    @CsvBindByName
    private String cellFdn;
    @CsvBindByName
    private String requesterVictimCellFdn;
    @CsvBindByName
    private boolean isVictim;

    @CsvBindByName
    private int currentPZeroNomPuschGrant;
    @CsvBindByName
    private int currentPZeroUePuschOffset256Qam;
    @CsvBindByName
    private Integer requiredPZeroNomPuschGrant;
    @CsvBindByName
    private Integer requiredZeroUePuschOffset256Qam;
    @CsvBindByName
    private int originalPZeroNomPuschGrant;
    @CsvBindByName
    private int originalPZeroUePuschOffset256Qam;

    @Setter
    @CsvBindByName
    private Integer requestedPZeroNomPuschGrant;
    @Setter
    @CsvBindByName
    private Integer requestedZeroUePuschOffset256Qam;
    @Setter
    @CsvBindByName
    private MitigationState mitigationState;
    @Setter
    @CsvBindByName
    Long changedTimestamp;
    @Setter
    @CsvBindByName
    Long currentTimestamp;
}
