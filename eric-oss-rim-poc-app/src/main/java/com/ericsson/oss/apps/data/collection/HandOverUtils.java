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
package com.ericsson.oss.apps.data.collection;

import com.ericsson.oss.apps.data.collection.features.handlers.RimHandlerException;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineHoCoefficient;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.repositories.PmBaselineHoCoefficientRepo;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class HandOverUtils {

    public static Map<String, HandOvers> filterHoCoefficeintFromBaseLine(Map<NRCellRelation, NRCellCU> cellRelationMap,
                                                                         final PmBaselineHoCoefficientRepo pmBaselineHoCoefficientRepo,
                                                                         boolean retainHoFdnAsIs) {

        //Filter Intra Cells Relations
        Map<NRCellRelation, String> victimNrCellRelationsMap = cellRelationMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toFdn()));

        if (victimNrCellRelationsMap.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            checkIfOriginalVictimCellIsUnique(victimNrCellRelationsMap.keySet());
        } catch (RimHandlerException rimEx) {
            log.error("Found Multiple Victim Cells for given list of Mitigation Cell Neighbors. Will Return Empty Handover Map."
                    + " victimNrCellRelationsMap = {}, Exception:", victimNrCellRelationsMap, rimEx);
            return Collections.emptyMap();
        }

        AtomicInteger totalHo = new AtomicInteger(0);
        Map<String, String> originalToAutoFdnMap = relationResolver(cellRelationMap, retainHoFdnAsIs);

        Map<String, PmBaselineHoCoefficient> pmBaselineHoCoefficientMap = pmBaselineHoCoefficientRepo
                .findAllById(originalToAutoFdnMap.keySet().stream().sorted().collect(Collectors.toList()))
                .stream()
                .filter(pmBaselineHoCoefficient -> pmBaselineHoCoefficient.getNumberHandovers() > 0)
                .peek(HandOverUtils::doPmBaselineHoCoefficentLookupPrint)
                .peek(pmBaselineHoCoefficient -> totalHo.getAndAdd(pmBaselineHoCoefficient.getNumberHandovers()))
                .collect(Collectors.toMap(pmBaselineHoCoefficient -> originalToAutoFdnMap.get(pmBaselineHoCoefficient.getFdnNrCellRelation()), Function.identity()));

        Map<String, HandOvers> hoMap = pmBaselineHoCoefficientMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> new HandOvers(entry.getKey(), entry.getValue().getNumberHandovers(), totalHo.get())));

        if (hoMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // return Map<Neighbor Cell Fdn, HoCoefficient>
        return victimNrCellRelationsMap.entrySet()
                .stream()
                .filter(entry -> hoMap.containsKey(entry.getKey().toFdn()))
                .collect(Collectors.toMap(Map.Entry::getValue, entry -> hoMap.get(entry.getKey().toFdn())));
    }

    public static void isValidCellRelationFdn(Set<String> victimNeCellFdns) {
        if (victimNeCellFdns.size() > 1) {
            throw new RimHandlerException("Error Processing Cells for Coupling And Ranking Of Neighbor Cells For P0");
        }
    }


    public static Map<String, String> relationResolver(Map<NRCellRelation, NRCellCU> cellRelationMap, boolean retainHoFdnAsIs) {
        return cellRelationMap.entrySet().stream()
                .map(entry -> {
                    NRCellRelation relation = entry.getKey();
                    String relationFdn = relation.getObjectId().toString();
                    if (retainHoFdnAsIs) {
                        return Pair.of(relationFdn, relationFdn);
                    } else {
                        return Pair.of(String.format("%s,NRCellRelation=auto%s", relation.getObjectId().fetchParentId().toFdn(), entry.getValue().getCGI().getAutonNci()), relationFdn);
                    }
                }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (e1, e2) -> {
                    log.error("duplicated target {} {}", e1, e2);
                    return e2;
                }));
    }

    /**
     * Check here if more than one match
     * <p>
     * For a given Mitigation cell, there are many neighbors, which are not
     * unique to this Mitigation/Victim Cell, these cells can be neighbors of may cells
     * But each neighbor should have a unique 'NR cell relation' to the original
     * victim/mitigation cell. So there should be only ONE original victim/mitigation cell
     * referenced by all the NR Cell Relations for this given set of neighbors.
     *
     * @param victimNrCellRelationsFdn fdn's from victim Cell relation map
     */
    private static void checkIfOriginalVictimCellIsUnique(Set<NRCellRelation> victimNrCellRelationsFdn) throws RimHandlerException {
        Set<String> victimNeCellFdns = victimNrCellRelationsFdn.stream()
                .map(fdnNrCellRelation -> fdnNrCellRelation.getObjectId().fetchParentId().toFdn())
                .collect(Collectors.toUnmodifiableSet());
        isValidCellRelationFdn(victimNeCellFdns);
    }

    private static void doPmBaselineHoCoefficentLookupPrint(PmBaselineHoCoefficient pmBaselineHoCoefficient) {
        log.trace("\n PmBaselineHoCoefficient: Mitigation Cell NRCellRelation fdn {}, numberHandovers = {}",
                pmBaselineHoCoefficient.getFdnNrCellRelation(), pmBaselineHoCoefficient.getNumberHandovers());
    }


    //@formatter:off
    /**
     * Calculate Cumulative Distribution of the % HO for each neighbor cell.
     * Accept all entries who CDF % value is below the threshold percentage,
     * then accept One more value only.
     * This method preserves the order of the input map.
     * <p>
     * EXAMPLE 1
                    fdn1     fdn2    fdn4   fdn3    fdn5      fdn7      fdn6      fdn9      fdn8      fdn10      fdn11      fdn12
     * %              30      25      15       4       4         4         4         4         4          3          2          1
     * CDF            30      55      70      74      78        82        86        90        94         97         99        100
     * TH= 95%      Accept    Accept  Accept  Accept  Accept    Accept    Accept    Accept    Accept     Accept     Reject    Reject
     *                                                                                                     ||
     *                                                                                                   acceptOneMore
     * EXAMPLE 2
     *             fdn100      fdn200      fdn400      fdn300
     * %               50          25          21           4
     * CDF             50          75          96          100
     * TH= 95%     Accept      Accept      Accept       Reject
     *                                       ||
     *                                   acceptOneMore
     *
     * @param couplingCoefficientKP0RankedMap the resulting cropped map, with the same order as the input one.
     */
    //@formatter:on
    public static Map<String, HandOvers> filterTopPercentByCdf(Map<String, HandOvers> couplingCoefficientKP0RankedMap,
                                                               double acceptHandoversAboveHoPercent) {
        AtomicDouble total = new AtomicDouble(0);
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean acceptOneMore = new AtomicBoolean(true);
        return couplingCoefficientKP0RankedMap.entrySet().stream()
                .peek(entry -> {
                    total.getAndAdd(entry.getValue().getHoCoefficient());
                    entry.getValue().setCumulativeTotalHandoversPercent(total.get());
                })
                .peek(entry -> doHandOverLookupPrint(entry.getValue(), count.incrementAndGet()))
                .filter(entry -> acceptThresholdPlusOne(entry.getValue(), acceptOneMore, acceptHandoversAboveHoPercent))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
    }

    /**
     * Map Sorter
     *
     * @param map map to sort
     * @return map sorted Highest HoCoefficient Value first
     */
    public static Map<String, HandOvers> sortMapByValue(Map<String, HandOvers> map) {
        Comparator<HandOvers> hoComparator = Comparator.comparingDouble(HandOvers::getHoCoefficient);
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(hoComparator.reversed()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
    }

    private static boolean acceptThresholdPlusOne(HandOvers ho, AtomicBoolean acceptOneMore, double threshold) {
        if (ho.getCumulativeTotalHandoversPercent() < threshold) {
            return true;
        }
        if (acceptOneMore.get()) {
            acceptOneMore.getAndSet(false);
            return true;
        }
        return false;
    }

    private static void doHandOverLookupPrint(HandOvers handOvers, int count) {
        log.trace("KCIO {} : HandOvers: Mitigation Cell NRCellRelation fdn {}, numberHandovers = {}, numberTotalHandovers = {}, CumNumHandovers = {}, HO Coefficient = {}",
                count, handOvers.getFdnNrCellRelation(), handOvers.getNumberHandovers(), handOvers.getNumberTotalHandovers(),
                handOvers.getCumulativeTotalHandoversPercent(), handOvers.getHoCoefficient());
    }
}
