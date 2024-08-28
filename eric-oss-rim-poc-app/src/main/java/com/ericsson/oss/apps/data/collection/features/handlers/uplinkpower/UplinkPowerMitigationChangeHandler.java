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
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import com.ericsson.oss.apps.data.collection.features.report.uplinkpower.UplinkPowerReportingStatus;
import com.ericsson.oss.apps.model.mitigation.IntegerParamChangeState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.repositories.CmNrCellDuRepo;
import com.ericsson.oss.apps.utils.Utils;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.model.mitigation.MitigationState.*;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mitigation.emergencyMode", havingValue = "false")
public class UplinkPowerMitigationChangeHandler implements FeatureHandler<FeatureContext> {

    private final ConfigChangeImplementor configChangeImplementor;

    private final CmNrCellDuRepo cmNrCellDuRepo;

    private final ThreadingConfig threadingConfig;
    private final Counter numCellP0MitigationVictimSucc;
    private final Counter numCellP0MitigationVictimFailed;
    private final Counter numCellP0MitigationNeighborSucc;
    private final Counter numCellP0MitigationNeighborFailed;
    private final Counter numCellP0RollbackSucc;
    private final Counter numCellP0RollbackFailed;

    @Value("${app.local:false}")
    private boolean localMode;
    @Value("${mitigation.closeLoopMode}")
    private boolean closedLoopMode;
    private long ropTimeStamp;

    private long numCellChangeNotMitigated;

    @Override
    public void handle(FeatureContext context) {
        numCellChangeNotMitigated = 0;
        ropTimeStamp = context.getRopTimeStamp();
        List<NRCellDU> nrCellDUWithParamChanges = cmNrCellDuRepo.findByParametersChangesIsNotNull();
        log.info("Found {} cells with potential parameter changes", nrCellDUWithParamChanges.size());
        List<UplinkPowerReportingStatus> uplinkPowerReportingStatusCollection = Utils.of().processInThreadPool(nrCellDUWithParamChanges,
                threadingConfig.getPoolSizeForULPowerMitigation(),
                this::change, null);
        context.getUplinkPowerReportingStatusList().addAll(uplinkPowerReportingStatusCollection);
        if (closedLoopMode) {
            log.error("Number Cells where Uplink Power not mitigated = {}, closedLoopMode = {}", numCellChangeNotMitigated, closedLoopMode);
        } else {
            log.info("Number Cells where Uplink Power not mitigated = {}, closedLoopMode = {}", numCellChangeNotMitigated, closedLoopMode);
        }
        log.info(
                "numCellP0MitigationVictimSucc: {} " +
                        "numCellP0MitigationVictimFailed: {} " +
                        "numCellP0MitigationNeighborSucc: {} " +
                        "numCellP0MitigationNeighborFailed: {} " +
                        "numCellP0RollbackSucc: {} " +
                        "numCellP0RollbackFailed: {}",
                numCellP0MitigationVictimSucc.count(),
                numCellP0MitigationVictimFailed.count(),
                numCellP0MitigationNeighborSucc.count(),
                numCellP0MitigationNeighborFailed.count(),
                numCellP0RollbackSucc.count(),
                numCellP0RollbackFailed.count());
    }

    private List<UplinkPowerReportingStatus> change(Collection<NRCellDU> nrCellDUWithParam, Object ignored) {
        return nrCellDUWithParam.parallelStream()
                // first set the new required values
                .peek(nrCellDU -> {
                    nrCellDU.getParametersChanges().getPZeroUePuschOffset256QamChangeState().calculateRequiredParameterValue();
                    nrCellDU.getParametersChanges().getPZeroNomPuschGrantChangeState().calculateRequiredParameterValue();
                })
                .flatMap(nrCellDU -> {
                    Collection<UplinkPowerReportingStatus> uplinkPowerReportingStatusCollection;
                    // rollback case
                    if (nrCellDU.getParametersChanges().isRollback()) {
                        uplinkPowerReportingStatusCollection = rollbackCell(nrCellDU);
                    }
                    // something changed
                    else if (nrCellDU.getParametersChanges().isRequestingChanges()) {
                        uplinkPowerReportingStatusCollection = changeCell(nrCellDU);
                    } else {
                        // nothing to change, just reporting
                        nrCellDU.getParametersChanges().setMitigationState(OBSERVATION);
                        uplinkPowerReportingStatusCollection = fillUplinkPowerReportingStatusList(nrCellDU).stream()
                                .peek(uplinkPowerReportingStatus -> setReportStatusAndTime(nrCellDU.getParametersChanges(), uplinkPowerReportingStatus))
                                .collect(Collectors.toList());
                    }
                    cmNrCellDuRepo.save(nrCellDU);
                    return uplinkPowerReportingStatusCollection.stream();
                }).collect(Collectors.toList());
    }

