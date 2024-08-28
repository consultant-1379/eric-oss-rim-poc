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
package com.ericsson.oss.apps.data.collection.features;

import com.ericsson.oss.apps.data.collection.features.handlers.InterferenceType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FtRopNRCellDUTest {

    @ParameterizedTest
    @CsvSource(value = {
            "false, 1,     OTHER",
            "true , 1,     MIXED",
            "true , 1,     REMOTE",
            "true , 1,     NOT_DETECTED",
            "false, null,  OTHER",
            "true , null,  MIXED",
            "true , null,  REMOTE",
            "false, null,  NOT_DETECTED",
    }, nullValues = {"null"})
    void isRemoteInterference(boolean isRemoteInterference, Long connectedComponentId, InterferenceType interferenceType) {
        FtRopNRCellDU ftRopNRCellDU = new FtRopNRCellDU();
        ftRopNRCellDU.setInterferenceType(interferenceType);
        ftRopNRCellDU.setConnectedComponentId(connectedComponentId);
        assertEquals(isRemoteInterference, ftRopNRCellDU.isRemoteInterference());
    }
}