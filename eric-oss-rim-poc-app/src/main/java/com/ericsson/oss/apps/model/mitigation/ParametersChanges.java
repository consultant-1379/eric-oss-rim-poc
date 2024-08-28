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

import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.model.mitigation.MitigationCellType.NEIGHBOR;
import static com.ericsson.oss.apps.model.mitigation.MitigationCellType.VICTIM;

/**
 * A container for the state of parameter changes and requests.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
public class ParametersChanges implements Serializable {

    private static final long serialVersionUID = 5853046871158300933L;

    @EmbeddedId
    @AttributeOverride(name = "meFdn", column = @Column(name = "nr_celldu_me_fdn"))
    @AttributeOverride(name = "refId", column = @Column(name = "nr_celldu_ref_id"))
    private ManagedObjectId objectId;

    public ParametersChanges(NRCellDU nrCellDU) {
        this.objectId = nrCellDU.getObjectId();
        this.nrCellDU = nrCellDU;
    }

    @OneToOne
    @MapsId
    private NRCellDU nrCellDU;

    @OneToOne(cascade = CascadeType.ALL)
    private IntegerParamChangeState pZeroNomPuschGrantChangeState = new IntegerParamChangeState();

    @OneToOne(cascade = CascadeType.ALL)
    private IntegerParamChangeState pZeroUePuschOffset256QamChangeState = new IntegerParamChangeState();

    private MitigationState mitigationState = MitigationState.PENDING;

    private long lastChangedTimestamp = Long.MIN_VALUE;

    /**
     * Checks if any change to the uplink power parameters has been requested as a victim.
     * Returns false if no change has been requested as victim or if there are no changes at all.
     *
     * @return true if it has any victim change requests, false otherwise
     */
    public boolean isUplinkPowerVictim() {
        return hasMitigationReasonVictim(pZeroNomPuschGrantChangeState) || hasMitigationReasonVictim(pZeroUePuschOffset256QamChangeState);
    }

    /**
     * Extract the list or requester ManagedObjectId. If this ParametersChanges is a neighbor
     * of a victim cells it would have a request with MitigationCellType.NEIGHBOR and the ManagedObjectId of the requester cell.
     *
     * @return the list of requesters (the victims)
     */
    public Set<ManagedObjectId> getRequesterAsNeighborUplinkPowerSet() {
        return Stream.concat(getRequesterAsNeighborUplinkPowerList(pZeroNomPuschGrantChangeState, intParamChangeRequest -> NEIGHBOR.equals(intParamChangeRequest.getMitigationCellType())).stream(),
                        getRequesterAsNeighborUplinkPowerList(pZeroUePuschOffset256QamChangeState, intParamChangeRequest -> NEIGHBOR.equals(intParamChangeRequest.getMitigationCellType())).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<ManagedObjectId> getRequesterAsSet() {
        return Stream.concat(getRequesterAsNeighborUplinkPowerList(pZeroNomPuschGrantChangeState, intParamChangeRequest -> true).stream(),
                        getRequesterAsNeighborUplinkPowerList(pZeroUePuschOffset256QamChangeState, intParamChangeRequest -> true).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<ManagedObjectId> getRequesterAsNeighborUplinkPowerList(IntegerParamChangeState integerParamChangeState, Predicate<IntParamChangeRequest> intParamChangeRequestPredicate) {
        return (integerParamChangeState == null) ? Collections.emptyList() : integerParamChangeState.getIntParamChangeRequests().stream()
                .filter(intParamChangeRequestPredicate)
                .map(IntParamChangeRequest::getRequesterFdn)
                .map(ManagedObjectId::of)
                .collect(Collectors.toUnmodifiableList());
    }

    public boolean removeRequests(Set<IntParamChangeRequest> requestsToRemove) {
        IntegerParamChangeState changeState = getPZeroNomPuschGrantChangeState();
        boolean removedPzeroRequest = (changeState != null) && changeState.getIntParamChangeRequests().removeAll(requestsToRemove);
        changeState = getPZeroUePuschOffset256QamChangeState();
        boolean removedPzero256Request = (changeState != null) && changeState.getIntParamChangeRequests().removeAll(requestsToRemove);
        return removedPzeroRequest || removedPzero256Request;
    }


    private boolean hasMitigationReasonVictim(IntegerParamChangeState paramChangeState) {
        if (paramChangeState == null) {
            return false;
        }
        return paramChangeState.getIntParamChangeRequests().stream()
                .anyMatch(parameterChangeRequest -> parameterChangeRequest.getMitigationCellType().equals(VICTIM));
    }

    /**
     * reset the parameters to their original values (effectively, makes this change a rollback)
     */
    public void setToOriginalValue() {
        getPZeroNomPuschGrantChangeState().setRequiredValue(getPZeroNomPuschGrantChangeState().getOriginalValue());
        getPZeroUePuschOffset256QamChangeState().setRequiredValue(getPZeroUePuschOffset256QamChangeState().getOriginalValue());
    }

    /**
     * checks if required values are the same as the original ones (effectively, if this change is a rollback)
     *
     * @return true if the required values are the same as the original ones
     */
    public boolean isRollback() {
        return getPZeroNomPuschGrantChangeState().getRequiredValue().equals(getPZeroNomPuschGrantChangeState().getOriginalValue())
                && getPZeroUePuschOffset256QamChangeState().getRequiredValue().equals(getPZeroUePuschOffset256QamChangeState().getOriginalValue());
    }

    /**
     * checks if required values are the same as the current ones
     *
     * @return true if the required values are the same as the current ones
     */
    public boolean isRequestingChanges()
    {
        return !pZeroUePuschOffset256QamChangeState.getRequiredValue().equals(nrCellDU.getPZeroUePuschOffset256Qam()) ||
                !pZeroNomPuschGrantChangeState.getRequiredValue().equals(nrCellDU.getPZeroNomPuschGrant());
    }
}
