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

import com.opencsv.bean.processor.StringProcessor;

import java.util.regex.Pattern;

public class PreProcessToCleanupArrayElements implements StringProcessor {
    private static final String BLANK_STRING = "    ";
    private int nElements;

    private final Pattern squareBrackets = Pattern.compile("[\\[\\]]");
    private final Pattern spaces = Pattern.compile("\\s+");


    @Override
    public String processString(String value) {
        if (value == null || value.trim().isEmpty() || value.isBlank() || value.contains("null")) {
            return BLANK_STRING;
        }
        final String removedBracketsAndCommas = squareBrackets.matcher(value).replaceAll("").replace(",", " ");
        final String inStrClean = spaces.matcher(removedBracketsAndCommas).replaceAll(" ").trim();

        if (inStrClean.trim().split(" ").length != nElements) {
            return BLANK_STRING;
        }
        return inStrClean.trim();
    }

    @Override
    public void setParameterString(String value) {
        nElements = Integer.parseInt(value);
    }
}