    private Collection<UplinkPowerReportingStatus> changeCell(NRCellDU nrCellDU) {
        Collection<UplinkPowerReportingStatus> uplinkPowerReportingStatusCollection = fillUplinkPowerReportingStatusList(nrCellDU);
        if (configChangeImplementor.implementChange(nrCellDU)) {
            updateNRCellDUParameters(nrCellDU);
            long changeTimeStamp = getChangeTimestamp();
            nrCellDU.getParametersChanges().setLastChangedTimestamp(changeTimeStamp);
            nrCellDU.getParametersChanges().setMitigationState(CONFIRMED);
            log.debug("Cell {} successfully mitigated", nrCellDU.getObjectId().toFdn());
            if (nrCellDU.getParametersChanges().isUplinkPowerVictim()) {
                numCellP0MitigationVictimSucc.increment();
            } else {
                numCellP0MitigationNeighborSucc.increment();
            }
        } else {
            nrCellDU.getParametersChanges().setMitigationState(CHANGE_FAILED);
            numCellChangeNotMitigated++;
            log.debug("Cell {} not mitigated", nrCellDU.getObjectId().toFdn());
            if (nrCellDU.getParametersChanges().isUplinkPowerVictim()) {
                numCellP0MitigationVictimFailed.increment();
            } else {
                numCellP0MitigationNeighborFailed.increment();
            }
        }
        uplinkPowerReportingStatusCollection
                .forEach(uplinkPowerReportingStatus -> setReportStatusAndTime(nrCellDU.getParametersChanges(), uplinkPowerReportingStatus));
        return uplinkPowerReportingStatusCollection;
    }

    private long getChangeTimestamp() {
        return localMode ? ropTimeStamp : System.currentTimeMillis();
    }


    private List<UplinkPowerReportingStatus> rollbackCell(NRCellDU nrCellDU) {
        UplinkPowerReportingStatus uplinkPowerReportingStatus = fillUplinkPowerReportingStatus(nrCellDU, null);
        // if it's a rollback but not requesting any change there is no need
        // to push the change to NCMP. We can just clean up.
        if (!nrCellDU.getParametersChanges().isRequestingChanges() || configChangeImplementor.implementChange(nrCellDU)) {
            updateNRCellDUParameters(nrCellDU);
            nrCellDU.getParametersChanges().setMitigationState(ROLLBACK_SUCCESSFUL);
            setReportStatusAndTime(nrCellDU.getParametersChanges(), uplinkPowerReportingStatus);
            nrCellDU.setParametersChanges(null);
            log.debug("Cell {} successfully rolled back", nrCellDU.getObjectId().toFdn());
            numCellP0RollbackSucc.increment();
        } else {
            nrCellDU.getParametersChanges().setMitigationState(ROLLBACK_FAILED);
            setReportStatusAndTime(nrCellDU.getParametersChanges(), uplinkPowerReportingStatus);
            log.warn("Cell {} failed to rollback", nrCellDU.getObjectId().toFdn());
            numCellP0RollbackFailed.increment();
        }
        return List.of(uplinkPowerReportingStatus);
    }

