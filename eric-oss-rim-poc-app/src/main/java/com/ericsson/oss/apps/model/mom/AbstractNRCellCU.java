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
import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "NRCELLCU", indexes = {
        @Index(name = "idx_abstractnrcellcu", columnList = "me_fdn, parent_ref_id")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class AbstractNRCellCU extends NRCell {

    private static final long serialVersionUID = -7199152085938550858L;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumnsOrFormulas(value = {
        @JoinColumnOrFormula(formula = @JoinFormula(value = "me_fdn", referencedColumnName = "me_fdn")),
        @JoinColumnOrFormula(formula = @JoinFormula(value = "parent_ref_id", referencedColumnName = "ref_id"))
    })
    private GNodeB node;

    AbstractNRCellCU(String fdn) {
        super(fdn);
    }

    @JsonIgnore
    public CGI getCGI() {
        PLMNId relatedPLMN = node.getPLMNId();
        return CGI.builder()
            .cellLocalId(cellLocalId)
            .gNBId(node.getGNBId())
            .gNBIdLength(node.getGNBIdLength())
            .mcc(relatedPLMN.getMcc())
            .mnc(relatedPLMN.getMnc())
            .build();
    }
}
