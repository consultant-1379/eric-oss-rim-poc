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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.apps.CoreApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = { CoreApplication.class, CellSelectionConfig.class, ThreadingConfig.class })
class ConfigTests {
    @Autowired
    private CellSelectionConfig thresholdConfig;

    @Autowired
    private ThreadingConfig threadingConfig;

    @Test
    void thresholdConfig_test() {
        log.info("Testing 'cell-selection-config' from application.yaml");
        assertEquals(2.0, thresholdConfig.getMinRemoteInterferenceVictimCellDb(), "Incorrect value for 'minRemoteInterferenceVictimCell'");
        assertEquals(1.0, thresholdConfig.getMaxRemoteInterferenceNeighborCellDb(), "Incorrect value for 'maxRemoteInterferenceNeighborCell'");
        assertEquals(5, thresholdConfig.getMaxRankSelectBestNeighborCells(), "Incorrect value for 'maxRankSelectBestNeighborCells'");
        assertEquals(95.0, thresholdConfig.getAcceptHandoversAboveHoPercent(), "Incorrect value for 'acceptHandoversAboveHoPercent'");
        assertEquals(1, thresholdConfig.getP0RejectNumberHandoversBelowValue(), "Incorrect value for 'p0RejectNumberHandoversBelowValue'");
        assertEquals(6, thresholdConfig.getCioAcceptTopRankedValue(), "Incorrect value for 'cioAcceptTopRankedValue'");
        assertEquals(1, thresholdConfig.getCioRejectNumberHandoversBelowValue(), "Incorrect value for 'cioRejectNumberHandoversBelowValue'");
    }

    @Test
    void threadingConfig_test() {
        log.info("Testing 'threading' from application.yaml");
        assertEquals(4, threadingConfig.getPoolSizeForRelationSyncQuery(), "Incorrect value for 'poolSizeForRelationSyncQuery'");
        assertEquals(8, threadingConfig.getPoolSizeForCtsGeoQuery(), "Incorrect value for 'minRemoteInterferenceVictimCell'");
        assertEquals(4, threadingConfig.getPoolSizeForNcmpCmQuery(), "Incorrect value for 'maxRemoteInterferenceNeighborCell'");
        assertEquals(4, threadingConfig.getPoolSizeForULPowerMitigation(), "Incorrect value for 'maxRankSelectBestNeighborCells'");
        assertEquals(4, threadingConfig.getPoolSizeForCIOMitigation(), "Incorrect value for 'p0AcceptHandoversAboveHoPercent'");
    }

}
