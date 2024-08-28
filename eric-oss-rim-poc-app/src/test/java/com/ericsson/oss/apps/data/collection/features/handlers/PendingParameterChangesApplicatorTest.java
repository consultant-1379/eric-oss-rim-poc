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

import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.model.mitigation.IntegerParamChangeState;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.repositories.ParameterChangesRepo;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PendingParameterChangesApplicatorTest {
    @InjectMocks
    private PendingParameterChangesApplicator pendingParameterChangesApplicator;
    @Mock
    private ParameterChangesRepo parameterChangesRepo;
    @Mock
    private ConfigChangeImplementor changeImplementor;
    @Mock
    private ThreadingConfig threadingConfig;

    @BeforeEach
    void setup()
    {
        when(threadingConfig.getPoolSizeForULPowerMitigation()).thenReturn(2);
    }


    @Test
    void applyChangesTestForEmptyRepo() {
        pendingParameterChangesApplicator.applyPendingChanges();
        Mockito.verify(changeImplementor, never()).implementChange((NRCellDU) Mockito.any());
    }

    @Test
    void applyPendingChangesTestForFilledRepo() {
        when(parameterChangesRepo.findByMitigationState(eq(MitigationState.PENDING))).thenReturn(getCellChanges(1, 2));
        pendingParameterChangesApplicator.applyPendingChanges();
        Mockito.verify(changeImplementor, Mockito.times(1)).implementChange((NRCellDU) Mockito.any());
    }

    @Test
    void applyPendingRollbackChangesTestForFilledRepo() {
        when(parameterChangesRepo.findByMitigationState(eq(MitigationState.PENDING))).thenReturn(getCellChanges(1, 1));
        pendingParameterChangesApplicator.applyPendingChanges();
        Mockito.verify(changeImplementor, Mockito.times(1)).implementChange((NRCellDU) Mockito.any());
    }

    @NotNull
    private List<ParametersChanges> getCellChanges(Integer originalValue, Integer requiredValue) {
        NRCellDU nrCellDU = new NRCellDU(FDN1);
        ParametersChanges parametersChanges = new ParametersChanges(nrCellDU);
        parametersChanges.setMitigationState(MitigationState.PENDING);
        parametersChanges.setPZeroNomPuschGrantChangeState(new IntegerParamChangeState(){{setRequiredValue(requiredValue);setOriginalValue(originalValue);}});
        parametersChanges.setPZeroUePuschOffset256QamChangeState(new IntegerParamChangeState(){{setRequiredValue(requiredValue);setOriginalValue(originalValue);}});
        return new ArrayList<>() {{
            add(parametersChanges);
        }};
    }
}
