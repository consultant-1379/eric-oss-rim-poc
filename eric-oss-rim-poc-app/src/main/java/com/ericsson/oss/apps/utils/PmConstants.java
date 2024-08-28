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
package com.ericsson.oss.apps.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PmConstants {

    public static final String ELEMENT_TYPE = "elementType";
    public static final String MO_FDN = "moFdn";
    public static final String NODE_FDN = "nodeFDN";
    public static final String ROP_END_TIME = "ropEndTime";
    public static final String ROP_BEGIN_TIME = "ropBeginTime";
    public static final String SUSPECT = "suspect";
    public static final String PM_COUNTERS = "pmCounters";

    public static final String MO_TYPE = "moType";
    public static final String OSSID = "ossid";
    public static final String SCHEMA_ID = "schemaID";


    public static final String COUNTER_TYPE_PDF = "pdf";
    public static final String COUNTER_TYPE_SINGLE = "single";
    public static final String COUNTER_TYPE_COMPRESSED_PDF = "compressedPdf";

    public static final long ROP_PERIOD_MS = 900000L;
    public static final double DOUBLE_TO_LONG_CONSTANT = 1E9;
    public static final String COUNTER_VALUE = "counterValue";
    public static final String COUNTER_TYPE = "counterType";
    public static final String IS_VALUE_PRESENT = "isValuePresent";

    public static final String MODIFIED_NRECLLDU_SCHEMA = "avro/schema/NRCellDU_GNBDU_1_MODIFIED.avsc";
    public static final String NRECLL_DU_SCHEMA = "avro/schema/NRCellDU_GNBDU_1.avsc";

    public static final String SCHEMA_SUBJECT_RIM = "RIM_";

    public static final long OLD_ROP_100_YEARS = 53153600000000L;

}
