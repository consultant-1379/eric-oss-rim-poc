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
package com.ericsson.oss.apps.data.collection.features.report.mobility;

import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MobilityReportingStatus {
    @CsvBindByName
    private String victimCellFdn;
    @CsvBindByName
    @Setter
    private String neighborCellFdn;
    private NRCellCU neighborCellCu;
    @CsvBindByName
    private Integer currentCio;
    @CsvBindByName
    private Integer requiredCio;
    @CsvBindByName
    private Integer originalCio;
    @CsvBindByName
    private String sourceRelationFdn;
    @CsvBindByName
    private String targetRelationFdn;
    @CsvBindByName
    @Setter
    private MitigationState mitigationState;
    @Setter
    @CsvBindByName
    Long changedTimestamp;
    @Setter
    @CsvBindByName
    Long currentTimestamp;

    public MobilityReportingStatus(long ropTimeStamp, String victimCellFdn, CellRelationChange relationChange) {
        this.victimCellFdn = victimCellFdn;
        this.mitigationState = relationChange.getMitigationState();
        this.originalCio = relationChange.getOriginalValue();
        this.requiredCio = relationChange.getRequiredValue();
        this.currentCio = relationChange.getSourceRelation().getCellIndividualOffsetNR();
        this.currentTimestamp = ropTimeStamp;
        this.changedTimestamp = relationChange.getLastChangedTimestamp();
        this.neighborCellCu = relationChange.getTargetRelation().getCell();
        this.sourceRelationFdn = relationChange.getSourceRelation().getObjectId().toString();
        this.targetRelationFdn = relationChange.getTargetRelation().getObjectId().toString();

    }

}
