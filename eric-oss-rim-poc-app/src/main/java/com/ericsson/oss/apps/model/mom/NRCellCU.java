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

import javax.persistence.Entity;
import javax.persistence.OneToOne;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class NRCellCU extends AbstractNRCellCU {

    private static final long serialVersionUID = -6216782776809799581L;

    @JsonProperty("nCI")
    private Long nCI;
    @JsonProperty(value = "nRFrequencyRef", access = JsonProperty.Access.WRITE_ONLY)
    @OneToOne(targetEntity = NRFrequency.class)
    private NRFrequency nRFrequency;

    public NRCellCU(String fdn) {
        super(fdn);
    }
}
