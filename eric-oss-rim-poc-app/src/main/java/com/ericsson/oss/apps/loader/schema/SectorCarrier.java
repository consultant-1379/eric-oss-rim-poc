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
import com.opencsv.bean.CsvBindByName;

import lombok.Data;

@Data
public class SectorCarrier extends Mo {
    @CsvBindByName
    private ManagedObject.AdministrativeState administrativeState;
    @CsvBindByName
    private Integer bSChannelBwUL;
    @CsvBindByName
    private Integer bSChannelBwDL;
    @CsvBindByName
    private Integer arfcnUL;
    @CsvBindByName
    private Integer arfcnDL;
}
