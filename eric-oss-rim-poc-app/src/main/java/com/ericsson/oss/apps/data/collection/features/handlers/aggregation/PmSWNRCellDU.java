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

package com.ericsson.oss.apps.data.collection.features.handlers.aggregation;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;

/**
 * This class is a pojo to store the results of sliding window calculation and hosts the sliding window native query.
 */
// result mapper for the native query (tells JAP how to invoke the constructor of this entity)
// NOTE: Same number and type of columns as in the constructor below.
@SqlResultSetMappings(value = {
        @SqlResultSetMapping(name = "SWNRCellDUResult",
                classes = {
                        @ConstructorResult(targetClass = PmSWNRCellDU.class,
                                columns = {
                                        @ColumnResult(name = "FDN", type = String.class),
                                        @ColumnResult(name = "N_ROPS_IN_LAST_SEEN_WINDOW", type = Byte.class),
                                        @ColumnResult(name = "AVG_DELTA_IPN", type = Double.class),
                                        @ColumnResult(name = "AVG_SYMBOL_DELTA_IPN", type = Double.class),
                                        @ColumnResult(name = "AVG_UE_TP_SW8", type = Double.class),
                                        @ColumnResult(name = "AVG_UE_TP_SW2", type = Double.class),
                                        @ColumnResult(name = "PERC_POS_SYMBOL_DELTA_IPN", type = Double.class),
                                        @ColumnResult(name = "ROP_COUNT", type = Integer.class)
                                })
                })
}
)
// native query, calculates both sw8 and sw2 (we get away with a single group by)
// JPA does not support named parameters in native queries
// PM_MAC_TIME_UL_RES_UE can be zero so there is a bit of logic to avoid division by zero errors
// Note : 'Average of the PmRopNrCellDU::avgUlUeTp' gives a different answer to the 'Calculation over the full 2/8 ROPS' (implemented in below query).
//        Hence, for this reason the PmRopNRCellDU avgUlUeTp cannot be used.
// N_ROPS_IN_LAST_SEEN_WINDOW counts the number of ROPs found in the time window of specified size since the current ROP.
@NamedNativeQuery(name = "SWNRCellDU",
        query = """
                select fdn,
                SUM(CASE WHEN rop_time >= ?5 THEN 1 ELSE 0 END) AS N_ROPS_IN_LAST_SEEN_WINDOW,
                AVG(CASE WHEN (AVG_DELTA_IPN = 'NaN' OR TOTAL_BIN_SUM_MAX_DELTA_IPN < ?7 OR TOTAL_BIN_SUM_MAX_DELTA_IPN IS NULL)THEN NULL ELSE AVG_DELTA_IPN END) as AVG_DELTA_IPN,
                AVG(CASE WHEN (AVG_SYMBOL_DELTA_IPN = 'NaN' OR TOTAL_BIN_SUM_SYMBOL_DELTA_IPN < ?6 OR TOTAL_BIN_SUM_SYMBOL_DELTA_IPN IS NULL) THEN NULL ELSE AVG_SYMBOL_DELTA_IPN END) as AVG_SYMBOL_DELTA_IPN,
                64*SUM(pm_Mac_Vol_Ul_Res_Ue)/SUM(CASE WHEN pm_Mac_Time_Ul_REs_Ue > 0 THEN pm_Mac_Time_Ul_REs_Ue ELSE NULL END) as avg_ue_tp_sw8,
                64*SUM(CASE WHEN rop_time >= ?1 THEN pm_Mac_Vol_Ul_Res_Ue ELSE 0 END)/SUM(CASE WHEN (rop_time >= ?2 AND pm_Mac_Time_Ul_REs_Ue > 0 ) THEN pm_Mac_Time_Ul_REs_Ue ELSE NULL END) as avg_ue_tp_sw2,
                100*SUM(POSITIVE_BIN_SUM_SYMBOL_DELTA_IPN)/SUM(CASE WHEN ( TOTAL_BIN_SUM_SYMBOL_DELTA_IPN >= ?8 ) THEN TOTAL_BIN_SUM_SYMBOL_DELTA_IPN ELSE NULL END) as PERC_POS_SYMBOL_DELTA_IPN,
                COUNT(DISTINCT rop_time) AS ROP_COUNT
                from pm_ropnrcelldu where rop_time between ?3 and ?4 group by fdn
                """,
        resultSetMapping = "SWNRCellDUResult")
@Entity
@Getter
@NoArgsConstructor
public class PmSWNRCellDU implements Serializable {

    @Serial
    private static final long serialVersionUID = -8014789333222345686L;
    @Id
    private String fdn;
    private byte nRopsInLastSeenWindow = 0;
    private double avgSw8AvgDeltaIpN = Double.NaN;
    private double avgSw8AvgSymbolDeltaIpN = Double.NaN;
    private double avgSw2UlUeThroughput = Double.NaN;
    private double avgSw8UlUeThroughput = Double.NaN;
    private double avgSw8PercPositiveSymbolDeltaIpNSamples = Double.NaN;
    private int ropCount = 0;

    // positional; order of parameters in constructor, must match order of SQL queries above, i.e. fdn, AVG_DELTA_IPN, avg_ue_tp_sw8, avg_ue_tp_sw2
    public PmSWNRCellDU(String fdn,
                        byte nRopsInLastSeenWindow,
                        Double avgSw8AvgDeltaIpN,
                        Double avgSw8AvgSymbolDeltaIpN,
                        Double avgSw8UlUeThroughput,
                        Double avgSw2UlUeThroughput,
                        Double avgSw8PercPositiveSymbolDeltaIpNSamples,
                        int ropCount) {
        this.fdn = fdn;
        this.nRopsInLastSeenWindow = nRopsInLastSeenWindow;
        this.avgSw8AvgDeltaIpN = nullToNan(avgSw8AvgDeltaIpN);
        this.avgSw8AvgSymbolDeltaIpN = nullToNan(avgSw8AvgSymbolDeltaIpN);
        this.avgSw2UlUeThroughput = nullToNan(avgSw2UlUeThroughput);
        this.avgSw8UlUeThroughput = nullToNan(avgSw8UlUeThroughput);
        this.avgSw8PercPositiveSymbolDeltaIpNSamples = nullToNan(avgSw8PercPositiveSymbolDeltaIpNSamples);
        this.ropCount = ropCount;
    }

    private double nullToNan(Double doubleNumber) {
        return (doubleNumber == null) ? Double.NaN : doubleNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PmSWNRCellDU that = (PmSWNRCellDU) o;

        return fdn.equals(that.fdn);
    }

    @Override
    public int hashCode() {
        return fdn.hashCode();
    }
}

