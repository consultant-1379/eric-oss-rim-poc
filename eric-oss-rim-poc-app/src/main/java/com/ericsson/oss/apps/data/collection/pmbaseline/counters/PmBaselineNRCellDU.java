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

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PmBaselineNRCellDU implements Serializable {
    private static final long serialVersionUID = -7861635514114144789L;
    @Id
    @CsvBindByName(required = true)
    private String fdn;

    @CsvBindByName(column = "n_rops")
    private Integer nRops;
    @CsvBindByName(column = "q_25")
    private Double uplInkThroughputQuartile25;
    @CsvBindByName(column = "median")
    private Double uplInkThroughputQuartile50;
    @CsvBindByName(column = "q_75")
    private Double uplInkThroughputQuartile75;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PmBaselineNRCellDU that = (PmBaselineNRCellDU) o;
        return fdn.equals(that.fdn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fdn);
    }
}
