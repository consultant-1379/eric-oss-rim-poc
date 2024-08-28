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
package com.ericsson.oss.apps.config;

import lombok.Getter;
import lombok.Setter;

/**
 * see <a href="https://portal.3gpp.org/desktopmodules/Specifications/SpecificationDetails.aspx?specificationId=3202">3GPP TS 38.104 chapter 5.4.2.1.</a>
 */
@Getter
@Setter
public class ArfcnRange {
    private int minNRef; // Min of Range of NREF, used also as NREF-Offs
    private int maxNRef; // Max of Range of NREF
    private int deltaFGlobalKhz; // ΔFGlobal (kHz)
    private int fREFOffsMHz; // FREF-Offs (MHz)
}
