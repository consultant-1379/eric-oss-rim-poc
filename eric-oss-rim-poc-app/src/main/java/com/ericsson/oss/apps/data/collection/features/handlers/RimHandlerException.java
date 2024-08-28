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

package com.ericsson.oss.apps.data.collection.features.handlers;

/**
 * Exception for when an external dependency (i.e. a pod) cannot be reached.
 */
public class RimHandlerException extends RuntimeException {

    private static final long serialVersionUID = 2009529903658373419L;

    public RimHandlerException(final String message) {
        super(message);
    }
}
