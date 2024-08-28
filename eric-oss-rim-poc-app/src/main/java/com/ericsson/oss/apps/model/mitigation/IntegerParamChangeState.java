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


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * This class keeps track of the change requests on a MO for a specific Integer parameter
 * only one request per requester per MO per parameter is allowed.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
public class IntegerParamChangeState implements Serializable {

    private static final long serialVersionUID = 1855918229486259672L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @ElementCollection
    private Set<IntParamChangeRequest> intParamChangeRequests = new HashSet<>();

    private Integer originalValue;

    private Integer requiredValue;

    /**
     * replaces a change request from a requester
     *
     * @param paramChangeRequest the new request
     * @return true if there was a request from the same requester, false otherwise
     */
    public boolean upsertParameterChangeRequest(IntParamChangeRequest paramChangeRequest) {
        boolean result = intParamChangeRequests.remove(paramChangeRequest);
        intParamChangeRequests.add(paramChangeRequest);
        return result;
    }

    public void calculateRequiredParameterValue() {
        requiredValue = intParamChangeRequests.stream().mapToInt(IntParamChangeRequest::getRequestedValue).max().orElse(originalValue);
    }

}