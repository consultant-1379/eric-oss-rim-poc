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
package com.ericsson.oss.apps.classification;

import com.ericsson.oss.apps.client.CtsClient;
import com.ericsson.oss.apps.client.NcmpClient;
import com.ericsson.oss.apps.model.CGI;
import com.ericsson.oss.apps.model.mom.*;
import com.ericsson.oss.apps.utils.LockByKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CellRelationService {

    private final EntityManager entityManager;
    private final AllowedCellService allowedCellService;
    private final CtsClient ctsClient;
    private final NcmpClient ncmpClient;

    Pattern pattern = Pattern.compile(".*NRCellRelation=auto\\d+$");

    // Todo: We might need to create additional indexes.
    public Optional<NRCellDU> getCellDUByCellCU(NRCellCU nrCellCU) {
        TypedQuery<NRCellDU> query = entityManager.createQuery(
                "SELECT cellDU FROM NRCellDU cellDU JOIN NRCellCU cellCU "
                        + "ON cellCU.nCI = cellDU.nCI AND cellCU.objectId.meFdn = cellDU.objectId.meFdn "
                        + "WHERE cellCU.objectId = :objectIds",
                NRCellDU.class);
        query.setParameter("objectIds", nrCellCU.getObjectId());
        return query.getResultList().stream().findFirst();
    }

    /**
     * The purpose of this function to map cell relations to cells. This is needed, because most of the case the referenced cell
     * is under another ME and then the relation is pointing to a proxy object, which only holds information about CGI.
     * If only CGI is available, then this functions try to find the corresponding cell for it.
     * This function filters the target cells based on the allow list.
     *
     * @param nrCellDU the cell for which we need the relations mapping
     * @return The map which maps relations to cells.
     */
    @NotNull
    public Map<NRCellRelation, NRCellCU> getAllowedCellRelationMap(NRCellDU nrCellDU, LockByKey<String> lockByKey) {
        return getNrCellRelationNRCellCUMap(nrCellDU, this::nrAllowedCellCU, lockByKey);
    }

    private Optional<NRCellCU> nrAllowedCellCU(NRCellCU nrCellCU) {
        return getCellDUByCellCU(nrCellCU)
                .filter(targetNrCellDU -> allowedCellService.isAllowed(targetNrCellDU.getObjectId()))
                .map(targetNrCellDU -> nrCellCU);
    }

    /**
     * A single cell relation is unidirectional, but if the counterpart of the relation exist, together they form
     * a bidirectional cell relation. Certain use cases are only working with bidirectional relations.
     * A bidirectional relation is only valid if the CIO parameter in the relation and in its counterpart is equal
     * in absolute but different in sign. The purpose of this function is to filter out relations that are having
     * their valid counterparts from the cellRelationMap and then map the relations together.
     *
     * @param cellRelationMap The map which maps relations to cells.
     * @return The map which maps relations to their counter relation.
     */
    @NotNull
    public Map<NRCellRelation, NRCellRelation> mapMutualRelations(Map<NRCellRelation, NRCellCU> cellRelationMap) {
        Map<NRCellRelation, NRCellRelation> mutualRelationMap = new HashMap<>();

        cellRelationMap.entrySet().parallelStream().forEach(relation -> {
            CGI sourceCGI = relation.getKey().getSourceCGI();
            getCellRelationBySourceCgiAndTargetCell(sourceCGI, relation.getValue())
                    .filter(mutualRelation -> relation.getKey().getCellIndividualOffsetNR() != null && mutualRelation.getCellIndividualOffsetNR() != null)
                    .filter(mutualRelation -> (relation.getKey().getCellIndividualOffsetNR() + mutualRelation.getCellIndividualOffsetNR()) == 0)
                    .ifPresentOrElse(mutualRelation -> mutualRelationMap.put(relation.getKey(), mutualRelation),
                            () -> log.debug("No reverse relation found for {}", relation));
        });

        return mutualRelationMap;
    }


    @org.jetbrains.annotations.NotNull
    private Map<NRCellRelation, NRCellCU> getNrCellRelationNRCellCUMap(NRCellDU nrCellDU, Function<NRCellCU, Optional<NRCellCU>> allowedFilterFunction, LockByKey<String> lockByKey) {
        Map<NRCellRelation, NRCellCU> cellRelationMap = new HashMap<>();

        listCellRelationByCellDU(nrCellDU).forEach(relation -> {
            AbstractNRCellCU targetCell = relation.getTargetCell();
            Consumer<NRCellCU> addToMap = relatedCell -> allowedFilterFunction.apply(relatedCell)
                    .ifPresent(acceptedCell -> cellRelationMap.put(relation, relatedCell));

            if (targetCell instanceof NRCellCU) {
                addToMap.accept((NRCellCU) targetCell);
            } else if (targetCell != null) {
                CGI targetCGI = relation.getTargetCGI();
                // this is a hack to work on netsim
                // see comments in isCellIdBarred
                if (allowedCellService.isCellIdBarred(targetCGI.getCellLocalId())) {
                    return;
                }
                String nodeGI = targetCGI.getNetFunctionCon();
                try {
                    lockByKey.lock(nodeGI);
                    getCellCUByCGI(targetCGI).ifPresentOrElse(addToMap, () -> syncExternalData(targetCGI, addToMap));
                } finally {
                    lockByKey.unlock(nodeGI);
                }
            } else {
                log.warn("Cannot resolve relation {} target cell {}, there is no target cell for the relation in the node!",
                        relation.getObjectId().toFdn(),
                        relation.getNRCellRef().toFdn());
            }

        });

        List<NRCellRelation> uniqueRelationList = getUniqueRelations(cellRelationMap.keySet());

        return uniqueRelationList.stream()
                .map(relation -> Pair.of(relation, cellRelationMap.get(relation)))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    private List<NRCellRelation> getUniqueRelations(Collection<NRCellRelation> cellRelationCollection) {
        return cellRelationCollection.stream()
                // group by target cgi
                .map(relation -> Pair.of(relation.getTargetCell().getCGI(), relation))
                .collect(Collectors.groupingBy(Pair::getFirst, Collectors.mapping(Pair::getSecond, Collectors.toList())))
                .values()
                .stream()
                .filter(Objects::nonNull)
                .filter(relationList -> !relationList.isEmpty())
                .peek(relationList -> {
                    if (relationList.size() > 1) {
                        relationList.forEach(relation -> log.warn("Duplicated relation {} to target {} ",
                                relation.getObjectId(),
                                relation.getTargetCell().getCGI().getNetFunctionCon()));
                    }
                })
                //check if there is an auto relation, if not return the first of the list
                .map(this::selectUniqueRelation)
                .collect(Collectors.toList());
    }

    private NRCellRelation selectUniqueRelation(List<NRCellRelation> cellRelationCollection) {
        //Sort to make the choice deterministic
        cellRelationCollection.sort(Comparator.comparing(relation -> relation.getObjectId().getRefId()));
        return cellRelationCollection.stream()
                .filter(relation -> pattern.matcher(relation.getObjectId().getRefId()).matches())
                .findFirst().orElse(cellRelationCollection.get(0));
    }

    private List<NRCellRelation> listCellRelationByCellDU(NRCellDU nrCellDU) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<NRCellRelation> cq = cb.createQuery(NRCellRelation.class);
        Root<NRCellRelation> root = cq.from(NRCellRelation.class);

        return entityManager.createQuery(
                cq.select(root).where(cb.and(
                        cb.equal(root.get(NRCellRelation_.CELL).get(NRCellCU_.N_CI), nrCellDU.getNCI()),
                        cb.equal(root.get(ManagedObject_.OBJECT_ID).get(ManagedObjectId_.ME_FDN), nrCellDU.getObjectId().getMeFdn())
                ))).getResultList();
    }

    // Todo: We might need to create additional indexes.
    private Optional<NRCellRelation> getCellRelationBySourceCgiAndTargetCell(CGI sourceCGI, NRCellCU targetCell) {
        TypedQuery<NRCellRelation> query = entityManager.createQuery(
                "SELECT relation FROM NRCellRelation relation "
                        + "WHERE relation.cell.objectId = :objectId AND relation.targetCell.cellLocalId = :cellLocalId "
                        + "AND relation.targetCell.node.gNBId = :gNBId AND relation.targetCell.node.gNBIdLength = :gNBIdLength "
                        + "AND relation.targetCell.node.pLMNId = :pLMNId",
                NRCellRelation.class);

        query.setParameter("objectId", targetCell.getObjectId());
        query.setParameter("cellLocalId", sourceCGI.getCellLocalId());
        query.setParameter("gNBId", sourceCGI.getGNBId());
        query.setParameter("gNBIdLength", sourceCGI.getGNBIdLength());
        query.setParameter("pLMNId", new PLMNId(sourceCGI.getMcc(), sourceCGI.getMnc()));

        List<NRCellRelation> relationList = query.getResultList();
        if (relationList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(selectUniqueRelation(relationList));
    }

    private Optional<NRCellCU> getCellCUByCGI(CGI cellId) {
        TypedQuery<NRCellCU> query = entityManager.createQuery(
                "SELECT cell FROM NRCellCU cell "
                        + "WHERE cell.cellLocalId = :cellLocalId AND cell.node.gNBId = :gNBId AND "
                        + "cell.node.gNBIdLength = :gNBIdLength AND cell.node.pLMNId = :pLMNId",
                NRCellCU.class);

        query.setParameter("pLMNId", new PLMNId(cellId.getMcc(), cellId.getMnc()));
        query.setParameter("gNBId", cellId.getGNBId());
        query.setParameter("gNBIdLength", cellId.getGNBIdLength());
        query.setParameter("cellLocalId", cellId.getCellLocalId());
        return query.getResultList().stream().findAny();
    }

    private void syncExternalData(CGI targetCGI, Consumer<NRCellCU> consumer) {
        log.debug("target CGI {} not found locally, trying to sync from NCMP", targetCGI.toString());
        ctsClient.getGNBCmHandleByCGI(targetCGI)
                .stream().peek(ncmpClient::syncMeContext)
                .forEach(handle -> {
                    Optional<NRCellCU> targetNRCellCU = getCellCUByCGI(targetCGI);
                    targetNRCellCU.ifPresentOrElse(
                            // add to the map (if in allow list)
                            consumer,
                            // this should not really happen! it means CGI  is misconfigured or missing in CM.
                            () -> log.warn("Cannot resolve targetCGI {} after sync from NCMP!", targetCGI));
                });
    }
}
