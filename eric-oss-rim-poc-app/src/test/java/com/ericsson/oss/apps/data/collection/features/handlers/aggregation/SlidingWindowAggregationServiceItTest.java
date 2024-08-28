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
package com.ericsson.oss.apps.data.collection.features.handlers.aggregation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(properties = {"app.data.pm.rop.longSlidingWindowRops=8", "app.data.pm.rop.shortSlidingWindowRops=2", "app.data.pm.rop.minValidSymbolDeltaIpnSteps=300", "app.data.pm.rop.minValidMaxDeltaIpnSteps=300"})
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:pm/rop/pmropnrcelldu_sw.sql")
})
class SlidingWindowAggregationServiceItTest {

    @Autowired
    SlidingWindowAggregationService slidingWindowAggregationService;

    @Test
    void calculateSlidingWindowCounters() {
        Map<String, PmSWNRCellDU> slidingWindowCountersMap = slidingWindowAggregationService.calculateSlidingWindowCounters(7200000).stream()
                .collect(Collectors.toMap(PmSWNRCellDU::getFdn, Function.identity()));
        assertPmSWNRCellDUFields(slidingWindowCountersMap.get("fdn1"), "fdn1", 3.4, 128, 65.88, 3.4, 44.444, 8);
        assertPmSWNRCellDUFields(slidingWindowCountersMap.get("fdn2"), "fdn2", Double.NaN, Double.NaN, 67.87, Double.NaN, Double.NaN, 8);
        assertPmSWNRCellDUFields(slidingWindowCountersMap.get("fdn3"), "fdn3", Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 8);
        /* For fdn 4: sw8 : Average of avgUlUeTp: getting the average ul ue tp  = 64* (pm_Mac_Vol_Ul_Res_Ue/pm_Mac_Time_Ul_REs_Ue)
                            for each ROP, and then getting the average of the averages = 120
                             'Calculation over the full 2/8 ROPS'  : getting the average ul ue tp for all 2/8 ROPS
                                                                     = 64* ((vol 1 + vol2 ....)/ (time1 + time2....) gives 126.22.
                      sw2 : Average of avgUlUeTp = 96, Average of sums = 106.67

                      For this reason on cannot simplify the 'SWNRCellDU' query in PmSWNRCellDU to use PmRopNRCellDU avgUlUeTp.
                      */
        assertPmSWNRCellDUFields(slidingWindowCountersMap.get("fdn4"), "fdn4", 4.0, 106.67, 126.22, 4, Double.NaN, 8);
        assertEquals(1, slidingWindowCountersMap.get("fdn5").getNRopsInLastSeenWindow());
        assertPmSWNRCellDUFields(slidingWindowCountersMap.get("fdn6"), "fdn6", 0, Double.NaN,Double.NaN, Double.NaN, Double.NaN, 4);

    }


    private void assertPmSWNRCellDUFields(PmSWNRCellDU pmSWNRCellDU,
                                          String fdn,
                                          double avgSw8AvgDeltaIpN,
                                          double avgSw2UlUeThroughput,
                                          double avgSw8UlUeThroughput,
                                          double avgSw8AvgSymbolDeltaIpN,
                                          double avgSw8PercPositiveSymbolDeltaIpNSamples,
                                          int nRops) {
        log.info(" pmSWNRCellDU: fdn = {}, avgSw8AvgDeltaIpN = {}, avgSw2UlUeThroughput = {},  avgSw8UlUeThroughput = {}",
                pmSWNRCellDU.getFdn(), pmSWNRCellDU.getAvgSw8AvgDeltaIpN(), pmSWNRCellDU.getAvgSw2UlUeThroughput(),
                pmSWNRCellDU.getAvgSw8UlUeThroughput());

        assertEquals(fdn, pmSWNRCellDU.getFdn());
        assertEquals(avgSw8AvgDeltaIpN, pmSWNRCellDU.getAvgSw8AvgDeltaIpN());
        assertEquals(avgSw2UlUeThroughput, pmSWNRCellDU.getAvgSw2UlUeThroughput(), 0.01);
        assertEquals(avgSw8UlUeThroughput, pmSWNRCellDU.getAvgSw8UlUeThroughput(), 0.01);
        assertEquals(avgSw8AvgSymbolDeltaIpN, pmSWNRCellDU.getAvgSw8AvgSymbolDeltaIpN(), 0.01);
        assertEquals(avgSw8PercPositiveSymbolDeltaIpNSamples, pmSWNRCellDU.getAvgSw8PercPositiveSymbolDeltaIpNSamples(), 0.001);
        assertEquals(2, pmSWNRCellDU.getNRopsInLastSeenWindow());
        assertEquals(nRops, pmSWNRCellDU.getRopCount());
    }

}