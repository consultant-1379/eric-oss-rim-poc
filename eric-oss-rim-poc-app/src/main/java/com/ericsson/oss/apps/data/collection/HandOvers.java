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

package com.ericsson.oss.apps.data.collection;

import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class HandOvers.
 * Utility class to hold the hand over calculations during the calculation of the coupling coefficients KPO and KCIO
 */
@Slf4j
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class HandOvers {
    private final String fdnNrCellRelation;

    private final Integer numberHandovers;

    private final Integer numberTotalHandovers;

    private Double cumulativeTotalHandoversPercent;

    /**
     * Equals.
     *
     * @param o
     *     the o
     *
     * @return true, if successful
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HandOvers that = (HandOvers) o;
        return fdnNrCellRelation.equals(that.fdnNrCellRelation) && numberHandovers.equals(that.numberHandovers)
                && numberTotalHandovers.equals(that.numberTotalHandovers);
    }

    /**
     * Hash code.
     *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(fdnNrCellRelation);
    }

    /**
     * Calculates the % HO coefficient
     *
     * @return
     */
    public Double getHoCoefficient() {
        if (numberTotalHandovers == null || numberTotalHandovers == 0 || numberHandovers == null) {
            log.error("Invalid value for either numberHandovers = {} or numberTotalHandovers = {}, will return 0", numberHandovers,
                    numberTotalHandovers);
            return 0.0;
        }
        return 100 * ((double) numberHandovers / (double) numberTotalHandovers);
    }
}
