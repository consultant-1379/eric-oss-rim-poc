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
package com.ericsson.oss.apps.data.collection.features;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class FtRopNRCellDUPair {

    @CsvBindByName
    private String fdn1;
    @CsvBindByName
    private String fdn2;
    @CsvBindByName
    private long ropTime;

    @CsvBindByName
    private double ductStrength;
    @CsvBindByName
    private double distance;
    @CsvBindByName
    private double guardDistance;
    @CsvBindByName
    private double guardOverDistance;
    @CsvBindByName
    private double tddOverlap;
    @CsvBindByName
    private double azimuthAffinity = Double.NaN;
    @CsvBindByName
    private double aggressorScore = Double.NaN;

    @CsvBindByName
    private double frequencyOverlap = Double.NaN;


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        FtRopNRCellDUPair that = (FtRopNRCellDUPair) o;
        return fdn1 != null && Objects.equals(fdn1, that.fdn1)
                && fdn2 != null && Objects.equals(fdn2, that.fdn2)
                && Objects.equals(ropTime, that.ropTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fdn1, fdn2, ropTime);
    }

}
