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

import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.repositories.ParameterChangesRepo;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ParamChangesHelperTest {

    @Mock
    ParametersChanges parametersChanges;

    @Mock
    private ParameterChangesRepo parameterChangesRepo;

    @Spy
    private MitigationConfig mitigationConfig;

    @InjectMocks
    private ParamChangesHelper paramChangesHelper;

    @BeforeEach
    void setup() {
        when(parameterChangesRepo.findAll()).thenReturn(List.of(parametersChanges));
    }

    @Test
    void getFdnSetByObservationStateNoVictim() {
        setParameterChangeNoVictim();
        mitigationConfig.setObservationWindowMs(0);
        val results = paramChangesHelper.getFdnSetByObservationState(Long.MAX_VALUE);
        assertTrue(results.getFirst().isEmpty());
        assertTrue(results.getSecond().isEmpty());
    }

    @Test
    void getFdnSetByObservationStateOutOfWindow() {
        setParameterChangeWithRopTimestamp(10L);
        setParameterChangeNrCellDu();
        mitigationConfig.setObservationWindowMs(2);
        val results = paramChangesHelper.getFdnSetByObservationState(20);
        assertEquals(Set.of(FDN1), results.getFirst());
        assertTrue(results.getSecond().isEmpty());
    }

    @Test
    void getVictimObservationStateNeverChanged() {
        setParameterChangeWithRopTimestamp(Long.MIN_VALUE);
        mitigationConfig.setObservationWindowMs(2);
        val results = paramChangesHelper.getFdnSetByObservationState(20);
        assertTrue(results.getFirst().isEmpty());
        assertTrue(results.getSecond().isEmpty());
    }

    @Test
    void getFdnSetByObservationStateInWindow() {
        setParameterChangeWithRopTimestamp(10L);
        setParameterChangeNrCellDu();
        mitigationConfig.setObservationWindowMs(11);
        val results = paramChangesHelper.getFdnSetByObservationState(20);
        assertEquals(Set.of(FDN1), results.getFirst());
        assertEquals(Set.of(FDN1), results.getSecond());
    }


    @Test
    void getNeighborsUnderMitigation() {
        setParameterChangeWithRequesterNeighborList();
        val results = paramChangesHelper.getUplinkpowerNeighborsUnderMitigation();
        val fdn1Neighbors = results.get(ManagedObjectId.of(FDN1 + 1));
        verifyNeighborMap(fdn1Neighbors, Set.of(ManagedObjectId.of(FDN2)));
        val fdn2Neighbors = results.get(ManagedObjectId.of(FDN1 + 2));
        verifyNeighborMap(fdn2Neighbors, Set.of(ManagedObjectId.of(FDN1), ManagedObjectId.of(FDN2)));
        val fdn3Neighbors = results.get(ManagedObjectId.of(FDN1 + 3));
        verifyNeighborMap(fdn3Neighbors, Set.of(ManagedObjectId.of(FDN1), ManagedObjectId.of(FDN2)));
    }

    private void verifyNeighborMap(Set<ManagedObjectId> neighbors, Set<ManagedObjectId> neighborFdnSet) {
        assertTrue(neighbors.containsAll(neighborFdnSet));
    }

    private void setParameterChangeNoVictim() {
        when(parametersChanges.isUplinkPowerVictim()).thenReturn(false);
    }

    private void setParameterChangeWithRopTimestamp(long timeStamp) {
        when(parametersChanges.isUplinkPowerVictim()).thenReturn(true);
        when(parametersChanges.getLastChangedTimestamp()).thenReturn(timeStamp);
    }

    private void setParameterChangeNrCellDu() {
        when(parametersChanges.getObjectId()).thenReturn(ManagedObjectId.of(FDN1));
    }

    private void setParameterChangeWithRequesterNeighborList() {
        ParametersChanges parametersChanges1 = mock(ParametersChanges.class);
        ParametersChanges parametersChanges2 = mock(ParametersChanges.class);
        when(parameterChangesRepo.findAll()).thenReturn(List.of(parametersChanges1, parametersChanges2));
        when(parametersChanges1.getObjectId()).thenReturn(ManagedObjectId.of(FDN1));
        when(parametersChanges2.getObjectId()).thenReturn(ManagedObjectId.of(FDN2));
        when(parametersChanges1.getRequesterAsNeighborUplinkPowerSet()).thenReturn(
                Set.of(ManagedObjectId.of(FDN1 + 2),
                        ManagedObjectId.of(FDN1 + 3)));
        when(parametersChanges2.getRequesterAsNeighborUplinkPowerSet()).thenReturn(
                Set.of(ManagedObjectId.of(FDN1 + 1),
                        ManagedObjectId.of(FDN1 + 2),
                        ManagedObjectId.of(FDN1 + 3)));
    }


}