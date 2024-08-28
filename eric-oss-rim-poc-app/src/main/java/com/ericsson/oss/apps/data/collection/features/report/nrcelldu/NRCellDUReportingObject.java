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
package com.ericsson.oss.apps.data.collection.features.report.nrcelldu;

import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.opencsv.bean.CsvBindAndSplitByName;
import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class NRCellDUReportingObject {
    @CsvBindByName
    private String fdn;
    @CsvBindByName
    private ManagedObject.AdministrativeState administrativeState;
    @CsvBindByName
    private Long nCI;
    @CsvBindByName
    private Integer pZeroNomPuschGrant;
    @CsvBindByName
    private Integer pZeroUePuschOffset256Qam;
    @CsvBindByName
    private Integer subCarrierSpacing;
    @CsvBindByName
    private NRCellDU.TddSpecialSlotPattern tddSpecialSlotPattern;
    @CsvBindByName
    private NRCellDU.TddUlDlPattern tddUlDlPattern;
    @CsvBindByName
    private ManagedObject.Toggle tddBorderVersion;
    @CsvBindAndSplitByName(elementType = Integer.class, writeDelimiter = ":")
    private List<Integer> bandList;
    @CsvBindAndSplitByName(elementType = Integer.class, writeDelimiter = ":")
    private List<Integer> bandListManual;
    @CsvBindByName
    private Double lat;
    @CsvBindByName
    private Double lon;
    @CsvBindByName
    private Integer bearing;
    @CsvBindByName
    private Integer arfcnDL;
    @CsvBindByName
    private Integer bSChannelBwDL;
    @CsvBindByName
    private Long ropTime;
}
