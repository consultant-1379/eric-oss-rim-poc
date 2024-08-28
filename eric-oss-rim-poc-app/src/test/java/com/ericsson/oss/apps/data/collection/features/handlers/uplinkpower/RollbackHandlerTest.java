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

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN2;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.buildFtRopNRCellDUInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ericsson.oss.apps.classification.AllowedCellService;
import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.model.mitigation.IntParamChangeRequest;
import com.ericsson.oss.apps.model.mitigation.MitigationCellType;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.repositories.ParameterChangesRepo;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ExtendWith(MockitoExtension.class)
class RollbackHandlerTest {

    @Mock
    private ParametersChanges parametersChanges1;
    @Mock
    private ParametersChanges parametersChanges2;
    @Mock
    private ParameterChangesRepo parameterChangesRepo;
    @Mock
    private AllowedCellService allowedCellService;
    @Mock
    private CellSelectionConfig cellSelectionConfig;

    @InjectMocks
    RollbackHandler rollbackHandler;

    private FeatureContext context;

    @Captor
    ArgumentCaptor<Iterable<ParametersChanges>> parametersChangesArgumentCaptor;

    @Captor
    ArgumentCaptor<Set<IntParamChangeRequest>> intParamChangeRequestArgumentCaptor;

    @BeforeEach
    void setUp() {
        when(cellSelectionConfig.getMaxDeltaIPNThresholdDb()).thenReturn(1D);
        context = new FeatureContext(0L);
    }

    @Test
    void rollbackChanges() {
        when(allowedCellService.isAllowed(anyString())).thenReturn(true);
        withFtNRCellDUs(0.9, 1.1).withParametersChanges(true, false);
        rollbackHandler.handle(context);
        verify(parametersChanges1).removeRequests(intParamChangeRequestArgumentCaptor.capture());
        verifyChangeRequests(false);
        verify(parameterChangesRepo).saveAll(parametersChangesArgumentCaptor.capture());
        val savedChanges = parametersChangesArgumentCaptor.getValue();
        savedChanges.forEach(savedParametersChanges -> assertEquals(parametersChanges1, savedParametersChanges));
    }

    @Test
    void rollbackChangesForNoData() {
        when(allowedCellService.isAllowed(anyString())).thenReturn(true);
        withFtNRCellDUs(2, 2, (byte) 0).withParametersChanges(true, false);
        rollbackHandler.handle(context);
        verify(parametersChanges1).removeRequests(intParamChangeRequestArgumentCaptor.capture());
        verifyChangeRequests(true);
        verify(parameterChangesRepo).saveAll(parametersChangesArgumentCaptor.capture());
        val savedChanges = parametersChangesArgumentCaptor.getValue();
        savedChanges.forEach(savedParametersChanges -> assertEquals(parametersChanges1, savedParametersChanges));
    }

    @Test
    void rollbackChangesNotInAllowedList() {
        when(allowedCellService.isAllowed(anyString())).thenReturn(false);
        withFtNRCellDUs(0.9, 1.1).withParametersChanges(true, true);
        rollbackHandler.handle(context);
        verify(parametersChanges1).removeRequests(intParamChangeRequestArgumentCaptor.capture());
        verifyChangeRequests(true);
        verify(parametersChanges2).removeRequests(any());
        verify(parameterChangesRepo).saveAll(parametersChangesArgumentCaptor.capture());
        var savedChanges = StreamSupport.stream(parametersChangesArgumentCaptor.getValue().spliterator(), false).collect(Collectors.toSet());
        assertEquals(savedChanges, Set.of(parametersChanges1, parametersChanges2));
    }

    @Test
    void rollbackNoChanges() {
        withFtNRCellDUs(0.9, 1.1).withParametersChanges(false, false);
        rollbackHandler.handle(context);
        verify(parameterChangesRepo).saveAll(parametersChangesArgumentCaptor.capture());
        assertFalse(parametersChangesArgumentCaptor.getValue().iterator().hasNext());
    }

    private void verifyChangeRequests(boolean secondChangeRequestRemoved) {
        val requestsToRemove = intParamChangeRequestArgumentCaptor.getValue();
        assertTrue(requestsToRemove.contains(new IntParamChangeRequest(FDN1, 0, MitigationCellType.VICTIM)));
        assertEquals(secondChangeRequestRemoved, requestsToRemove.contains(new IntParamChangeRequest(FDN2, 0, MitigationCellType.VICTIM)));
    }

    private RollbackHandlerTest withFtNRCellDUs(double avgSw8AvgDeltaIpNCell1, double avgSw8AvgDeltaIpNCell2) {
        return withFtNRCellDUs(avgSw8AvgDeltaIpNCell1, avgSw8AvgDeltaIpNCell2, (byte) 2);
    }

    private RollbackHandlerTest withFtNRCellDUs(double avgSw8AvgDeltaIpNCell1, double avgSw8AvgDeltaIpNCell2, byte nRopsLastSeen) {
        val ftRopNRCellDUInput1 = buildFtRopNRCellDUInput(FDN1);
        ftRopNRCellDUInput1.setNRopsInLastSeenWindow(nRopsLastSeen);
        ftRopNRCellDUInput1.setAvgSw8AvgDeltaIpN(avgSw8AvgDeltaIpNCell1);
        val ftRopNRCellDUInput2 = buildFtRopNRCellDUInput(FDN2);
        ftRopNRCellDUInput2.setAvgSw8AvgDeltaIpN(avgSw8AvgDeltaIpNCell2);
        ftRopNRCellDUInput2.setNRopsInLastSeenWindow(nRopsLastSeen);
        context.getFdnToFtRopNRCellDU().put(FDN1, ftRopNRCellDUInput1);
        context.getFdnToFtRopNRCellDU().put(FDN2, ftRopNRCellDUInput2);
        return this;
    }

    private void withParametersChanges(boolean paramChanges1RequestsRemoved, boolean paramChanges2RequestsRemoved) {
        when(parameterChangesRepo.findAll()).thenReturn(List.of(parametersChanges1, parametersChanges2));
        setParameterChangesMock(parametersChanges1, paramChanges1RequestsRemoved, FDN1);
        setParameterChangesMock(parametersChanges2, paramChanges2RequestsRemoved, FDN2);
    }

    private void setParameterChangesMock(ParametersChanges parametersChanges, boolean requestsRemoved, String fdn) {
        when(parametersChanges.isUplinkPowerVictim()).thenReturn(true);
        when(parametersChanges.getObjectId()).thenReturn(ManagedObjectId.of(fdn));
        when(parametersChanges.removeRequests(any())).thenReturn(requestsRemoved);
    }

}