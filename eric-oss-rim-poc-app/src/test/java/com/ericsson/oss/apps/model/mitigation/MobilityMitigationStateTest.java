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
package com.ericsson.oss.apps.model.mitigation;

import com.ericsson.oss.apps.data.collection.features.report.mobility.MobilityReportingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN2;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.getCellRelationChange;
import static com.ericsson.oss.apps.model.mitigation.MitigationState.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MobilityMitigationStateTest {

    private static final long ROP_TIME = 1234;
    private static final long LAST_CHANGED = 4321;
    private static final int REQUIRED_VALUE = 42;
    private static final int ORIGINAL_VALUE = 40;

    MobilityMitigationState mobilityMitigationState;
    CellRelationChange cellRelationChange;

    @BeforeEach
    public void setup() {
        mobilityMitigationState = new MobilityMitigationState();
        cellRelationChange = getCellRelationChange(ORIGINAL_VALUE);
    }

    /**
     * given MobilityMitigationState has a change in stepChanges
     * when the report records are fetched
     * then
     * there is one record in the resulting list
     * the parameters and state are set to the correct values
     */
    @Test
    void getMobilityMitigationRecordsStepChanges() {
        mobilityMitigationState.getStepChanges().put(FDN2, List.of(cellRelationChange));
        testGetMobilityMitigationRecord(PENDING);
    }

    /**
     * given MobilityMitigationState has a change in rollbackChanges
     * when the report records are fetched
     * then
     * there is one record in the resulting list
     * the parameters and state are set to the correct values
     */
    @Test
    void getMobilityMitigationRecordsRollbackChanges() {
        mobilityMitigationState.getRollbackChanges().put(FDN2, List.of(cellRelationChange));
        testGetMobilityMitigationRecord(PENDING);
    }

    /**
     * given MobilityMitigationState has a change in noChanges
     * when the report records are fetched
     * then
     * there is one record in the resulting list
     * the parameters are set to the correct values
     * the state is set to OBSERVATION
     */
    @Test
    void getMobilityMitigationRecordsObservation() {
        mobilityMitigationState.getNoChanges().put(FDN2, List.of(cellRelationChange));
        testGetMobilityMitigationRecord(OBSERVATION);
    }

    private void testGetMobilityMitigationRecord(MitigationState expectedMitigationState) {
        cellRelationChange.setLastChangedTimestamp(LAST_CHANGED);
        cellRelationChange.setMitigationState(CHANGE_FAILED);
        cellRelationChange.setRequiredValue(REQUIRED_VALUE);
        var reportRecords = mobilityMitigationState.getMobilityMitigationRecords(ROP_TIME);
        assertEquals(1, reportRecords.size());
        var reportRecord = reportRecords.get(0);
        verifyReportRecordFields(reportRecord, expectedMitigationState);
    }

    private void verifyReportRecordFields(MobilityReportingStatus reportRecord, MitigationState expectedMitigationState) {
        assertEquals(ROP_TIME, reportRecord.getCurrentTimestamp());
        assertEquals(FDN2, reportRecord.getVictimCellFdn());
        assertEquals(expectedMitigationState, reportRecord.getMitigationState());
        assertEquals(cellRelationChange.getLastChangedTimestamp(), reportRecord.getChangedTimestamp());
        assertEquals(cellRelationChange.getRequiredValue(), reportRecord.getRequiredCio());
        assertEquals(cellRelationChange.getOriginalValue(), reportRecord.getOriginalCio());
        assertEquals(cellRelationChange.getSourceRelation().getCellIndividualOffsetNR(), reportRecord.getCurrentCio());
        assertEquals(cellRelationChange.getTargetRelation().getCell(), reportRecord.getNeighborCellCu());
        assertEquals(cellRelationChange.getSourceRelation().getObjectId().toString(), reportRecord.getSourceRelationFdn());
        assertEquals(cellRelationChange.getTargetRelation().getObjectId().toString(), reportRecord.getTargetRelationFdn());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1,PENDING,PENDING",
            "0,PENDING,OBSERVATION",
            "0,CHANGE_FAILED,OBSERVATION",
            "1,CHANGE_FAILED,CHANGE_FAILED",
    })
    void testChangeActionMitigationStatusPending(int deltaChange, MitigationState state, MitigationState expectedState) {
        mobilityMitigationState.getStepChanges().put(FDN2, List.of(cellRelationChange));
        cellRelationChange.setRequiredValue(ORIGINAL_VALUE + deltaChange);
        cellRelationChange.setMitigationState(state);
        var reportRecord = mobilityMitigationState.getMobilityMitigationRecords(ROP_TIME).get(0);
        assertEquals(expectedState, reportRecord.getMitigationState());
    }

    @ParameterizedTest
    @CsvSource(value = {"0", "1",})
    void testChangeActionMitigationStatusObservationNoChange(int deltaChange) {
        mobilityMitigationState.getNoChanges().put(FDN2, List.of(cellRelationChange));
        cellRelationChange.setRequiredValue(ORIGINAL_VALUE + deltaChange);
        var reportRecord = mobilityMitigationState.getMobilityMitigationRecords(ROP_TIME).get(0);
        assertEquals(OBSERVATION, reportRecord.getMitigationState());
    }

    @Test
    void testChangeActionMitigationStatusObservationStep() {
        mobilityMitigationState.getStepChanges().put(FDN2, List.of(cellRelationChange));
        cellRelationChange.setRequiredValue(ORIGINAL_VALUE);
        var reportRecord = mobilityMitigationState.getMobilityMitigationRecords(ROP_TIME).get(0);
        assertEquals(OBSERVATION, reportRecord.getMitigationState());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1,ROLLBACK_SUCCESSFUL,ROLLBACK_SUCCESSFUL",
            "0,ROLLBACK_SUCCESSFUL,ROLLBACK_SUCCESSFUL",
            "0,ROLLBACK_FAILED,ROLLBACK_FAILED",
            "1,ROLLBACK_FAILED,ROLLBACK_FAILED",
    })
    void testChangeActionMitigationStatusRollback(int deltaChange, MitigationState state, MitigationState expectedState) {
        mobilityMitigationState.getRollbackChanges().put(FDN2, List.of(cellRelationChange));
        cellRelationChange.setRequiredValue(ORIGINAL_VALUE + deltaChange);
        cellRelationChange.setMitigationState(state);
        var reportRecord = mobilityMitigationState.getMobilityMitigationRecords(ROP_TIME).get(0);
        assertEquals(expectedState, reportRecord.getMitigationState());
    }

}
