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
package com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower;

import com.ericsson.oss.apps.client.NcmpClient;
import com.ericsson.oss.apps.data.collection.features.handlers.ConfigChangeImplementor;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UplinkPowerChangeImplementorTest {

    @InjectMocks
    ConfigChangeImplementor configChangeImplementor;

    @Mock
    NcmpClient ncmpClient;

    @Captor
    private ArgumentCaptor<ManagedObject> managedObjectArgumentCaptor;

    @BeforeEach
    void setup()
    {
        ReflectionTestUtils.setField(configChangeImplementor, "closeLoopMode", true);
    }

    @Test
    void testImplementNrCellDu()
    {
        testImplementNrCellDu(true);
    }

    @Test
    void testImplementNrCellRelation()
    {
        testImplementNrCellRelation(true);
    }

    @Test
    void testImplementNrCellDuFailed()
    {
        testImplementNrCellDu(false);
    }

    @Test
    void testImplementNrCellRelationFailed()
    {
        testImplementNrCellRelation(false);
    }


    private void testImplementNrCellDu(boolean isPatchSuccessful) {
        NRCellDU nrCellDU = getNrCellDUWithParamChanges();
        when(ncmpClient.patchCmResource(any())).thenReturn(isPatchSuccessful);
        assertEquals(isPatchSuccessful, configChangeImplementor.implementChange(nrCellDU));
        verifyArgumentsAndCallsNrCellDu();
    }

    private void testImplementNrCellRelation(boolean isPatchSuccessful) {
        when(ncmpClient.patchCmResource(any())).thenReturn(isPatchSuccessful);
        CellRelationChange cellRelationChange = getCellRelationChange(0, 1);
        assertEquals(isPatchSuccessful, configChangeImplementor.implementChange(cellRelationChange));
        verifyArgumentsAndCallsNrCellRelation(isPatchSuccessful ?
                List.of(cellRelationChange.getSourceRelation(), cellRelationChange.getTargetRelation()) :
                List.of(cellRelationChange.getSourceRelation()));
    }

    @NotNull
    private NRCellDU getNrCellDUWithParamChanges() {
        NRCellDU nrCellDU = new NRCellDU(FDN1);
        ParametersChanges parametersChanges = new ParametersChanges(nrCellDU);
        parametersChanges.getPZeroNomPuschGrantChangeState().setRequiredValue(1);
        parametersChanges.getPZeroUePuschOffset256QamChangeState().setRequiredValue(2);
        nrCellDU.setParametersChanges(parametersChanges);
        return nrCellDU;
    }

    private void verifyArgumentsAndCallsNrCellDu() {
        managedObjectArgumentCaptor.getAllValues().stream()
                .map(managedObject -> (NRCellDU) managedObject)
                .peek(cellforChange -> assertEquals(1, cellforChange.getPZeroNomPuschGrant()))
                .forEach(cellforChange -> assertEquals(2, cellforChange.getPZeroUePuschOffset256Qam()));
    }

    private void verifyArgumentsAndCallsNrCellRelation(List<NRCellRelation> nrCellRelations) {
        verify(ncmpClient, times(nrCellRelations.size())).patchCmResource(managedObjectArgumentCaptor.capture());
        List<ManagedObject> capturedArgument = managedObjectArgumentCaptor.getAllValues();
        assertEquals(capturedArgument, nrCellRelations);
    }


}