/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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
package com.ericsson.oss.apps.model.mom;

import com.ericsson.oss.apps.model.CGI;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true, includeFieldNames = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class NRCellRelation extends ManagedObject {

    private static final long serialVersionUID = 4116157051366354856L;

    private Integer cellIndividualOffsetNR;

    // this avoids confusion between lombok
    // and jackson serialization
    @JsonProperty("isHoAllowed")
    private Boolean hoAllowed;

    public void isHoAllowed(Boolean isHoAllowed) {
        this.hoAllowed = isHoAllowed;
    }

    public Boolean isHoAllowed() {
        return hoAllowed;
    }

    @Embedded
    @JsonProperty(value = "nRCellRef", access = JsonProperty.Access.WRITE_ONLY)
    @AttributeOverrides(value = {
            @AttributeOverride(name = "meFdn", column = @Column(name = "nrcell_me_fdn")),
            @AttributeOverride(name = "refId", column = @Column(name = "nrcell_ref_id"))
    })
    private ManagedObjectId nRCellRef;

    @JsonIgnore
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumnsOrFormulas(value = {
            @JoinColumnOrFormula(formula = @JoinFormula(value = "me_fdn", referencedColumnName = "me_fdn")),
            @JoinColumnOrFormula(formula = @JoinFormula(value = "parent_ref_id", referencedColumnName = "ref_id"))
    })
    private NRCellCU cell;

    @JsonIgnore
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumnsOrFormulas(value = {
            @JoinColumnOrFormula(formula = @JoinFormula(value = "nrcell_me_fdn", referencedColumnName = "me_fdn")),
            @JoinColumnOrFormula(formula = @JoinFormula(value = "nrcell_ref_id", referencedColumnName = "ref_id"))
    })
    private AbstractNRCellCU targetCell;

    public NRCellRelation(String fdn) {
        super(fdn);
    }

    @JsonIgnore
    public CGI getSourceCGI() {
        return cell.getCGI();
    }

    @JsonIgnore
    public CGI getTargetCGI() {
        return targetCell.getCGI();
    }
}
