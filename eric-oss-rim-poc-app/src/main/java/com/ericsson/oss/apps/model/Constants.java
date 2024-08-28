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
package com.ericsson.oss.apps.model;

import lombok.experimental.UtilityClass;

@UtilityClass
// Todo: this should be split uo if its grow too large.
public class Constants {
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String EMPTY = "";
    public static final String EQUAL = "=";
    public static final String RIGHT_SQUARE_BRACKET = "]";
    public static final String SEMICOLON = ";";
    public static final String SLASH = "/";

    public static final String ID = "id";
    public static final String ATTRIBUTES = "attributes";

    public static final String DN_PREFIX = "dnPrefix";
    public static final String REF_ID = "refId";
    public static final String OBJECT_ID = "objectId";

    public static final double PER_SYMBOL_DISTANCE = 21.42;
    public static final double DEFAULT_SCS = 15D;

    //km/sec
    public static final double LIGHT_SPEED = 299702.547;
    public static final double SYMBOL_DURATION_15_KHZ = 1E-3 / 14;

}