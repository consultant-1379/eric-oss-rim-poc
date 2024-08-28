/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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
package com.ericsson.oss.apps.api.retries;

import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class RestClientExceptionClassifier extends BinaryExceptionClassifier {
    static final long serialVersionUID = 7612929140324598338L;
    public RestClientExceptionClassifier(boolean defaultValue) {
        super(defaultValue);
    }

    @Override
    public Boolean classify(Throwable classifiable) {
        return !(classifiable instanceof HttpClientErrorException) || HttpStatus.TOO_MANY_REQUESTS.equals(((HttpClientErrorException) classifiable).getStatusCode());
    }

}
