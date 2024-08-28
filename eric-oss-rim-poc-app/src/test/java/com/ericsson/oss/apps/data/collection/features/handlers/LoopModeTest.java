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

package com.ericsson.oss.apps.data.collection.features.handlers;

import com.ericsson.oss.apps.client.NcmpClient;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest
public class LoopModeTest {

    @Autowired
    private ConfigChangeImplementor changeImplementor;
    @MockBean
    private NcmpClient ncmpClient;

    @BeforeEach
    void setup()
    {
        when(ncmpClient.patchCmResource(any())).thenReturn(true);
    }

    @Test
    void testCloseLoopNRCellDUTestFailedPatch() {
        when(ncmpClient.patchCmResource(any())).thenReturn(false);
        ReflectionTestUtils.setField(changeImplementor, "closeLoopMode", true);
        assertFalse(changeImplementor.implementChange(getNrCellDU()));
        Mockito.verify(ncmpClient).patchCmResource(any());
    }

    @Test
    void testCloseLoopNRCellRelationTestFailedPatch() {
        when(ncmpClient.patchCmResource(any())).thenReturn(false);
        ReflectionTestUtils.setField(changeImplementor, "closeLoopMode", true);
        assertFalse(changeImplementor.implementChange(getCellRelationChange(0, 1)));
        Mockito.verify(ncmpClient).patchCmResource(any());
    }

    @Test
    void testCloseLoopNRCellDUTest() {
        ReflectionTestUtils.setField(changeImplementor, "closeLoopMode", true);
        assertTrue(changeImplementor.implementChange(getNrCellDU()));
        Mockito.verify(ncmpClient).patchCmResource(any());
    }

    @Test
    void testCloseLoopNRCellRelationTest() {
        ReflectionTestUtils.setField(changeImplementor, "closeLoopMode", true);
        assertTrue(changeImplementor.implementChange(getCellRelationChange(0, 1)));
        Mockito.verify(ncmpClient, times(2)).patchCmResource(any());
    }

    @Test
    void testOpenLoopNRCellDUTest() {
        ReflectionTestUtils.setField(changeImplementor, "closeLoopMode", false);
        assertTrue(changeImplementor.implementChange(getNrCellDU()));
        Mockito.verify(ncmpClient, Mockito.never()).patchCmResource(any());
    }

    @Test
    void testOpenLoopNRCellRelationTest() {
        ReflectionTestUtils.setField(changeImplementor, "closeLoopMode", false);
        assertTrue(changeImplementor.implementChange(getCellRelationChange(0, 1)));
        Mockito.verify(ncmpClient, Mockito.never()).patchCmResource(any());
    }

    @NotNull
    private NRCellDU getNrCellDU() {
        NRCellDU nrCellDU = new NRCellDU(FDN1);
        ParametersChanges parametersChanges = new ParametersChanges(nrCellDU);
        nrCellDU.setParametersChanges(parametersChanges);
        return nrCellDU;
    }
}
