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

import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.report.mobility.MobilityReportingStatus;
import com.ericsson.oss.apps.model.mitigation.MobilityMitigationState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobilityMitigationHandlerTest {

    private static final String CHECK_PREVIOUS_MITIGATIONS = "checkPreviousMitigations";
    private static final String ROLLBACK_MITIGATIONS = "rollbackMitigations";
    private static final String REGISTER_NEW_MITIGATIONS = "registerNewMitigations";
    private static final String AUTHORIZE_MITIGATIONS = "authorizeMitigations";
    private static final String ROLLBACK_UNHEALTHY_CHANGES = "rollbackUnhealthyChanges";
    private static final String APPLY_MITIGATIONS = "applyMitigations";
    private static final long ROP_TIME_STAMP = 1L;

    private FeatureContext context;
    @Mock
    private MobilityMitigationPolicy policy;

    @Mock
    private MobilityMitigationState state;
    @Mock
    private MobilityMitigationPolicyBuilder cellSelectionConfig;
    @Mock
    private MobilityMitigationAction mobilityMitigationAction;
    @InjectMocks
    private MobilityMitigationHandler handler;
    @Mock
    MobilityReportingStatus mobilityReportingStatus;

    @Test
    void testHandler() {
        context = new FeatureContext(ROP_TIME_STAMP);
        when(cellSelectionConfig.buildPolicy(eq(context))).thenReturn(policy);
        when(policy.getMitigationState()).thenReturn(state);
        when(state.getMobilityMitigationRecords(ROP_TIME_STAMP)).thenReturn(List.of(mobilityReportingStatus));
        List<String> transitionOrder = new LinkedList<>();

        Mockito.doAnswer(invocation -> {transitionOrder.add(CHECK_PREVIOUS_MITIGATIONS);return null;})
                .when(policy).checkPreviousMitigations();
        Mockito.doAnswer(invocation -> {transitionOrder.add(ROLLBACK_MITIGATIONS);return null;})
                .when(policy).rollbackMitigations();
        Mockito.doAnswer(invocation -> {transitionOrder.add(REGISTER_NEW_MITIGATIONS);return null;})
                .when(policy).registerNewMitigations();
        Mockito.doAnswer(invocation -> {transitionOrder.add(AUTHORIZE_MITIGATIONS);return null;})
                .when(policy).authorizeMitigations();
        Mockito.doAnswer(invocation -> {transitionOrder.add(ROLLBACK_UNHEALTHY_CHANGES);return null;})
                .when(policy).rollbackChanges();
        Mockito.doAnswer(invocation -> {transitionOrder.add(APPLY_MITIGATIONS);return null;})
                .when(policy).applyMitigations();

        handler.handle(context);

        verify(mobilityMitigationAction, times(1)).setChangeCustomizer(any());
        assertEquals(List.of(CHECK_PREVIOUS_MITIGATIONS, ROLLBACK_MITIGATIONS, REGISTER_NEW_MITIGATIONS,
                AUTHORIZE_MITIGATIONS, ROLLBACK_UNHEALTHY_CHANGES, APPLY_MITIGATIONS), transitionOrder);

        verify(state, times(1)).getMobilityMitigationRecords(ROP_TIME_STAMP);
        assertEquals(List.of(mobilityReportingStatus), context.getMobilityReportingStatusList());
        assertEquals(List.of(CHECK_PREVIOUS_MITIGATIONS, ROLLBACK_MITIGATIONS, REGISTER_NEW_MITIGATIONS,
                AUTHORIZE_MITIGATIONS, ROLLBACK_UNHEALTHY_CHANGES, APPLY_MITIGATIONS), transitionOrder);

    }
}
