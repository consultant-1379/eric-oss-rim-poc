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

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

class ParametersChangesTest {

    @ParameterizedTest
    @CsvSource(value = {
            "VICTIM, NEIGHBOR, true",
            "NEIGHBOR, VICTIM, true",
            "NEIGHBOR, NEIGHBOR, false",
            "null, NEIGHBOR, false",
            "NEIGHBOR, null, false",
            "null, null, false",
            "VICTIM, VICTIM, true"
    }, nullValues = {"null"})
    void isUplinkPowerVictim(MitigationCellType pZeroMitigationCellType, MitigationCellType p256MitigationCellType, boolean result) {
        assertEquals(result, getParametersChanges(pZeroMitigationCellType, p256MitigationCellType).isUplinkPowerVictim());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "VICTIM, NEIGHBOR, 2",
            "NEIGHBOR, VICTIM, 1",
            "NEIGHBOR, NEIGHBOR, both ",
            "null, NEIGHBOR, 2",
            "NEIGHBOR, null, 1",
            "null, null, none",
            "VICTIM, VICTIM, none"
    }, nullValues = {"null"})
    void getRequesterUplinkPowerNeighborSet(MitigationCellType pZeroMitigationCellType, MitigationCellType p256MitigationCellType, String result) {
        val requesterNeighbor = getParametersChanges(pZeroMitigationCellType, p256MitigationCellType).getRequesterAsNeighborUplinkPowerSet();
        switch (result) {
            case ("none"):
                assertTrue(requesterNeighbor.isEmpty());
                break;
            case ("1"): {
                assertEquals(1, requesterNeighbor.size());
                assertTrue(requesterNeighbor.contains(ManagedObjectId.of(FDN1 + 1)));
                break;
            }
            case ("2"): {
                assertEquals(1, requesterNeighbor.size());
                assertTrue(requesterNeighbor.contains(ManagedObjectId.of(FDN1 + 2)));
                break;
            }
            case ("both"): {
                assertEquals(2, requesterNeighbor.size());
                assertTrue(requesterNeighbor.contains(ManagedObjectId.of(FDN1 + 2)));
                assertTrue(requesterNeighbor.contains(ManagedObjectId.of(FDN1 + 1)));
            }
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
            "VICTIM, NEIGHBOR, 1, 2, true",
            "VICTIM, NEIGHBOR, 2, 1, true",
            "VICTIM, NEIGHBOR, 3, null, false",
            "null, null, 1, null, false",
            "null, NEIGHBOR, 1, null, false"
    }, nullValues = {"null"})
    void removeRequests(MitigationCellType pZeroMitigationCellType, MitigationCellType p256MitigationCellType, int removeRequestIdx, Integer leftRequestIdx, boolean result) {
        ParametersChanges parametersChanges = getParametersChanges(pZeroMitigationCellType, p256MitigationCellType);
        boolean hasChanged = parametersChanges.removeRequests(Set.of(
                new IntParamChangeRequest(FDN1 + removeRequestIdx, 0, MitigationCellType.VICTIM)));
        assertEquals(result, hasChanged);
        if (leftRequestIdx != null) {
            assertTrue(extractIntParamChangeRequest(parametersChanges).contains(FDN1 + leftRequestIdx));
        }
    }


    private ParametersChanges getParametersChanges(MitigationCellType pZeroMitigationCellType, MitigationCellType p256MitigationCellType) {
        ParametersChanges parametersChanges = new ParametersChanges();
        if (pZeroMitigationCellType != null || p256MitigationCellType != null) {
            parametersChanges.setPZeroNomPuschGrantChangeState(getIntParamChangeState(pZeroMitigationCellType, FDN1 + 1));
            parametersChanges.setPZeroUePuschOffset256QamChangeState(getIntParamChangeState(p256MitigationCellType, FDN1 + 2));
        }
        return parametersChanges;
    }

    private IntegerParamChangeState getIntParamChangeState(MitigationCellType mitigationCellType, String requesterFdn) {
        IntegerParamChangeState integerParamChangeState = new IntegerParamChangeState();
        if (mitigationCellType != null) {
            integerParamChangeState.getIntParamChangeRequests().add(new IntParamChangeRequest(requesterFdn, 0, mitigationCellType));
        }
        return integerParamChangeState;
    }

    private Set<String> extractIntParamChangeRequest(ParametersChanges parametersChanges) {
        val result = (parametersChanges.getPZeroUePuschOffset256QamChangeState() != null) ?
                parametersChanges.getPZeroUePuschOffset256QamChangeState().getIntParamChangeRequests() : Collections.<IntParamChangeRequest>emptySet();
        result.addAll((parametersChanges.getPZeroNomPuschGrantChangeState() != null) ?
                parametersChanges.getPZeroNomPuschGrantChangeState().getIntParamChangeRequests() : Collections.emptySet());
        return result.stream().map(IntParamChangeRequest::getRequesterFdn).collect(Collectors.toUnmodifiableSet());
    }

}