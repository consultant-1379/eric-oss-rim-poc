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

package com.ericsson.oss.apps.data.collection.pmbaseline.counters;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.opencsv.bean.CsvBindByName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The Class PmBaselineHoCoefficient.
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PmBaselineHoCoefficient implements Serializable {

    private static final long serialVersionUID = 301614276841597801L;

    @Id
    @CsvBindByName(column = "fdn", required = true)
    private String fdnNrCellRelation;

    @CsvBindByName(column = "numberHandovers")
    private Integer numberHandovers;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PmBaselineHoCoefficient that = (PmBaselineHoCoefficient) o;
        return fdnNrCellRelation.equals(that.fdnNrCellRelation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fdnNrCellRelation);
    }

}
