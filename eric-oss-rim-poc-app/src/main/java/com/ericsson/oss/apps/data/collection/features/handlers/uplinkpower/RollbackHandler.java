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

import com.ericsson.oss.apps.classification.AllowedCellService;
import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDU;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import com.ericsson.oss.apps.model.mitigation.IntParamChangeRequest;
import com.ericsson.oss.apps.model.mitigation.MitigationState;
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.repositories.ParameterChangesRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mitigation.emergencyMode", havingValue = "false")
public class RollbackHandler implements FeatureHandler<FeatureContext> {

    private final ParameterChangesRepo parameterChangesRepo;
    private final AllowedCellService allowedCellService;
    private final CellSelectionConfig cellSelectionConfig;


    @Override
    public void handle(FeatureContext context) {
        List<ParametersChanges> parametersChangesList = parameterChangesRepo.findAll();
        Set<String> victimFdnsUnderMitigation = getVictimFdnsUnderMitigation(parametersChangesList);
        Set<String> fdnsRecovered = getFdnsRecovered(context.getFdnToFtRopNRCellDU().values(), victimFdnsUnderMitigation, cellSelectionConfig.getMaxDeltaIPNThresholdDb());
        log.info("{} cells found healthy", fdnsRecovered.size());
        Set<String> fdnsUnderMitigationNotInAllowedList = getFdnsUnderMitigationNotInAllowedList(victimFdnsUnderMitigation);
        log.info("{} cells found not included in the allow list", fdnsRecovered.size());
        fdnsRecovered.addAll(fdnsUnderMitigationNotInAllowedList);
        Set<String> fdnsMissingData = getFdnsMissingData(context.getFdnToFtRopNRCellDU().values(), victimFdnsUnderMitigation);
        log.info("{} cells found with insufficient data", fdnsMissingData.size());
        fdnsRecovered.addAll(fdnsMissingData);
        List<ParametersChanges> rollbackChangesList = removeRequestsFromChanges(fdnsRecovered, parametersChangesList);
        log.info("rolling back {} changes (includes neighbors)", rollbackChangesList.size());
        parameterChangesRepo.saveAll(rollbackChangesList);
    }

    private Set<String> getFdnsMissingData(Collection<FtRopNRCellDU> ftRopNrCellDuCollection, Set<String> victimFdnsUnderMitigation) {
        return ftRopNrCellDuCollection.stream()
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getNRopsInLastSeenWindow() == 0)
                .map(ftRopNRCellDU -> ftRopNRCellDU.getMoRopId().getFdn())
                .filter(victimFdnsUnderMitigation::contains)
                .collect(Collectors.toSet());
    }

    private Set<String> getFdnsRecovered(Collection<FtRopNRCellDU> ftRopNrCellDuCollection, Set<String> victimFdnsUnderMitigation, double maxDeltaIPNLowWatermark) {
        return ftRopNrCellDuCollection.stream()
                .filter(ftRopNRCellDU -> ftRopNRCellDU.getAvgSw8AvgDeltaIpN() < maxDeltaIPNLowWatermark)
                .map(ftRopNRCellDU -> ftRopNRCellDU.getMoRopId().getFdn())
                .filter(victimFdnsUnderMitigation::contains)
                .collect(Collectors.toSet());
    }

    private Set<String> getFdnsUnderMitigationNotInAllowedList(Set<String> victimFdnsUnderMitigation) {
        return victimFdnsUnderMitigation.stream()
                .filter(fdn -> !allowedCellService.isAllowed(fdn))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> getVictimFdnsUnderMitigation(List<ParametersChanges> parametersChangesList) {
        return parametersChangesList.stream()
                .filter(ParametersChanges::isUplinkPowerVictim)
                .map(parametersChanges -> parametersChanges.getObjectId().toFdn())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int getPriority() {
        return 181;
    }

    /**
     * wipes from the set of changes all the requests with ID
     * in the set of moids supplied (the cells that recovered from RI)
     * that includes the requests set on neighbors
     *
     * @param cellFdnsToRollback cells fdns to rollback
     * @param parametersChanges  the list of parameter changes to process
     * @return the list of ParametersChanges that changed as result of the operation
     */
    private List<ParametersChanges> removeRequestsFromChanges(Set<String> cellFdnsToRollback,
                                                              List<ParametersChanges> parametersChanges) {
        Set<IntParamChangeRequest> requestsToRemove = cellFdnsToRollback.stream()
                .map(fdn -> {
                    IntParamChangeRequest intParamChangeRequest = new IntParamChangeRequest();
                    intParamChangeRequest.setRequesterFdn(fdn);
                    return intParamChangeRequest;
                }).collect(Collectors.toUnmodifiableSet());
        return parametersChanges.parallelStream()
                .filter(parametersChange -> parametersChange.removeRequests(requestsToRemove))
                .peek(parametersChange -> parametersChange.setMitigationState(MitigationState.PENDING))
                .collect(Collectors.toUnmodifiableList());
    }


}
