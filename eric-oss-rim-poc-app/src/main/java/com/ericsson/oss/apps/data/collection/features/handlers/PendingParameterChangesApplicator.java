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
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.repositories.ParameterChangesRepo;
import com.ericsson.oss.apps.utils.MitigationUtils;
import com.ericsson.oss.apps.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.model.mitigation.MitigationState.*;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${mitigation.closeLoopMode} && !${mitigation.emergencyMode}")
public class PendingParameterChangesApplicator {
    private final ConfigChangeImplementor configChangeImplementor;
    private final ParameterChangesRepo parameterChangesRepo;
    private final ThreadingConfig threadingConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void applyPendingChanges() {
        log.info("=== Checking pending parameter changes ===");
        List<ParametersChanges> parametersChangesList = parameterChangesRepo.findByMitigationState(MitigationState.PENDING);
        log.info("Found {} parameter changes", parametersChangesList.size());
        List<ParametersChanges> failedParameterChanges = Utils.of().processInThreadPool(parametersChangesList,
                threadingConfig.getPoolSizeForULPowerMitigation(),
                this::applyPendingChangesParallel,
                null);
        failedParameterChanges.stream().findFirst().ifPresent(ignored -> log.error("Failed to apply {} uplink power changes", failedParameterChanges.size()));
    }

    @NotNull
    private List<ParametersChanges> applyPendingChangesParallel(Collection<ParametersChanges> parametersChangesList, Object ignored) {
        return parametersChangesList.parallelStream()
                .peek(parametersChanges -> log.info("Found {} cell on pending changes", parametersChanges.getNrCellDU().getObjectId().toFdn()))
                .peek(parametersChanges -> {
                    MitigationState mitigationState = MitigationUtils.mapMitigationState(configChangeImplementor.implementChange(parametersChanges.getNrCellDU()), parametersChanges.isRollback());
                    parametersChanges.setMitigationState(mitigationState);
                    parameterChangesRepo.save(parametersChanges);
                })
                .filter(parametersChanges -> parametersChanges.getMitigationState().equals(ROLLBACK_FAILED) || parametersChanges.getMitigationState().equals(CHANGE_FAILED))
                .peek(MitigationUtils::logFailedParametersChanges)
                .collect(Collectors.toList());
    }


}
