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
package com.ericsson.oss.apps.data.collection.features.handlers;

import com.ericsson.oss.apps.data.collection.features.FtRopNRCellDUPair;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.*;
import static com.ericsson.oss.apps.model.mom.NRCellDU.TddSpecialSlotPattern.TDD_SPECIAL_SLOT_PATTERN_00;
import static com.ericsson.oss.apps.model.mom.NRCellDU.TddUlDlPattern.TDD_ULDL_PATTERN_00;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class TDDOverlapCalculatorTest {

    private static final long ROP_TIME_STAMP = 0L;
    private static final NRCellDU NR_CELL_DU = NRCellDU.builder()
            .subCarrierSpacing(30)
            .tddSpecialSlotPattern(TDD_SPECIAL_SLOT_PATTERN_00)
            .tddUlDlPattern(TDD_ULDL_PATTERN_00)
            .advancedDlSuMimoEnabled(Boolean.FALSE)
            .build();

    @Mock
    private Counter numCellPairsTDDOverlap;

    @InjectMocks
    private TDDOverlapCalculator tddOverlapCalculator;

    @Test
    void calculateTDDOverlap() {
        FtRopNRCellDUPair pair = buildFtRopNRCellDUPair(FDN1, FDN2);
        FeatureContext featureContext = new FeatureContext(ROP_TIME_STAMP);
        featureContext.setFtRopNRCellDUPairs(Collections.singletonList(pair));
        featureContext.getFdnToNRCellDUMap().put(FDN1, NR_CELL_DU);
        featureContext.getFdnToNRCellDUMap().put(FDN2, NR_CELL_DU);
        tddOverlapCalculator.handle(featureContext);
        assertFalse(tddOverlapCalculator.isLast(featureContext));
        List<FtRopNRCellDUPair> resultList = featureContext.getFtRopNRCellDUPairs();
        FtRopNRCellDUPair result1 = resultList.get(0);
        assertEquals(0.086, result1.getTddOverlap(), 0.001);
        Mockito.verify(numCellPairsTDDOverlap, Mockito.times(1)).increment(1);
    }


    @ParameterizedTest
    @CsvSource(value = {
            "TDD_ULDL_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_00, 15, 15, 10, 0",
            "TDD_ULDL_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_00, 15, 15, 45, 0",
            "TDD_ULDL_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_00, 30, 15, 45, 0.086",
            "TDD_ULDL_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_01, TDD_SPECIAL_SLOT_PATTERN_01, 30, 15, 130, 0.653",
            "TDD_ULDL_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_01, TDD_SPECIAL_SLOT_PATTERN_01, 30, 15, 230, 1",
            "TDD_ULDL_PATTERN_00, TDD_SPECIAL_SLOT_PATTERN_01, TDD_SPECIAL_SLOT_PATTERN_01, 30, 15, 500, 1",
            "TDD_ULDL_PATTERN_03, TDD_SPECIAL_SLOT_PATTERN_03, TDD_SPECIAL_SLOT_PATTERN_03, 30, 15, 45, 0.057",
            "TDD_ULDL_PATTERN_03, TDD_SPECIAL_SLOT_PATTERN_03, TDD_SPECIAL_SLOT_PATTERN_03, 30, 15, 500, 1",
    })
    void testCalculateTDDOverlap(NRCellDU.TddUlDlPattern tddUlDlPattern2,
                                 NRCellDU.TddSpecialSlotPattern tddSpecialSlotPattern1,
                                 NRCellDU.TddSpecialSlotPattern tddSpecialSlotPattern2,
                                 int scs1,
                                 int scs2,
                                 double distance,
                                 double result) {
        TDDOverlapCalculator.CellTDDInfo cellTDDInfo1 = getCellTddInfo(TDD_ULDL_PATTERN_00, tddSpecialSlotPattern1, scs1, NR_CELL_DU.getEffectiveGuardSymbols());
        TDDOverlapCalculator.CellTDDInfo cellTDDInfo2 = getCellTddInfo(tddUlDlPattern2, tddSpecialSlotPattern2, scs2, NR_CELL_DU.getEffectiveGuardSymbols());
        double tddOverlapCalculator = TDDOverlapCalculator.calculateTDDOverlap(distance, cellTDDInfo1, cellTDDInfo2);
        assertEquals(result, tddOverlapCalculator, 0.001D);
    }

    private TDDOverlapCalculator.CellTDDInfo getCellTddInfo(NRCellDU.TddUlDlPattern tddUlDlPattern,
                                                            NRCellDU.TddSpecialSlotPattern tddSpecialSlotPattern,
                                                            int scs, int guardSymbols) {
        return TDDOverlapCalculator.CellTDDInfo.builder()
                .subCarrierSpacing(scs)
                .tddSpecialSlotPattern(tddSpecialSlotPattern)
                .guardSymbols(guardSymbols)
                .tddUlDlPattern(tddUlDlPattern).build();
    }
}