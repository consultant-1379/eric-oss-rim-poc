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
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mitigation.emergencyMode", havingValue = "false")
public class MobilityMitigationHandler implements FeatureHandler<FeatureContext> {

    private final MobilityMitigationPolicyBuilder policyBuilder;
    private final MobilityMitigationAction mitigationAction;

    @Value("${app.local:false}")
    private boolean localMode;

    @Override
    public void handle(FeatureContext context) {
        mitigationAction.setChangeCustomizer(change ->
                change.setLastChangedTimestamp(localMode ? context.getRopTimeStamp() : System.currentTimeMillis()));
        MobilityMitigationPolicy policy = policyBuilder.buildPolicy(context);
        policy.checkPreviousMitigations();
        policy.rollbackMitigations();
        policy.registerNewMitigations();
        policy.authorizeMitigations();
        policy.rollbackChanges();
        policy.applyMitigations();
        context.getMobilityReportingStatusList().addAll(policy.getMitigationState().getMobilityMitigationRecords(context.getRopTimeStamp()));
    }

    @Override
    public int getPriority() {
        return 170;
    }
}
