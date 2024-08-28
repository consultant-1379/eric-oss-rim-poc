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
package com.ericsson.oss.apps.model.mitigation;

import com.ericsson.oss.apps.model.mom.NRCellRelation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class CellRelationChange implements Serializable {

    private static final long serialVersionUID = 5851046571157300933L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @OneToOne(cascade = {CascadeType.MERGE})
    @AttributeOverrides(value = {
            @AttributeOverride(name = "me_fdn", column = @Column(name = "source_me_fdn")),
            @AttributeOverride(name = "ref_id", column = @Column(name = "source_ref_id"))
    })
    private NRCellRelation sourceRelation;
    @OneToOne(cascade = {CascadeType.MERGE})
    @AttributeOverrides(value = {
            @AttributeOverride(name = "me_fdn", column = @Column(name = "target_me_fdn")),
            @AttributeOverride(name = "ref_id", column = @Column(name = "target_ref_id"))
    })
    private NRCellRelation targetRelation;

    @Setter(value = AccessLevel.PRIVATE)
    private Integer originalValue;

    private Integer requiredValue;

    public CellRelationChange(NRCellRelation sourceRelation, NRCellRelation targetRelation) {
        this.sourceRelation = sourceRelation;
        this.targetRelation = targetRelation;
        this.originalValue = sourceRelation.getCellIndividualOffsetNR();
        this.requiredValue = originalValue;
    }

    private MitigationState mitigationState = MitigationState.CONFIRMED;

    private long lastChangedTimestamp = Long.MIN_VALUE;

    @Getter
    private transient Boolean changeActionReport = Boolean.TRUE;

    public void setRequiredValue(Integer requiredValue) {
        if (!(sourceRelation.getCellIndividualOffsetNR().equals(requiredValue) &&
                targetRelation.getCellIndividualOffsetNR().equals(-requiredValue))) {
            mitigationState = MitigationState.PENDING;
            changeActionReport = Boolean.TRUE;
        } else {
            mitigationState = MitigationState.CONFIRMED;
            changeActionReport = Boolean.FALSE;
        }
        this.requiredValue = requiredValue;
    }

    public void setToOriginalValue() {
        setRequiredValue(getOriginalValue());
    }

    public String toString() {
        return String.format("Source: %s target: %s required value: %d original value: %d",
                sourceRelation.getObjectId(),
                targetRelation.getObjectId(),
                getRequiredValue(),
                originalValue);
    }

}
