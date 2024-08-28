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

import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.ConfigChangeImplementor;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.report.uplinkpower.UplinkPowerReportingStatus;
import com.ericsson.oss.apps.model.mitigation.IntParamChangeRequest;
import com.ericsson.oss.apps.model.mitigation.IntegerParamChangeState;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.repositories.CmNrCellDuRepo;
import io.micrometer.core.instrument.Counter;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Comparator;
import java.util.List;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN2;
import static com.ericsson.oss.apps.model.mitigation.MitigationCellType.NEIGHBOR;
import static com.ericsson.oss.apps.model.mitigation.MitigationState.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UplinkPowerMitigationChangeHandlerTest {

    private static final Integer ORIGINAL_P0_VALUE = 37;
    private static final Integer ORIGINAL_P0_256_VALUE = 32;


    private static final Integer CURRENT_P0_VALUE = 40;
    private static final Integer CURRENT_P0_256_VALUE = 35;

    private static final long LAST_CHANGED_TIMESTAMP = 12345678;
    public static final int ROP_TIME_STAMP = 1234;
    @Mock
    private CmNrCellDuRepo cmNrCellDuRepo;

    @Mock
    private ConfigChangeImplementor configChangeImplementor;

    @Mock
    private Counter counter;

    @InjectMocks
    UplinkPowerMitigationChangeHandler uplinkPowerMitigationChangeHandler;
    private NRCellDU nrCellDU;

    FeatureContext context;
    @Mock
    private ThreadingConfig threadingConfig;


    @BeforeEach
    void setup() {
        context = new FeatureContext(ROP_TIME_STAMP);
        when(threadingConfig.getPoolSizeForULPowerMitigation()).thenReturn(2);
    }

    /**
     * given
     * there is a cell with a parameter structure request attached in repo
     * the parameter structure has two requests for P0 and P0256 (two each)
     * the current parameter value is different from at least one of the requests
     * NCMP replies successfully
     * <p>
     * when
     * the handler is run
     * <p>
     * then
     * a change is sent to NCMP with the max value requested for the parameters
     * set __change state__ to CONFIRMED
     * set __current value__ to new value
     * set __changed timestamp__ to current time
     * cell and parameter state is saved in DB
     */
    @Test
    void testSuccessfulParameterChange() {
        ReflectionTestUtils.setField(uplinkPowerMitigationChangeHandler, "localMode", true);
        withNrCellDU().withChangeRequest();
        when(configChangeImplementor.implementChange(nrCellDU)).thenReturn(true);
        uplinkPowerMitigationChangeHandler.handle(context);
        verify(configChangeImplementor).implementChange(nrCellDU);

        assertEquals(CONFIRMED, nrCellDU.getParametersChanges().getMitigationState());
        assertEquals(CURRENT_P0_VALUE + 2, nrCellDU.getPZeroNomPuschGrant());
        assertEquals(CURRENT_P0_256_VALUE + 2, nrCellDU.getPZeroUePuschOffset256Qam());
        verifyRequiredParameters(CURRENT_P0_VALUE + 2, CURRENT_P0_256_VALUE + 2);
        assertEquals(ROP_TIME_STAMP, nrCellDU.getParametersChanges().getLastChangedTimestamp());

        verify(cmNrCellDuRepo).save(nrCellDU);
        verify(counter).increment();
        checkChangeReportStatus(context.getUplinkPowerReportingStatusList(), CONFIRMED);
        ReflectionTestUtils.setField(uplinkPowerMitigationChangeHandler, "localMode", false);
    }

    /**
     * given
     * there is a cell with a parameter structure request attached in repo
     * the parameter structure has two requests for P0 and P0256 (two each)
     * the current parameter value is different from at least one of the requests
     * NCMP replies unsuccessfully
     * <p>
     * when
     * the handler is run
     * <p>
     * then
     * a change is sent to NCMP with the max value requested for the parameters
     * set __change state__ to FAILED
     * leave __current value__ unchanged
     * leave __changed timestamp__ unchanged
     * cell and parameter state is saved in DB
     */
    @Test
    void testUnSuccessfulParameterChange() {
        withNrCellDU().withChangeRequest();
        when(configChangeImplementor.implementChange(nrCellDU)).thenReturn(false);
        uplinkPowerMitigationChangeHandler.handle(context);
        verify(configChangeImplementor).implementChange(nrCellDU);
        verifyNoChangesNRCellDU(CHANGE_FAILED, CURRENT_P0_VALUE + 2, CURRENT_P0_256_VALUE + 2);
        verify(cmNrCellDuRepo).save(nrCellDU);
        checkChangeReportStatus(context.getUplinkPowerReportingStatusList(), CHANGE_FAILED);
        verify(counter).increment();
    }

    /**
     * given
     * there is a cell with a parameter structure request attached in repo
     * the parameter structure has two requests for P0 and P0256 (two each)
     * the current parameter value is the same as the max of the requests (per parameter)
     * <p>
     * when
     * the handler is run
     * <p>
     * then
     * no change is sent to NCMP
     * leave __change state__ unchanged
     * leave __current value__ unchanged
     * leave __changed timestamp__ unchanged
     * no change is sent to DB
     */
    @Test
    void testUnnecessaryParameterChange() {
        withNrCellDU().withUnnecessaryChangeRequest();
        uplinkPowerMitigationChangeHandler.handle(context);
        verify(configChangeImplementor, never()).implementChange(any(NRCellDU.class));
        verifyNoChangesNRCellDU(OBSERVATION, CURRENT_P0_VALUE, CURRENT_P0_256_VALUE);
        checkNoChangeReportStatus(context.getUplinkPowerReportingStatusList());
        verify(cmNrCellDuRepo).save(nrCellDU);
    }

    /**
     * given
     * there is a cell with a parameter structure request attached in repo
     * the parameter structure has no requests for P0 and P0256
     * the current parameter values of the cell are different from the original ones
     * NCMP replies successfully
     * when
     * the handler is run
     * <p>
     * then
     * a change is sent to NCMP with the original value of P0 and P0256
     * remove the change structure from the cell
     * save cell without change structure in DB
     */
    //TODO add test to verify configChangeImplementor is not called if there is no change in params
    @Test
    void testSuccessfulRollback() {
        withNrCellDU().withNoChangeRequest();
        when(configChangeImplementor.implementChange(nrCellDU)).thenReturn(true);
        uplinkPowerMitigationChangeHandler.handle(context);
        verify(configChangeImplementor).implementChange(nrCellDU);
        assertNull(nrCellDU.getParametersChanges());
        verify(cmNrCellDuRepo).save(nrCellDU);
        assertEquals(ORIGINAL_P0_VALUE, nrCellDU.getPZeroNomPuschGrant());
        assertEquals(ORIGINAL_P0_256_VALUE, nrCellDU.getPZeroUePuschOffset256Qam());
        verify(counter).increment();
        val rollBackStatusReport = context.getUplinkPowerReportingStatusList().get(0);
        checkRollbackReportStatus(rollBackStatusReport, ROLLBACK_SUCCESSFUL);
    }

    /**
     * given
     * there is a cell with a parameter structure request attached in repo
     * the parameter structure has no requests for P0 and P0256
     * the current parameter values of the cell are the same as the original ones
     * NCMP replies successfully
     * when
     * the handler is run
     * <p>
     * then
     * no change is sent to NCMP
     * remove the change structure from the cell
     * save cell without change structure in DB
     */
    @Test
    void testSuccessfulRollbackNoChange() {
        withNrCellDU(ORIGINAL_P0_VALUE, ORIGINAL_P0_256_VALUE).withNoChangeRequest();
        uplinkPowerMitigationChangeHandler.handle(context);
        verify(configChangeImplementor, never()).implementChange(nrCellDU);
        assertNull(nrCellDU.getParametersChanges());
        verify(cmNrCellDuRepo).save(nrCellDU);
        assertEquals(ORIGINAL_P0_VALUE, nrCellDU.getPZeroNomPuschGrant());
        assertEquals(ORIGINAL_P0_256_VALUE, nrCellDU.getPZeroUePuschOffset256Qam());
        verify(counter).increment();
    }

    /**
     * given
     * there is a cell with a parameter structure request attached in repo
     * the parameter structure has no requests for P0 and P0256
     * NCMP replies unsuccessfully
     * when
     * the handler is run
     * <p>
     * then
     * a change is sent to NCMP with the original value of P0 and P0256
     * set  __change state__ to FAILED
     * leave __current value__ unchanged
     * set __required_value__ to original value
     * leave __changed timestamp__ unchanged
     * cell and parameter state is saved in DB
     */
    @Test
    void testUnSuccessfulRollback() {
        withNrCellDU().withNoChangeRequest();
        when(configChangeImplementor.implementChange(nrCellDU)).thenReturn(false);
        uplinkPowerMitigationChangeHandler.handle(context);
        verify(configChangeImplementor).implementChange(nrCellDU);
        verifyNoChangesNRCellDU(ROLLBACK_FAILED, ORIGINAL_P0_VALUE, ORIGINAL_P0_256_VALUE);
        verify(cmNrCellDuRepo).save(nrCellDU);
        verify(counter).increment();
        val rollBackStatusReport = context.getUplinkPowerReportingStatusList().get(0);
        checkRollbackReportStatus(rollBackStatusReport, ROLLBACK_FAILED);
    }

    private void checkRollbackReportStatus(UplinkPowerReportingStatus rollBackStatusReport, MitigationState rollbackStatus) {
        assertEquals(rollbackStatus, rollBackStatusReport.getMitigationState());
        assertEquals(FDN1, rollBackStatusReport.getCellFdn());
        assertEquals(ORIGINAL_P0_VALUE, rollBackStatusReport.getOriginalPZeroNomPuschGrant());
        assertEquals(ORIGINAL_P0_256_VALUE, rollBackStatusReport.getOriginalPZeroUePuschOffset256Qam());
        assertEquals(ORIGINAL_P0_VALUE, rollBackStatusReport.getRequiredPZeroNomPuschGrant());
        assertEquals(ORIGINAL_P0_256_VALUE, rollBackStatusReport.getRequiredZeroUePuschOffset256Qam());
        checkCurrentAndOriginalReportValues(rollBackStatusReport);
    }

    private void checkChangeReportStatus(List<UplinkPowerReportingStatus> changeStatusReport, MitigationState rollbackStatus) {
        changeStatusReport.sort(Comparator.comparing(UplinkPowerReportingStatus::getRequesterVictimCellFdn));
        checkChangeReportStatus(changeStatusReport.get(0), FDN1, 1, 2, rollbackStatus);
        checkChangeReportStatus(changeStatusReport.get(1), FDN2, 2, 2, rollbackStatus);
    }

    private void checkNoChangeReportStatus(List<UplinkPowerReportingStatus> changeStatusReport) {
        changeStatusReport.sort(Comparator.comparing(UplinkPowerReportingStatus::getRequesterVictimCellFdn));
        checkChangeReportStatus(changeStatusReport.get(0), FDN1, -1, 0, MitigationState.OBSERVATION);
        checkChangeReportStatus(changeStatusReport.get(1), FDN2, 0, 0, MitigationState.OBSERVATION);
    }

    private void checkChangeReportStatus(UplinkPowerReportingStatus changeStatusReport,
                                         String cellFdn,
                                         int requestedValueDiffFromCurrent,
                                         int requiredValueDiffFromCurrent,
                                         MitigationState rollbackStatus) {
        checkStatusAndCellFdn(changeStatusReport, cellFdn, rollbackStatus);
        assertEquals(CURRENT_P0_VALUE + requiredValueDiffFromCurrent, changeStatusReport.getRequiredPZeroNomPuschGrant());
        assertEquals(CURRENT_P0_256_VALUE + requiredValueDiffFromCurrent, changeStatusReport.getRequiredZeroUePuschOffset256Qam());
        assertEquals(CURRENT_P0_VALUE + requestedValueDiffFromCurrent, changeStatusReport.getRequestedPZeroNomPuschGrant());
        assertEquals(CURRENT_P0_256_VALUE + requestedValueDiffFromCurrent, changeStatusReport.getRequestedZeroUePuschOffset256Qam());
        checkCurrentAndOriginalReportValues(changeStatusReport);
    }

    private void checkCurrentAndOriginalReportValues(UplinkPowerReportingStatus changeStatusReport) {
        assertEquals(ORIGINAL_P0_VALUE, changeStatusReport.getOriginalPZeroNomPuschGrant());
        assertEquals(ORIGINAL_P0_256_VALUE, changeStatusReport.getOriginalPZeroUePuschOffset256Qam());
        assertEquals(CURRENT_P0_VALUE, changeStatusReport.getCurrentPZeroNomPuschGrant());
        assertEquals(CURRENT_P0_256_VALUE, changeStatusReport.getCurrentPZeroUePuschOffset256Qam());
    }

    private void checkStatusAndCellFdn(UplinkPowerReportingStatus changeStatusReport, String cellFdn, MitigationState rollbackStatus) {
        assertEquals(rollbackStatus, changeStatusReport.getMitigationState());
        assertEquals(cellFdn, changeStatusReport.getRequesterVictimCellFdn());
        assertEquals(FDN1, changeStatusReport.getCellFdn());
    }


    private void verifyNoChangesNRCellDU(MitigationState initialMitigationState,
                                         Integer requiredP0Value,
                                         Integer requiredP0256Value) {
        assertEquals(initialMitigationState, nrCellDU.getParametersChanges().getMitigationState());
        verifyNoChangesNRCellDU();
        verifyRequiredParameters(requiredP0Value, requiredP0256Value);
    }

    private void verifyRequiredParameters(Integer requiredP0Value,
                                          Integer requiredP0256Value) {
        assertEquals(requiredP0Value, nrCellDU.getParametersChanges().getPZeroNomPuschGrantChangeState().getRequiredValue());
        assertEquals(requiredP0256Value, nrCellDU.getParametersChanges().getPZeroUePuschOffset256QamChangeState().getRequiredValue());
    }

    private void verifyNoChangesNRCellDU() {
        assertEquals(CURRENT_P0_VALUE, nrCellDU.getPZeroNomPuschGrant());
        assertEquals(CURRENT_P0_256_VALUE, nrCellDU.getPZeroUePuschOffset256Qam());
        assertEquals(LAST_CHANGED_TIMESTAMP, nrCellDU.getParametersChanges().getLastChangedTimestamp());
    }

    UplinkPowerMitigationChangeHandlerTest withNrCellDU() {
        withNrCellDU(CURRENT_P0_VALUE, CURRENT_P0_256_VALUE);
        return this;
    }

    UplinkPowerMitigationChangeHandlerTest withNrCellDU(Integer currentP0Value, Integer currentP0256Value) {
        nrCellDU = new NRCellDU(FDN1);
        nrCellDU.setPZeroUePuschOffset256Qam(currentP0256Value);
        nrCellDU.setPZeroNomPuschGrant(currentP0Value);
        when(cmNrCellDuRepo.findByParametersChangesIsNotNull()).thenReturn(List.of(nrCellDU));
        return this;
    }

    void withNoChangeRequest() {
        ParametersChanges parametersChanges = new ParametersChanges(nrCellDU);
        parametersChanges.setPZeroNomPuschGrantChangeState(getIntegerParamChangeState(ORIGINAL_P0_VALUE));
        parametersChanges.setPZeroUePuschOffset256QamChangeState(getIntegerParamChangeState(ORIGINAL_P0_256_VALUE));
        parametersChanges.setLastChangedTimestamp(LAST_CHANGED_TIMESTAMP);
        nrCellDU.setParametersChanges(parametersChanges);
    }

    void withUnnecessaryChangeRequest() {
        withChangeRequest(CURRENT_P0_VALUE, CURRENT_P0_256_VALUE);
    }

    void withChangeRequest() {
        withChangeRequest(CURRENT_P0_VALUE + 2, CURRENT_P0_256_VALUE + 2);
    }

    void withChangeRequest(int requestedP0Value,
                           int requestedP256Value) {
        ParametersChanges parametersChanges = new ParametersChanges(nrCellDU);
        parametersChanges.setMitigationState(MitigationState.CONFIRMED);
        parametersChanges.setPZeroNomPuschGrantChangeState(getIntegerParamChangeState(UplinkPowerMitigationChangeHandlerTest.ORIGINAL_P0_VALUE));
        parametersChanges.setPZeroUePuschOffset256QamChangeState(getIntegerParamChangeState(UplinkPowerMitigationChangeHandlerTest.ORIGINAL_P0_256_VALUE));
        parametersChanges.getPZeroNomPuschGrantChangeState().getIntParamChangeRequests().addAll(List.of(
                new IntParamChangeRequest(FDN1, requestedP0Value - 1, NEIGHBOR),
                new IntParamChangeRequest(FDN2, requestedP0Value, NEIGHBOR)
        ));
        parametersChanges.getPZeroUePuschOffset256QamChangeState().getIntParamChangeRequests().addAll(List.of(
                new IntParamChangeRequest(FDN1, requestedP256Value - 1, NEIGHBOR),
                new IntParamChangeRequest(FDN2, requestedP256Value, NEIGHBOR)
        ));
        parametersChanges.setLastChangedTimestamp(LAST_CHANGED_TIMESTAMP);
        nrCellDU.setParametersChanges(parametersChanges);
    }

    IntegerParamChangeState getIntegerParamChangeState(int originalValue) {
        val integerParamChangeState = new IntegerParamChangeState();
        integerParamChangeState.setOriginalValue(originalValue);
        return integerParamChangeState;
    }

}