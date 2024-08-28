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

import com.ericsson.oss.apps.config.NumericParameterConfig;
import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.model.mitigation.IntegerParamChangeState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.BiConsumer;
import java.util.function.Function;

@RequiredArgsConstructor
@Getter
class NRCellDUParamGetterSetterFunctions {

    private final Function<NRCellDU, Integer> nrCellDuParameterGetter;
    private final Function<MitigationConfig, NumericParameterConfig<Integer>> numericParameterConfigGetter;
    private final Function<ParametersChanges, IntegerParamChangeState> intParamChangeStateGetter;
    private final BiConsumer<ParametersChanges, IntegerParamChangeState> intParamChangeStateSetter;

    static NRCellDUParamGetterSetterFunctions getPZeroNomPuschGrantFunctions() {
        return new NRCellDUParamGetterSetterFunctions(
                NRCellDU::getPZeroNomPuschGrant,
                MitigationConfig::getPZeroNomPuschGrantDb,
                ParametersChanges::getPZeroNomPuschGrantChangeState,
                ParametersChanges::setPZeroNomPuschGrantChangeState);
    }

    static NRCellDUParamGetterSetterFunctions getpZeroUePuschOffset256QamFunctions() {
        return new NRCellDUParamGetterSetterFunctions(
                NRCellDU::getPZeroUePuschOffset256Qam,
                MitigationConfig::getPZeroUePuschOffset256QamDb,
                ParametersChanges::getPZeroUePuschOffset256QamChangeState,
                ParametersChanges::setPZeroUePuschOffset256QamChangeState
        );
    }
}
