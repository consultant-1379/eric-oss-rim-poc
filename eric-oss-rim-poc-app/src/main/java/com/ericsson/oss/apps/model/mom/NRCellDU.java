/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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
package com.ericsson.oss.apps.model.mom;

import com.ericsson.oss.apps.model.mitigation.ParametersChanges;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Slf4j
public class NRCellDU extends NRCell {

    private static final long serialVersionUID = 4139693346381992364L;

    @Getter
    @RequiredArgsConstructor
    public enum TddUlDlPattern {
        TDD_ULDL_PATTERN_00(1),
        TDD_ULDL_PATTERN_01(2),
        TDD_ULDL_PATTERN_02(1),
        TDD_ULDL_PATTERN_03(3),
        TDD_ULDL_PATTERN_04(2);

        private final int ulSlots;
    }

    @Getter
    @RequiredArgsConstructor
    public enum TddSpecialSlotPattern {
        TDD_SPECIAL_SLOT_PATTERN_00(3 , 0),
        TDD_SPECIAL_SLOT_PATTERN_01(11 , 3),
        TDD_SPECIAL_SLOT_PATTERN_02(2 , 2),
        TDD_SPECIAL_SLOT_PATTERN_03(4 , 4),
        TDD_SPECIAL_SLOT_PATTERN_04(6 , 4),
        TDD_SPECIAL_SLOT_PATTERN_05(18, 4);

        private final int guardSymbols;
        private final int ulSymbols;

    }

    private AdministrativeState administrativeState;
    @JsonProperty("nCI")
    private Long nCI;
    @JsonProperty("pZeroNomPuschGrant")
    private Integer pZeroNomPuschGrant;
    @JsonProperty("pZeroUePuschOffset256Qam")
    private Integer pZeroUePuschOffset256Qam;

    private Integer subCarrierSpacing;
    private TddSpecialSlotPattern tddSpecialSlotPattern;
    private TddUlDlPattern tddUlDlPattern;
    private Toggle tddBorderVersion;
    // Set for demo purposes, the default from network is false
    private Boolean ul256QamEnabled = Boolean.TRUE;
    @ElementCollection
    private List<Integer> bandList;
    @ElementCollection
    private List<Integer> bandListManual;
    @JsonProperty(value="nRSectorCarrierRef", access=JsonProperty.Access.WRITE_ONLY)
    @OneToMany(targetEntity = NRSectorCarrier.class)
    private List<NRSectorCarrier> nRSectorCarriers = new ArrayList<>();

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumns(value = {
        @JoinColumn(name = "ref_id", referencedColumnName = "nr_celldu_ref_id"),
        @JoinColumn(name = "me_fdn", referencedColumnName = "nr_celldu_me_fdn")
    })
    private ParametersChanges parametersChanges;
    @JsonProperty("advancedDlSuMimoEnabled")
    private Boolean advancedDlSuMimoEnabled = Boolean.FALSE;

    public NRCellDU(String fdn) {
        super(fdn);
    }

    @JsonIgnore
    public int getEffectiveGuardSymbols() {
        int effectiveGuardSymbol = tddSpecialSlotPattern.getGuardSymbols() + tddSpecialSlotPattern.getUlSymbols();
        if (tddSpecialSlotPattern == TddSpecialSlotPattern.TDD_SPECIAL_SLOT_PATTERN_01) {
            //one symbol taken by control channel in DL
            effectiveGuardSymbol -= 1;
        }
        if (advancedDlSuMimoEnabled && tddSpecialSlotPattern != TddSpecialSlotPattern.TDD_SPECIAL_SLOT_PATTERN_00) {
            //SRS signal for the beam-forming purposes
            effectiveGuardSymbol -= 2;
        }
        return effectiveGuardSymbol;
    }
}