    private void setReportStatusAndTime(ParametersChanges parametersChanges,
                                        UplinkPowerReportingStatus uplinkPowerReportingStatus) {
        uplinkPowerReportingStatus.setMitigationState(parametersChanges.getMitigationState());
        uplinkPowerReportingStatus.setChangedTimestamp(parametersChanges.getLastChangedTimestamp());
        uplinkPowerReportingStatus.setCurrentTimestamp(getChangeTimestamp());
    }

    private void updateNRCellDUParameters(NRCellDU nrCellDU) {
        ParametersChanges parametersChanges = nrCellDU.getParametersChanges();
        nrCellDU.setPZeroNomPuschGrant(parametersChanges.getPZeroNomPuschGrantChangeState().getRequiredValue());
        nrCellDU.setPZeroUePuschOffset256Qam(parametersChanges.getPZeroUePuschOffset256QamChangeState().getRequiredValue());
    }


    private Collection<UplinkPowerReportingStatus> fillUplinkPowerReportingStatusList(NRCellDU nrCellDU) {
        ParametersChanges parametersChanges = nrCellDU.getParametersChanges();
        Set<String> requesterSet = parametersChanges.getRequesterAsSet().stream()
                .map(ManagedObjectId::toFdn)
                .collect(Collectors.toUnmodifiableSet());
        Map<String, UplinkPowerReportingStatus> fdnToUplinkPowerReportingStatus = requesterSet.stream()
                .map(requesterFdn -> fillUplinkPowerReportingStatus(nrCellDU, requesterFdn))
                .collect(Collectors.toMap(UplinkPowerReportingStatus::getRequesterVictimCellFdn, Function.identity()));
        setRequestedValue(parametersChanges.getPZeroNomPuschGrantChangeState(), fdnToUplinkPowerReportingStatus, UplinkPowerReportingStatus::setRequestedPZeroNomPuschGrant);
        setRequestedValue(parametersChanges.getPZeroUePuschOffset256QamChangeState(), fdnToUplinkPowerReportingStatus, UplinkPowerReportingStatus::setRequestedZeroUePuschOffset256Qam);
        return fdnToUplinkPowerReportingStatus.values();
    }

    private UplinkPowerReportingStatus fillUplinkPowerReportingStatus(NRCellDU nrCellDU, String fdn) {
        ParametersChanges parametersChanges = nrCellDU.getParametersChanges();
        String victimFdn = nrCellDU.getObjectId().toFdn();
        return UplinkPowerReportingStatus.builder()
                .cellFdn(victimFdn)
                .currentPZeroNomPuschGrant(nrCellDU.getPZeroNomPuschGrant())
                .currentPZeroUePuschOffset256Qam(nrCellDU.getPZeroUePuschOffset256Qam())
                .requesterVictimCellFdn(fdn)
                .isVictim(victimFdn.equals(fdn))
                .originalPZeroNomPuschGrant(parametersChanges.getPZeroNomPuschGrantChangeState().getOriginalValue())
                .requiredPZeroNomPuschGrant(parametersChanges.getPZeroNomPuschGrantChangeState().getRequiredValue())
                .originalPZeroUePuschOffset256Qam(parametersChanges.getPZeroUePuschOffset256QamChangeState().getOriginalValue())
                .requiredZeroUePuschOffset256Qam(parametersChanges.getPZeroUePuschOffset256QamChangeState().getRequiredValue())
                .build();
    }

    private void setRequestedValue(IntegerParamChangeState integerParamChangeState,
                                   Map<String, UplinkPowerReportingStatus> fdnToUplinkPowerReportingStatus,
                                   ObjIntConsumer<UplinkPowerReportingStatus> requestedParameterSetter
    ) {
        integerParamChangeState.getIntParamChangeRequests().stream()
                .filter(intParamChangeRequest -> fdnToUplinkPowerReportingStatus.containsKey(intParamChangeRequest.getRequesterFdn()))
                .forEach(intParamChangeRequest -> requestedParameterSetter.accept(fdnToUplinkPowerReportingStatus.get(intParamChangeRequest.getRequesterFdn()), intParamChangeRequest.getRequestedValue()));
    }

    @Override
    public int getPriority() {
        return 182;
    }

    @Override
    public boolean isLast(FeatureContext context) {
        return false;
    }

}
