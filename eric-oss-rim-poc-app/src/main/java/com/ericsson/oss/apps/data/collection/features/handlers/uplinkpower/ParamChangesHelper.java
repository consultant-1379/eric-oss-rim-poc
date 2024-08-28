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
import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.repositories.ParameterChangesRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * helper class to filter changes and extract neighbors affected
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ParamChangesHelper {

    private final ParameterChangesRepo parameterChangesRepo;
    private final MitigationConfig mitigationConfig;


    /**
     * Scans all changes to find victim fdns and if they are within observation window
     *
     * @param ropTimeStamp current ROP
     * @return victim fdns set, victim fdns in observation window set
     */
    @NotNull
    Pair<Set<String>, Set<String>> getFdnSetByObservationState(long ropTimeStamp) {
        Map<String, Boolean> fdnToObservationState = parameterChangesRepo.findAll().stream()
                .filter(ParametersChanges::isUplinkPowerVictim)
                // never changed, this is the case when changes are requested for the first time but they fail to implement
                .filter(parametersChanges -> parametersChanges.getLastChangedTimestamp() > Long.MIN_VALUE)
                .map(parametersChanges -> Pair.of(parametersChanges.getObjectId().toFdn(), parametersChanges.getLastChangedTimestamp() + mitigationConfig.getObservationWindowMs() > ropTimeStamp))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        return Pair.of(fdnToObservationState.keySet(), fdnToObservationState.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * we get the neighbors under mitigation from the uplink power changes
     * (not changing neighbor list for cells under uplink power mitigation)
     *
     * @return map with victim cell requester as key and set of neighbors mitigates as value
     */
    Map<ManagedObjectId, Set<ManagedObjectId>> getUplinkpowerNeighborsUnderMitigation() {
        return parameterChangesRepo.findAll().stream()
                .flatMap(parameterChanges -> parameterChanges.getRequesterAsNeighborUplinkPowerSet().stream()
                        .map(managedObjectId -> Pair.of(managedObjectId, parameterChanges.getObjectId())))
                .collect(Collectors.groupingBy(Pair::getFirst, Collectors.mapping(Pair::getSecond, Collectors.toUnmodifiableSet())));
    }


}
