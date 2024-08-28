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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class NeighborDictionary {
    @Id
    private String fdn;

    @ElementCollection
    @MapKeyJoinColumns(value = {@MapKeyJoinColumn(name="me_fdn"), @MapKeyJoinColumn(name="ref_id")})
    private Map<NRCellRelation, NRCellRelation> neighbors = new HashMap<>();
}
