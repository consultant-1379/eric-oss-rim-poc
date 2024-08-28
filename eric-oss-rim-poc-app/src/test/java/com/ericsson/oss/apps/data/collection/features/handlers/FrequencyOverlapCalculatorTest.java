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
package com.ericsson.oss.apps.data.collection.features.handlers;

import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN1;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.FDN2;
import static com.ericsson.oss.apps.data.collection.features.handlers.HandlerTestUtils.buildFtRopNRCellDUPair;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import com.ericsson.oss.apps.config.ArfcnRange;
import com.ericsson.oss.apps.config.ClusteringConfig;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRSectorCarrier;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


import java.util.List;

class FrequencyOverlapCalculatorTest {

    FeatureContext context;
    ClusteringConfig clusteringConfig;

    @BeforeEach
    void setUp() {
        context = new FeatureContext(0L);
        context.setFtRopNRCellDUPairs(List.of(buildFtRopNRCellDUPair(FDN1, FDN2)));
        clusteringConfig = new ClusteringConfig();
        clusteringConfig.setArfcnRanges(List.of(
                getArfcnRange(0, 599999, 5, 0),
                getArfcnRange(600000, 2016666, 15, 3000),
                getArfcnRange(2016667, 3279167, 60, 24250)));
    }

    private ArfcnRange getArfcnRange(int minNRef,
                                     int maxNRef,
                                     int deltaFGlobalKhz,
                                     int fREFOffsMHz
    ) {

        ArfcnRange arfcnRange = new ArfcnRange();
        arfcnRange.setMinNRef(minNRef);
        arfcnRange.setMaxNRef(maxNRef);
        arfcnRange.setDeltaFGlobalKhz(deltaFGlobalKhz);
        arfcnRange.setFREFOffsMHz(fREFOffsMHz);
        return arfcnRange;
    }

    /**
     * given
     * context has a cell pair
     * context has nrcelldu and nrsectorcarrier information for both cells
     * clustering configuration is populated
     * source cell band covers X% of target cell band
     * when
     * handle method is invoked
     * then
     * the cell pair has cell overlap X% populated
     */
    @ParameterizedTest
    @CsvSource(value = {
            "80, 509202, 60, 523146, 0.00467, 1",
            "60, 523146, 80, 509202, 0.00350, 1",
            "80, 509202, 60, 509202, 1, 1",
            "60, 509202, 80, 509202, 0.75, 1",
            "60, 509202, 60, 523146, 0, 0",
            "null, 509202, 60, 523146, NaN, 0",
            "60, 509202, 0, 509202, 0, 0",
            "80, 509202, 20, 126900, 0, 0",
            "0, 509202, 0, 126900, 0, 0",
            "80, 0, 80, 0, 1, 1",
    }, nullValues = {"null"})
    void testHandle(Integer sourceCellBW,
                    Integer sourceArfcn,
                    Integer targetCellBW,
                    Integer targetArfcn,
                    double frequencyOverlap,
                    int counterIncrement) {
        context.getFdnToNRCellDUMap().put(FDN1, getNRCellDU(FDN1, sourceCellBW, sourceArfcn));
        context.getFdnToNRCellDUMap().put(FDN2, getNRCellDU(FDN2, targetCellBW, targetArfcn));
        Counter counter = mock(Counter.class);
        FrequencyOverlapCalculator frequencyOverlapCalculator = new FrequencyOverlapCalculator(clusteringConfig, counter);
        frequencyOverlapCalculator.handle(context);
        assertEquals(frequencyOverlap, context.getFtRopNRCellDUPairs().get(0).getFrequencyOverlap(), 0.00001);
        verify(counter, times(counterIncrement)).increment();
    }

    private NRCellDU getNRCellDU(String fdn, Integer sourceCellBW, Integer sourceArfcn) {
        NRCellDU cellDU = new NRCellDU(fdn);
        NRSectorCarrier nRSectorCarrier1 = new NRSectorCarrier(cellDU.getObjectId().toFdn());
        nRSectorCarrier1.setBSChannelBwDL(sourceCellBW);
        nRSectorCarrier1.setArfcnDL(sourceArfcn);
        cellDU.getNRSectorCarriers().add(nRSectorCarrier1);
        return cellDU;
    }
}