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
package com.ericsson.oss.apps.model.mitigation;

import com.ericsson.oss.apps.data.collection.features.report.mobility.MobilityReportingStatus;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class MobilityMitigationState {
    private final Map<String, List<CellRelationChange>> rollbackChanges = new HashMap<>();
    private final Map<String, List<CellRelationChange>> stepChanges = new HashMap<>();
    private final Map<String, List<CellRelationChange>> noChanges = new HashMap<>();

    public List<MobilityReportingStatus> getMobilityMitigationRecords(long ropTimeStamp) {
        return Stream.of(getMobilityMitigationRecords(ropTimeStamp, rollbackChanges, CellRelationChange::getMitigationState),
                        getMobilityMitigationRecords(ropTimeStamp, stepChanges, this::getStateForStep),
                        getMobilityMitigationRecords(ropTimeStamp, noChanges, ignored -> MitigationState.OBSERVATION))
                .flatMap(s -> s)
                .collect(Collectors.toList());
    }

    private Stream<MobilityReportingStatus> getMobilityMitigationRecords(long ropTimeStamp, Map<String, List<CellRelationChange>> victimCellFdnToRelationChangesMap,
                                                                 Function<CellRelationChange, MitigationState> stateGetter) {
        return victimCellFdnToRelationChangesMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(relationChange ->
                                {
                                    var mobilityReportingStatus = new MobilityReportingStatus(ropTimeStamp, entry.getKey(), relationChange);
                                    mobilityReportingStatus.setMitigationState(stateGetter.apply(relationChange));
                                    return mobilityReportingStatus;
                                }
                        )
                );
    }

    private MitigationState getStateForStep(CellRelationChange relationChange) {
        if (relationChange.getChangeActionReport()) {
            return relationChange.getMitigationState();
        }
        return MitigationState.OBSERVATION;
    }

}
