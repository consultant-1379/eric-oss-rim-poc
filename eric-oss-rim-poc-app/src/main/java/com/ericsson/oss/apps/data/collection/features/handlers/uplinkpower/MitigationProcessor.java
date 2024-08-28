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
import com.ericsson.oss.apps.config.NumericParameterConfig;
import com.ericsson.oss.apps.model.mitigation.IntParamChangeRequest;
import com.ericsson.oss.apps.model.mitigation.IntegerParamChangeState;
import com.ericsson.oss.apps.model.mitigation.MitigationCellType;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class MitigationProcessor {

    private final MitigationConfig mitigationConfig;

    List<NRCellDU> processCellsForMitigation(Map<NRCellDU, List<NRCellDU>> cellsForMitigation) {
        return cellsForMitigation.entrySet().stream().flatMap(nrCellDUAndNeighbors -> {
                    NRCellDU nrCellDU = nrCellDUAndNeighbors.getKey();
                    List<NRCellDU> neighborCellList = nrCellDUAndNeighbors.getValue();

                    Optional<Integer> pZeroNomPuschGrantRequestedVictimIncrement = generateParameterChangesForVictimCell(nrCellDU, NRCellDUParamGetterSetterFunctions.getPZeroNomPuschGrantFunctions());
                    pZeroNomPuschGrantRequestedVictimIncrement.ifPresent(increment -> {
                        generateChangesForNeighborCells(neighborCellList, nrCellDU, increment, NRCellDUParamGetterSetterFunctions.getPZeroNomPuschGrantFunctions());
                        generateChangesForNeighborCells(neighborCellList, nrCellDU, 0, NRCellDUParamGetterSetterFunctions.getpZeroUePuschOffset256QamFunctions());
                    });
                    generateParameterChangesForVictimCell(nrCellDU, NRCellDUParamGetterSetterFunctions.getpZeroUePuschOffset256QamFunctions());
                    return Stream.concat(neighborCellList.stream(), Stream.of(nrCellDU));
                })
                // remove duplicates - this requires neighbors to be the SAME OBJECT!
                // having the same Object ID won't work and will cause problems when we save the results
                .distinct()
                // sorting by fdn to make result deterministic
                .sorted(Comparator.comparing(cell -> cell.getObjectId().toFdn()))
                .collect(Collectors.toUnmodifiableList());
    }

    private void generateChangesForNeighborCells(List<NRCellDU> nrCellDUNeighborList, NRCellDU victimCell, int victimIncrementValue, NRCellDUParamGetterSetterFunctions parameterGettterSetterFunctions) {
        int halfVictimIncrementValue = (int) Math.ceil(victimIncrementValue / 2D);
        int parameterIncrement = halfVictimIncrementValue - halfVictimIncrementValue%2;
        int maxParameterValue = parameterGettterSetterFunctions.getNumericParameterConfigGetter().apply(mitigationConfig).getMaxAbsoluteValue();
        nrCellDUNeighborList.forEach(nrCellDU -> {
            ParametersChanges parametersChanges = setParametersChangesIfNull(nrCellDU);
            Integer currentValue = parameterGettterSetterFunctions.getNrCellDuParameterGetter().apply(nrCellDU);
            if (currentValue == null) {
                log.error("Current value for neighbor cell parameter getter is null, cannot process mitigation request");
                return;
            }
            IntegerParamChangeState parameterChangeState = setParameterOriginalValueIfNull(parametersChanges, parameterGettterSetterFunctions, currentValue);
            int requestedValue = Math.min(parameterChangeState.getOriginalValue() + parameterIncrement, maxParameterValue);
            IntParamChangeRequest parameterChangeRequest = new IntParamChangeRequest(victimCell.getObjectId().toFdn(), requestedValue, MitigationCellType.NEIGHBOR);
            parameterChangeState.upsertParameterChangeRequest(parameterChangeRequest);
            parametersChanges.setMitigationState(MitigationState.PENDING);
        });
    }

    private Optional<Integer> generateParameterChangesForVictimCell(NRCellDU nrCellDU,
                                                                    NRCellDUParamGetterSetterFunctions parameterGettterSetterFunctions) {
        ParametersChanges parametersChanges = setParametersChangesIfNull(nrCellDU);
        Integer currentIntegerParameterValue = parameterGettterSetterFunctions.getNrCellDuParameterGetter().apply(nrCellDU);
        if (currentIntegerParameterValue == null) {
            log.error("Current value for victim cell parameter getter is null, cannot process mitigation request");
            return Optional.empty();
        }
        IntegerParamChangeState parameterChangeState = setParameterOriginalValueIfNull(parametersChanges, parameterGettterSetterFunctions, currentIntegerParameterValue);
        Integer originalValue = parameterChangeState.getOriginalValue();
        int requestedValue = getIntegerParameterRequestedValue(currentIntegerParameterValue, originalValue, parameterGettterSetterFunctions.getNumericParameterConfigGetter().apply(mitigationConfig));
        IntParamChangeRequest parameterChangeRequest = new IntParamChangeRequest(nrCellDU.getObjectId().toFdn(), requestedValue, MitigationCellType.VICTIM);
        parameterChangeState.upsertParameterChangeRequest(parameterChangeRequest);
        parametersChanges.setMitigationState(MitigationState.PENDING);
        return Optional.of(requestedValue - originalValue);
    }

    @NotNull
    private IntegerParamChangeState setParameterOriginalValueIfNull(ParametersChanges parametersChanges,
                                                                    NRCellDUParamGetterSetterFunctions parameterGettterSetterFunctions,
                                                                    Integer originalValue) {
        IntegerParamChangeState parameterChangeState = parameterGettterSetterFunctions.getIntParamChangeStateGetter().apply(parametersChanges);
        if (parameterChangeState.getOriginalValue() == null) {
            parameterChangeState.setOriginalValue(originalValue);
        }
        return parameterChangeState;
    }

    @NotNull
    private ParametersChanges setParametersChangesIfNull(NRCellDU nrCellDU) {
        ParametersChanges parametersChanges = nrCellDU.getParametersChanges();
        if (parametersChanges == null) {
            parametersChanges = new ParametersChanges(nrCellDU);
            nrCellDU.setParametersChanges(parametersChanges);
        }
        return parametersChanges;
    }

    private int getIntegerParameterRequestedValue(int currentValue, int originalValue, NumericParameterConfig<Integer> mitigationConfig) {
        int stepSize = mitigationConfig.getStepSize();
        return NumberUtils.min(mitigationConfig.getMaxAbsoluteValue(), currentValue + stepSize, (mitigationConfig.getMaxSteps() * stepSize) + originalValue);
    }

}
