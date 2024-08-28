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
import com.ericsson.oss.apps.data.collection.features.handlers.mobility.MobilityMitigationAction;
import com.ericsson.oss.apps.data.collection.features.handlers.mobility.MobilityMitigationHandler;
import com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.UplinkPowerMitigationChangeHandler;
import com.ericsson.oss.apps.data.collection.features.handlers.uplinkpower.UplinkPowerMitigationRequestHandler;
import com.ericsson.oss.apps.data.collection.features.report.ReportSaver;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mitigation.IntegerParamChangeState;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import com.ericsson.oss.apps.repositories.ParameterChangesRepo;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.getCellRelationChange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest(properties = "mitigation.emergencyMode=true")
public class EmergencyModeApplicatorTest {

    @InjectMocks
    private EmergencyModeApplicator emergencyModeApplicator;
    @Mock
    private ParameterChangesRepo parameterChangesRepo;
    @Mock
    private CellRelationChangeRepo cellRelationChangeRepo;
    @Mock
    private ConfigChangeImplementor changeImplementor;
    @Mock
    private MobilityMitigationAction mobilityMitigationAction;
    @Mock
    private ReportSaver reportSaver;
    @Captor
    private ArgumentCaptor<List<?>> failReportArgumentCaptor;
    @Captor
    private ArgumentCaptor<String> reportNameArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> ropArgumentCaptor;
    @Autowired
    ApplicationContext applicationContext;
    @Mock
    private ThreadingConfig threadingConfig;

    @BeforeEach
    void setup()
    {
        when(threadingConfig.getPoolSizeForULPowerMitigation()).thenReturn(2);
        when(threadingConfig.getPoolSizeForCIOMitigation()).thenReturn(2);
    }

    @Test
    void emergencyModeApplicatorTest() {
        Mockito.when(parameterChangesRepo.findAll()).thenReturn(getCellChanges(1,1));
        Mockito.when(cellRelationChangeRepo.findAll()).thenReturn(getCellRelationChanges());
        emergencyModeApplicator.applyEmergencyModules();
        Mockito.verify(changeImplementor, Mockito.times(1)).implementChange((NRCellDU) Mockito.any());
        Mockito.verify(mobilityMitigationAction, Mockito.atLeast(1)).rollBackChanges(Mockito.any());
        verify(reportSaver, times(2)).createReport(failReportArgumentCaptor.capture(), ropArgumentCaptor.capture(), reportNameArgumentCaptor.capture());
    }

    @Test
    void unloadedBeanCheck() {
        assertEquals(0, applicationContext.getBeanNamesForType(MobilityMitigationHandler.class).length);
        assertEquals(0, applicationContext.getBeanNamesForType(UplinkPowerMitigationRequestHandler.class).length);
        assertEquals(0, applicationContext.getBeanNamesForType(UplinkPowerMitigationChangeHandler.class).length);
        assertEquals(0, applicationContext.getBeanNamesForType(PendingParameterChangesApplicator.class).length);
        assertEquals(0, applicationContext.getBeanNamesForType(PendingRelationChangesApplicator.class).length);
    }

    @NotNull
    private static List<ParametersChanges> getCellChanges(Integer originalValue, Integer requiredValue) {
        NRCellDU nrCellDU = new NRCellDU(FDN1);
        ParametersChanges parametersChanges = new ParametersChanges(nrCellDU);
        parametersChanges.setMitigationState(MitigationState.ROLLBACK_FAILED);
        parametersChanges.setPZeroNomPuschGrantChangeState(new IntegerParamChangeState(){{setRequiredValue(requiredValue);setOriginalValue(originalValue);}});
        parametersChanges.setPZeroUePuschOffset256QamChangeState(new IntegerParamChangeState(){{setRequiredValue(requiredValue);setOriginalValue(originalValue);}});
        return new ArrayList<>() {{
            add(parametersChanges);
        }};
    }

    @NotNull
    private static List<CellRelationChange> getCellRelationChanges() {
        CellRelationChange cellRelationChange = getCellRelationChange(4, 24);
        cellRelationChange.setMitigationState(MitigationState.ROLLBACK_FAILED);
        return Collections.singletonList(cellRelationChange);
    }
}
