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
import lombok.*;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "GNodeB", indexes = {
        @Index(name = "idx_gnodeb_gnbid", columnList = "gNBId")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class GNodeB extends ManagedObject {

    private static final long serialVersionUID = -7199153485938550858L;

    @JsonProperty(value = "gNBId")
    private Long gNBId;
    @JsonProperty(value = "gNBIdLength")
    private Integer gNBIdLength;
    @Embedded
    @JsonProperty(value = "pLMNId")
    private PLMNId pLMNId;

    GNodeB(String fdn) {
        super(fdn);
    }
}
