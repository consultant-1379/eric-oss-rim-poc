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

import com.ericsson.oss.apps.data.collection.features.handlers.mobility.MobilityMitigationAction;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.getCellRelationChange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class PendingRelationChangesApplicatorTest {
    @InjectMocks
    private PendingRelationChangesApplicator pendingRelationChangesApplicator;
    @Mock
    private CellRelationChangeRepo cellRelationChangeRepo;
    @Mock
    private MobilityMitigationAction mobilityMitigationAction;
    @Captor
    private ArgumentCaptor<Collection<CellRelationChange>> changesCaptor;

    @Test
    void applyChangesTestForEmptyRepo() {
        pendingRelationChangesApplicator.applyPendingChanges();
        Mockito.verify(mobilityMitigationAction, Mockito.times(1)).applyPendingChanges(changesCaptor.capture());
        assertTrue(changesCaptor.getValue().isEmpty());
    }

    @Test
    void applyPendingChangesTestForFilledRepo() {
        List<CellRelationChange> changes = Collections.singletonList(getCellRelationChange(0, 4));
        Mockito.when(cellRelationChangeRepo.findByMitigationState(eq(MitigationState.PENDING))).thenReturn(changes);
        pendingRelationChangesApplicator.applyPendingChanges();
        Mockito.verify(mobilityMitigationAction, Mockito.times(1)).applyPendingChanges(changesCaptor.capture());
        assertEquals(changes, changesCaptor.getValue());
    }
}
