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
package com.ericsson.oss.apps.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NumericParameterConfig<T extends Number> {

    private T stepSize;

    private T maxAbsoluteValue;

    private T maxSteps;
}
