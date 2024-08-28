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
package com.ericsson.oss.apps.model.mom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.Entity;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class NRSectorCarrier extends ManagedObject {

    private static final long serialVersionUID = 4177270645822367105L;
    public NRSectorCarrier(String fdn) {
        super(fdn);
    }

    private AdministrativeState administrativeState;
    private Integer arfcnDL;
    private Integer arfcnUL;
    @JsonProperty("bSChannelBwDL")
    private Integer bSChannelBwDL;
    @JsonProperty("bSChannelBwUL")
    private Integer bSChannelBwUL;
}
