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
package com.ericsson.oss.apps.loader.schema;

import com.ericsson.oss.apps.model.mom.ManagedObject;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.opencsv.bean.CsvBindAndSplitByName;
import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.util.LinkedList;

@Data
public class CellDu extends Mo {
    @CsvBindByName
    private ManagedObject.AdministrativeState administrativeState;
    @CsvBindByName
    private Integer cellLocalId;
    @CsvBindByName
    private Long nCI;
    @CsvBindByName
    private Integer pZeroNomPuschGrant;
    @CsvBindByName
    private Integer pZeroUePuschOffset256Qam;
    @CsvBindByName
    private Integer subCarrierSpacing;
    @CsvBindByName
    private ManagedObject.Toggle tddBorderVersion;
    @CsvBindAndSplitByName(elementType = Integer.class, splitOn = "#")
    private LinkedList<Integer> bandList;
    @CsvBindAndSplitByName(elementType = Integer.class, splitOn = "#")
    private LinkedList<Integer> bandListManual;
    @CsvBindByName
    private NRCellDU.TddSpecialSlotPattern tddSpecialSlotPattern;
    @CsvBindByName
    private NRCellDU.TddUlDlPattern tddUlDlPattern;
    @CsvBindByName
    private String nRSectorCarrierRef;
    @CsvBindByName
    private Boolean advancedDlSuMimoEnabled;
}