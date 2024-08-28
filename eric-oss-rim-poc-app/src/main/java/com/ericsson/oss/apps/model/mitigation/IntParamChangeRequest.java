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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * this class hosts a request for change and the reason for the request
 */
@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IntParamChangeRequest implements Serializable {

    private static final long serialVersionUID = -3516437886421151282L;
    /**
     * The requester fdn (the victim cell that requires this change).
     * A victim cell may require a change on itself, or on a neighbor.
     */
    @EqualsAndHashCode.Include
    private String requesterFdn;

    /**
     * The parameter value requested
     */
    private Integer requestedValue;

    /**
     * can be victim if a victim requests a change on itself or neighbor
     * if a victim is requesting it on a neighbor
     */
    private MitigationCellType mitigationCellType;

}
