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
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN_TEMPLATE;
import static com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.NRCellDUParamGetterSetterFunctions.getPZeroNomPuschGrantFunctions;
import static com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.NRCellDUParamGetterSetterFunctions.getpZeroUePuschOffset256QamFunctions;
import static com.ericsson.oss.apps.model.mitigation.MitigationCellType.NEIGHBOR;
import static com.ericsson.oss.apps.model.mitigation.MitigationCellType.VICTIM;
import static com.ericsson.oss.apps.model.mitigation.MitigationState.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.config.NumericParameterConfig;
import com.ericsson.oss.apps.model.mitigation.IntParamChangeRequest;
import com.ericsson.oss.apps.model.mitigation.IntegerParamChangeState;
import com.ericsson.oss.apps.model.mitigation.MitigationCellType;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MitigationProcessorTest {

    public static final int P_ZERO_NOM_PUSCH_GRANT = -100;
    private static final int P_ZERO_OFFSET_256 = -110;
    private static final int STEP_SIZE = 4;

    @Mock
    private MitigationConfig mitigationConfig;

    @InjectMocks
    MitigationProcessor mitigationProcessor;

    @Test
    void cellsEntersMitigationNoNeighbors() {

        NRCellDU victimCell1 = new NRCellDU(FDN1);
        NRCellDU victimCell2 = new NRCellDU(FDN2);
        setCurrentP0Params(victimCell1, P_ZERO_NOM_PUSCH_GRANT, P_ZERO_OFFSET_256);
        setCurrentP0Params(victimCell2, P_ZERO_NOM_PUSCH_GRANT, P_ZERO_OFFSET_256);

        setUpUplinkPowerConfig();

        List<NRCellDU> resultCellList = mitigationProcessor.processCellsForMitigation(Map.of(victimCell1, Collections.emptyList(), victimCell2, Collections.emptyList()));

        assertEquals(2, resultCellList.size());
        verifyCellChangeRequest(victimCell1, resultCellList.get(0), victimCell1, -96, P_ZERO_NOM_PUSCH_GRANT, VICTIM, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(victimCell2, resultCellList.get(1), victimCell2, -96, P_ZERO_NOM_PUSCH_GRANT, VICTIM, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(victimCell1, resultCellList.get(0), victimCell1, -106, P_ZERO_OFFSET_256, VICTIM, getpZeroUePuschOffset256QamFunctions());
        verifyCellChangeRequest(victimCell2, resultCellList.get(1), victimCell2, -106, P_ZERO_OFFSET_256, VICTIM, getpZeroUePuschOffset256QamFunctions());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "-100, -110, -96, -106, -98",
            "-82, -110, -80, -106, -82",
            "-100, -82, -96, -80, -98"
    }, nullValues = {"null"})
    void cellsEntersMitigationNeighbors(int pZero, int pZero256, int pZeroRequestedValue, int pZero256RequestedValue, int pZeroNeighRequestedValue) {
        NRCellDU victimCell1 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx1", "mectx1", "GNBDUFunction", "NRCellDU", "cell1"));
        NRCellDU neighborCell1 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx2", "mectx2", "GNBDUFunction", "NRCellDU", "cell1"));
        NRCellDU neighborCell2 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx3", "mectx3", "GNBDUFunction", "NRCellDU", "cell1"));

        setCurrentP0Params(victimCell1, pZero, pZero256);
        setCurrentP0Params(neighborCell1, pZero, pZero256);
        setCurrentP0Params(neighborCell2, pZero, pZero256);

        setUpUplinkPowerConfig();
        List<NRCellDU> resultCellList = mitigationProcessor.processCellsForMitigation(Map.of(victimCell1, List.of(neighborCell1, neighborCell2)));

        verifyCellChangeRequest(victimCell1, resultCellList.get(0), victimCell1, pZeroRequestedValue, pZero, VICTIM, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(victimCell1, resultCellList.get(0), victimCell1, pZero256RequestedValue, pZero256, VICTIM, getpZeroUePuschOffset256QamFunctions());
        verifyCellChangeRequest(neighborCell1, resultCellList.get(1), victimCell1, pZeroNeighRequestedValue, pZero, NEIGHBOR, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(neighborCell2, resultCellList.get(2), victimCell1, pZeroNeighRequestedValue, pZero, NEIGHBOR, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(neighborCell1, resultCellList.get(1), victimCell1, pZero256, pZero256, NEIGHBOR, getpZeroUePuschOffset256QamFunctions());
        verifyCellChangeRequest(neighborCell2, resultCellList.get(2), victimCell1, pZero256, pZero256, NEIGHBOR, getpZeroUePuschOffset256QamFunctions());
    }


    @Test
    void cellsEntersMitigationCommonNeighbor() {
        NRCellDU victimCell1 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx1", "mectx1", "GNBDUFunction", "NRCellDU", "cell1"));
        NRCellDU victimCell2 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx2", "mectx2", "GNBDUFunction", "NRCellDU", "cell1"));
        NRCellDU neighborCell1 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx3", "mectx3", "GNBDUFunction", "NRCellDU", "cell1"));

        setCurrentP0Params(victimCell1, P_ZERO_NOM_PUSCH_GRANT, P_ZERO_OFFSET_256);
        setCurrentP0Params(neighborCell1, P_ZERO_NOM_PUSCH_GRANT, P_ZERO_OFFSET_256);
        // second cell is already at max P0 - 1, so should cause a smaller increment
        setCurrentP0Params(victimCell2, P_ZERO_NOM_PUSCH_GRANT + 17, P_ZERO_OFFSET_256);

        setUpUplinkPowerConfig();

        List<NRCellDU> resultCellList = mitigationProcessor.processCellsForMitigation(
                Map.of(victimCell1, List.of(neighborCell1), victimCell2, List.of(neighborCell1)));

        assertEquals(3, resultCellList.size());
        verifyCellChangeRequest(victimCell1, resultCellList.get(0), victimCell1, -96, P_ZERO_NOM_PUSCH_GRANT, VICTIM, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(victimCell2, resultCellList.get(1), victimCell2, P_ZERO_NOM_PUSCH_GRANT + 20, P_ZERO_NOM_PUSCH_GRANT + 17, VICTIM, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(neighborCell1, resultCellList.get(2), victimCell1, -98, P_ZERO_NOM_PUSCH_GRANT, NEIGHBOR, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(neighborCell1, resultCellList.get(2), victimCell2, -98, P_ZERO_NOM_PUSCH_GRANT, NEIGHBOR, getPZeroNomPuschGrantFunctions());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "2 , -96",
            "1 , -98" //we are already STEP above the original value, so there will be no increment on victim or neighbor
    })
    void secondMitigationStep(int maxSteps, int neighborExpectRequestedValue) {
        NRCellDU victimCell1 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx1", "mectx1", "GNBDUFunction", "NRCellDU", "cell1"));
        NRCellDU neighborCell1 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx2", "mectx2", "GNBDUFunction", "NRCellDU", "cell1"));

        // Setup changes
        setCurrentP0Params(victimCell1, P_ZERO_NOM_PUSCH_GRANT + STEP_SIZE, P_ZERO_OFFSET_256 + STEP_SIZE);
        setCurrentP0Params(neighborCell1, P_ZERO_NOM_PUSCH_GRANT + 2, P_ZERO_OFFSET_256 + 2);

        victimCell1.setParametersChanges(getParameterChangesVictim(victimCell1));
        neighborCell1.setParametersChanges(getParameterChangesNeighbor(victimCell1, neighborCell1));

        setUpUplinkPowerConfig(maxSteps);

        List<NRCellDU> resultCellList = mitigationProcessor.processCellsForMitigation(Map.of(victimCell1, List.of(neighborCell1)));
        assertEquals(2, resultCellList.size());

        verifyCellChangeRequest(victimCell1, resultCellList.get(0), victimCell1, Math.min(-92, P_ZERO_NOM_PUSCH_GRANT + (STEP_SIZE * maxSteps)), P_ZERO_NOM_PUSCH_GRANT, VICTIM, getPZeroNomPuschGrantFunctions());
        verifyCellChangeRequest(victimCell1, resultCellList.get(0), victimCell1, Math.min(-102, P_ZERO_OFFSET_256 + (STEP_SIZE * maxSteps)), P_ZERO_OFFSET_256, VICTIM, getpZeroUePuschOffset256QamFunctions());
        verifyCellChangeRequest(neighborCell1, resultCellList.get(1), victimCell1, neighborExpectRequestedValue, P_ZERO_NOM_PUSCH_GRANT, NEIGHBOR, getPZeroNomPuschGrantFunctions());

        assertEquals(PENDING, resultCellList.get(0).getParametersChanges().getMitigationState());
        assertEquals(PENDING, resultCellList.get(1).getParametersChanges().getMitigationState());
    }

    @Test
    void noOddChangesInP0() {
        NRCellDU victimCell1 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx1", "mectx1", "GNBDUFunction", "NRCellDU", "cell1"));
        NRCellDU neighborCell1 = new NRCellDU(String.format(FDN_TEMPLATE, "mectx2", "mectx2", "GNBDUFunction", "NRCellDU", "cell1"));

        // Setup changes
        setCurrentP0Params(victimCell1, P_ZERO_NOM_PUSCH_GRANT + STEP_SIZE, P_ZERO_OFFSET_256 + STEP_SIZE);
        setCurrentP0Params(neighborCell1, P_ZERO_NOM_PUSCH_GRANT + 2, P_ZERO_OFFSET_256 + 2);

        var victimCellParamChanges = getParameterChangesVictim(victimCell1);
        victimCell1.setParametersChanges(victimCellParamChanges);
        neighborCell1.setParametersChanges(getParameterChangesNeighbor(victimCell1, neighborCell1));

        //this way we have a +6 delta on victim compared to original value
        victimCellParamChanges.getPZeroNomPuschGrantChangeState().setOriginalValue(P_ZERO_NOM_PUSCH_GRANT+2);

        setUpUplinkPowerConfig(2);

        List<NRCellDU> resultCellList = mitigationProcessor.processCellsForMitigation(Map.of(victimCell1, List.of(neighborCell1)));
        assertEquals(2, resultCellList.size());

        verifyCellChangeRequest(victimCell1, resultCellList.get(0), victimCell1, -92, P_ZERO_NOM_PUSCH_GRANT + 2, VICTIM, getPZeroNomPuschGrantFunctions());
        // rather than getting half the delta of the victim (3) we get 2 - the next lower even number
        verifyCellChangeRequest(neighborCell1, resultCellList.get(1), victimCell1, -98, P_ZERO_NOM_PUSCH_GRANT, NEIGHBOR, getPZeroNomPuschGrantFunctions());

        assertEquals(PENDING, resultCellList.get(0).getParametersChanges().getMitigationState());
        assertEquals(PENDING, resultCellList.get(1).getParametersChanges().getMitigationState());
    }

    private void setCurrentP0Params(NRCellDU nrCellDU, Integer pZeroNomPuschGrant, Integer zeroUePuschOffset256Qam) {
        nrCellDU.setPZeroNomPuschGrant(pZeroNomPuschGrant);
        nrCellDU.setPZeroUePuschOffset256Qam(zeroUePuschOffset256Qam);
    }


    private void verifyCellChangeRequest(NRCellDU cell,
                                         NRCellDU returnedCell,
                                         NRCellDU requesterCell,
                                         int requestedValue,
                                         int originalValue,
                                         MitigationCellType mitigationCellType,
                                         NRCellDUParamGetterSetterFunctions nrCellDUParamGetterSetterFunctions) {
        assertEquals(cell, returnedCell);
        ParametersChanges parametersChanges = returnedCell.getParametersChanges();
        IntegerParamChangeState changeState = nrCellDUParamGetterSetterFunctions.getIntParamChangeStateGetter().apply(parametersChanges);
        assertEquals(PENDING, parametersChanges.getMitigationState());
        assertEquals(originalValue, changeState.getOriginalValue());
        Optional<IntParamChangeRequest> changeRequest = changeState
                .getIntParamChangeRequests()
                .stream()
                .filter(paramChangeRequest -> paramChangeRequest.getRequesterFdn().equals(requesterCell.getObjectId().toFdn()))
                .findFirst();
        assertTrue(changeRequest.isPresent());
        assertEquals(requestedValue, changeRequest.get().getRequestedValue());
        assertEquals(returnedCell.getObjectId(), parametersChanges.getObjectId());
        assertEquals(mitigationCellType, changeRequest.get().getMitigationCellType());
    }

    private void setUpUplinkPowerConfig() {
        setUpUplinkPowerConfig(2);
    }

    private void setUpUplinkPowerConfig(int maxSteps) {
        when(mitigationConfig.getPZeroNomPuschGrantDb()).thenReturn(getNumericParameterConfig(maxSteps));
        when(mitigationConfig.getPZeroUePuschOffset256QamDb()).thenReturn(getNumericParameterConfig(maxSteps));
    }

    private NumericParameterConfig<Integer> getNumericParameterConfig(int maxSteps) {
        NumericParameterConfig<Integer> numericParameterConfig = new NumericParameterConfig<>();
        numericParameterConfig.setStepSize(STEP_SIZE);
        numericParameterConfig.setMaxSteps(maxSteps);
        numericParameterConfig.setMaxAbsoluteValue(-80);
        return numericParameterConfig;
    }

    private ParametersChanges getParameterChangesVictim(NRCellDU victim) {
        ParametersChanges parametersChanges = new ParametersChanges();
        parametersChanges.setPZeroNomPuschGrantChangeState(getNumericParameterChangeState(P_ZERO_NOM_PUSCH_GRANT, -97, victim.getObjectId(), VICTIM));
        parametersChanges.setPZeroUePuschOffset256QamChangeState(getNumericParameterChangeState(P_ZERO_OFFSET_256, -107, victim.getObjectId(), VICTIM));
        parametersChanges.setObjectId(victim.getObjectId());
        parametersChanges.setMitigationState(MitigationState.CONFIRMED);
        return parametersChanges;
    }

    private ParametersChanges getParameterChangesNeighbor(NRCellDU victim, NRCellDU neighbor) {
        ParametersChanges parametersChanges = new ParametersChanges();
        parametersChanges.setPZeroNomPuschGrantChangeState(getNumericParameterChangeState(P_ZERO_NOM_PUSCH_GRANT, -98, victim.getObjectId(), NEIGHBOR));
        parametersChanges.setObjectId(neighbor.getObjectId());
        parametersChanges.setMitigationState(MitigationState.CONFIRMED);
        return parametersChanges;
    }

    private IntegerParamChangeState getNumericParameterChangeState(int originalValue,
                                                                   int requestedValue,
                                                                   ManagedObjectId requesterId,
                                                                   MitigationCellType mitigationCellType) {
        IntegerParamChangeState integerParamChangeState = new IntegerParamChangeState();
        integerParamChangeState.setOriginalValue(originalValue);
        integerParamChangeState.getIntParamChangeRequests().add(new IntParamChangeRequest(requesterId.toFdn(), requestedValue, mitigationCellType));
        return integerParamChangeState;
    }


}