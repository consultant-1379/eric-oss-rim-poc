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
package com.ericsson.oss.apps.data.collection.pmrop;

import org.apache.commons.lang3.math.NumberUtils;

import com.opencsv.bean.processor.StringProcessor;

public class PreProcessToCleanupValues implements StringProcessor {
    String defaultValue;

    @Override
    public String processString(String value) {
        if (!NumberUtils.isCreatable(value) || value.contains("null")) {
            return defaultValue;
        }
        return value.trim();
    }

    @Override
    public void setParameterString(String value) {
        defaultValue = value;
    }
}
