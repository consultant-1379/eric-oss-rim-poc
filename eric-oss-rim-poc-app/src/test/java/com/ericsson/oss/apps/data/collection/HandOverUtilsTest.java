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
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.repositories.PmBaselineHoCoefficientRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.NRCELL_CU_FDN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
class HandoverUtilsTest {

    @Mock
    PmBaselineHoCoefficientRepo pmBaselineHoCoefficientRepo;

    @Test
    public void testMultipleRelationParents() {
        Map<NRCellRelation, NRCellCU> relationMap = Map.of(
                new NRCellRelation(NRCELL_CU_FDN + 1 + ",NRCellRelation=auto1"), new NRCellCU(NRCELL_CU_FDN + 1),
                new NRCellRelation(NRCELL_CU_FDN + 2 + ",NRCellRelation=auto1"), new NRCellCU(NRCELL_CU_FDN + 2));
        assertEquals(Collections.emptyMap(), HandOverUtils.filterHoCoefficeintFromBaseLine(relationMap, pmBaselineHoCoefficientRepo, false));
    }

    @Test
    public void testEmptyInput() {
        Map<NRCellRelation, NRCellCU> relationMap = Collections.emptyMap();
        assertEquals(Collections.emptyMap(), HandOverUtils.filterHoCoefficeintFromBaseLine(relationMap, pmBaselineHoCoefficientRepo, false));
    }

    @Test
    public void filterTopPercentByCdfLongTail_test() {
        Map<String, HandOvers> hoMap = new HashMap<>();
        IntStream hoValues = IntStream.of(30, 25, 15, 4, 4, 4, 4, 4, 4, 3, 2, 1);
        AtomicInteger fdnIndex = new AtomicInteger(1);
        hoValues.forEach(val -> {
            hoMap.put("fdn" + +fdnIndex.get(), new HandOvers("fdn" + fdnIndex.getAndIncrement(), val, 100));
        });

        Map<String, HandOvers> hoExpectedMap = new HashMap<>(hoMap);
        hoExpectedMap.remove("fdn12");
        hoExpectedMap.remove("fdn11");
        Map<String, HandOvers> hoMapSorted = HandOverUtils.sortMapByValue(hoMap);
        Map<String, HandOvers> resultMap = HandOverUtils.filterTopPercentByCdf(hoMapSorted, 95.0);

        assertEquals(hoExpectedMap, resultMap);
    }

    @Test
    public void filterTopPercentByCdfShortTail_test() {
        Map<String, HandOvers> hoMap = new HashMap<>();
        IntStream hoValues = IntStream.of(50, 25, 21, 4);
        hoValues.forEach(val -> {
            hoMap.put("fdn" + val, new HandOvers("fdn" + val, val, 100));
        });

        Map<String, HandOvers> hoExpectedMap = new HashMap<>(hoMap);
        hoExpectedMap.remove("fdn4");

        Map<String, HandOvers> hoMapSorted = HandOverUtils.sortMapByValue(hoMap);
        Map<String, HandOvers> resultMap = HandOverUtils.filterTopPercentByCdf(hoMapSorted, 95.0);

        assertEquals(hoExpectedMap, resultMap);
    }

    // Sonar
    @Test
    public void isValidCellRelationFdn_test() {
        try {
            Set<String> victimNrCellFdns = new HashSet<>();
            victimNrCellFdns.add("fdn1");
            victimNrCellFdns.add("fdn2");

            HandOverUtils.isValidCellRelationFdn(victimNrCellFdns);
            fail("Expected isValidCellRelationFdn to throw RimHandlerException for set with two FDN's");
        } catch (RimHandlerException rhe) {
            //expect exception
            assertEquals("Error Processing Cells for Coupling And Ranking Of Neighbor Cells For P0", rhe.getMessage());
        }
    }
}