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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.CsvRecurse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class ManagedObject implements Serializable {

    private static final long serialVersionUID = -2253865270086823832L;

    public enum AdministrativeState {
        LOCKED,
        UNLOCKED,
        SHUTTINGDOWN
    }

    public enum Toggle {
        DISABLED,
        ENABLED
    }

    @CsvRecurse
    @EmbeddedId
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @EqualsAndHashCode.Include
    private ManagedObjectId objectId;

    @Column(name = "parent_ref_id")
    private String parentRefId;

    ManagedObject(String fdn) {
        objectId = ManagedObjectId.of(fdn);
    }

    @PreUpdate
    @PrePersist
    public void calc() {
        parentRefId = objectId.fetchParentId().getRefId();
    }

    public String toFdn()
    {
        return this.getObjectId().toFdn();
    }
}
