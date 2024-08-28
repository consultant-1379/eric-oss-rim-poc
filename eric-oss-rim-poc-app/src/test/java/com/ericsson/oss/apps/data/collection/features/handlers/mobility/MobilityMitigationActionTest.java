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
package com.ericsson.oss.apps.data.collection.features.handlers.mobility;

import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.ConfigChangeImplementor;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.getCellRelationChange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MobilityMitigationActionTest {

    @Spy
    private MitigationConfig mitigationConfig;
    @Mock
    private ConfigChangeImplementor changeImplementor;
    @Mock
    private CellRelationChangeRepo changeRepo;
    @InjectMocks
    private MobilityMitigationAction mitigationAction;
    @Captor
    private ArgumentCaptor<CellRelationChange> changesCaptor;
    @Mock
    Counter counter;

    private CellRelationChange noChange;
    private CellRelationChange failRollbackChange;
    private CellRelationChange successIncrementChange;
    private Set<CellRelationChange> changes;

    @Mock
    private ThreadingConfig threadingConfig;

    @BeforeEach
    void setup() {
        mitigationConfig.getCellIndividualOffset().setMaxAbsoluteValue(24);
        mitigationConfig.getCellIndividualOffset().setStepSize(4);

        noChange = getCellRelationChange();
        failRollbackChange = getCellRelationChange(0, 1, 0);
        successIncrementChange = getCellRelationChange(4, 1, 6);

        changes = Set.of(noChange, failRollbackChange, successIncrementChange);
        when(threadingConfig.getPoolSizeForCIOMitigation()).thenReturn(2);

        when(changeImplementor.implementChange(successIncrementChange)).thenReturn(true);
        when(changeImplementor.implementChange(failRollbackChange)).thenReturn(false);
    }

    @Test
    void testApplyPendingChanges() {
        mitigationAction.applyPendingChanges(changes);
        Mockito.verify(changeRepo, times(2)).save(changesCaptor.capture());
        assertChanges(Set.of(successIncrementChange, failRollbackChange), MitigationState.CONFIRMED, MitigationState.ROLLBACK_FAILED);
    }

    @Test
    void testRollbackChanges() {
        mitigationAction.rollBackChanges(changes);

        Mockito.verify(changeRepo, times(2)).delete(changesCaptor.capture());
        assertChanges(Set.of(successIncrementChange, noChange), MitigationState.ROLLBACK_SUCCESSFUL, MitigationState.ROLLBACK_FAILED);
        assertTrue(noChange.getLastChangedTimestamp() > Long.MIN_VALUE);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1, 4",
            "6, 24"
    })
    void testIncrementChanges(int maxSteps, int noChangeRequiredValue) {
        mitigationConfig.getCellIndividualOffset().setMaxSteps(maxSteps);
        noChange.setRequiredValue(noChangeRequiredValue);
        noChange.setMitigationState(MitigationState.CONFIRMED);

        when(changeImplementor.implementChange(noChange)).thenReturn(true);

        mitigationAction.incrementChanges(changes);

        Mockito.verify(changeRepo, times(3)).save(changesCaptor.capture());
        assertChanges(changes, MitigationState.CONFIRMED, MitigationState.CHANGE_FAILED);
        assertTrue(noChange.getLastChangedTimestamp() > Long.MIN_VALUE);
    }

    void assertChanges(Set<CellRelationChange> changes, MitigationState positiveState, MitigationState negativeState) {
        Mockito.verify(changeImplementor).implementChange(successIncrementChange);
        Mockito.verify(changeImplementor).implementChange(failRollbackChange);
        assertEquals(changes, new HashSet<>(changesCaptor.getAllValues()));
        assertEquals(positiveState, noChange.getMitigationState());
        assertEquals(negativeState, failRollbackChange.getMitigationState());
        assertEquals(positiveState, successIncrementChange.getMitigationState());
        assertTrue(failRollbackChange.getLastChangedTimestamp() > Long.MIN_VALUE);
        assertTrue(successIncrementChange.getLastChangedTimestamp() > Long.MIN_VALUE);
    }
}
