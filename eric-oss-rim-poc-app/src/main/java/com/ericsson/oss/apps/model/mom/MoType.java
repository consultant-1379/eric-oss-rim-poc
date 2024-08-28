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
package com.ericsson.oss.apps.model.mom;

import com.ericsson.oss.apps.model.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum MoType {

    MANAGED_ELEMENT(ManagedElement.class),
    GNBDU_FUNCTION(GNBDUFunction.class),
    GNBCUCP_FUNCTION(GNBCUCPFunction.class),
    NR_CELL_DU(NRCellDU.class, "ericsson-enm:GNBDUFunction/ericsson-enm:NRCellDU"),
    NR_CELL_CU(NRCellCU.class, "ericsson-enm:GNBCUCPFunction/ericsson-enm:NRCellCU"),
    NR_CELL_RELATION(NRCellRelation.class, "ericsson-enm:GNBCUCPFunction/ericsson-enm:NRCellCU/ericsson-enm:NRCellRelation"),
    NR_FREQUENCY(NRFrequency.class, "ericsson-enm:GNBCUCPFunction/ericsson-enm:NRNetwork/ericsson-enm:NRFrequency"),
    NR_SECTOR_CARRIER(NRSectorCarrier.class, "ericsson-enm:GNBDUFunction/ericsson-enm:NRSectorCarrier"),
    EXTERNAL_GNBCUCP_FUNCTION(ExternalGNBCUCPFunction.class, "ericsson-enm:GNBCUCPFunction/ericsson-enm:NRNetwork/ericsson-enm:ExternalGNBCUCPFunction"),
    EXTERNAL_NR_CELL_CU(ExternalNRCellCU.class, "ericsson-enm:GNBCUCPFunction/ericsson-enm:NRNetwork/ericsson-enm:ExternalGNBCUCPFunction/ericsson-enm:ExternalNRCellCU");

    public static final String DEFAULT_NAMESPACE = "ericsson-enm";
    private static final Map<String, MoType> MO_TYPE_MAP = Arrays.stream(MoType.values())
            .collect(Collectors.toUnmodifiableMap(MoType::toString, Function.identity()));

    public static MoType getMoType(String resourcePath) {
        int start = resourcePath.lastIndexOf(Constants.COMMA) + 1;
        String dn = resourcePath.substring(start, Math.max(resourcePath.lastIndexOf(Constants.EQUAL), start));
        return MO_TYPE_MAP.getOrDefault(dn, MANAGED_ELEMENT);
    }

    private final Class<? extends ManagedObject> type;
    private final String fullName;

    MoType(Class<? extends ManagedObject> tClass) {
        this(tClass, DEFAULT_NAMESPACE + Constants.COLON + tClass.getSimpleName());
    }

    @Override
    public String toString() {
        return type.getSimpleName();
    }
}