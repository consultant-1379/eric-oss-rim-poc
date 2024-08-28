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
package com.ericsson.oss.apps.model.mitigation.mom;

import com.ericsson.oss.apps.data.collection.features.handlers.geospatial.DistanceCalculator;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NRCellDUTests {
    private static final String FDN_TEMPLATE = "SubNetwork=GINO01,MeContext=%s,ManagedElement=%s,GNBDUFunction=1,NRCellDU=%s";
    private static final String FDN = String.format(FDN_TEMPLATE, "mino", "mino", "lino");

    @ParameterizedTest
    @CsvSource(value = {
            "TDD_SPECIAL_SLOT_PATTERN_00, false, 3",
            "TDD_SPECIAL_SLOT_PATTERN_00, true, 3",
            "TDD_SPECIAL_SLOT_PATTERN_02, false, 4",
            "TDD_SPECIAL_SLOT_PATTERN_02, true, 2",
            "TDD_SPECIAL_SLOT_PATTERN_00, false, 3",
            "TDD_SPECIAL_SLOT_PATTERN_00, true, 3",
            "TDD_SPECIAL_SLOT_PATTERN_02, false, 4",
            "TDD_SPECIAL_SLOT_PATTERN_02, true, 2",
            "TDD_SPECIAL_SLOT_PATTERN_00, false, 3",
            "TDD_SPECIAL_SLOT_PATTERN_00, true, 3",
            "TDD_SPECIAL_SLOT_PATTERN_01, false, 13",
            "TDD_SPECIAL_SLOT_PATTERN_01, true, 11",
            "TDD_SPECIAL_SLOT_PATTERN_03, false, 8",
            "TDD_SPECIAL_SLOT_PATTERN_03, true, 6",
            "TDD_SPECIAL_SLOT_PATTERN_04, false, 10",
            "TDD_SPECIAL_SLOT_PATTERN_04, true, 8",
            "TDD_SPECIAL_SLOT_PATTERN_05, false, 22",
            "TDD_SPECIAL_SLOT_PATTERN_05, true, 20",
            "TDD_SPECIAL_SLOT_PATTERN_03, false, 8",
            "TDD_SPECIAL_SLOT_PATTERN_03, true, 6",
            "TDD_SPECIAL_SLOT_PATTERN_05, false, 22",
            "TDD_SPECIAL_SLOT_PATTERN_05, true, 20"
    }, nullValues = {"null"})
    void effectiveGuardSymbolTest(
            NRCellDU.TddSpecialSlotPattern tddSpecialSlotPattern,
            Boolean advancedDlSuMimoEnabled,
            Integer result
    ) {
        NRCellDU nrCellDU = createNrCellDu(tddSpecialSlotPattern, advancedDlSuMimoEnabled);
        int guardSymbol = nrCellDU.getEffectiveGuardSymbols();
        Assertions.assertEquals(result, guardSymbol);
    }

    private NRCellDU createNrCellDu(NRCellDU.TddSpecialSlotPattern tddSpecialSlotPattern,
                                    boolean advancedDlSuMimoEnabled) {
        NRCellDU nrCellDU = new NRCellDU(FDN);
        nrCellDU.setTddSpecialSlotPattern(tddSpecialSlotPattern);
        nrCellDU.setAdvancedDlSuMimoEnabled(advancedDlSuMimoEnabled);
        nrCellDU.setSubCarrierSpacing(107);
        return nrCellDU;
    }
}
