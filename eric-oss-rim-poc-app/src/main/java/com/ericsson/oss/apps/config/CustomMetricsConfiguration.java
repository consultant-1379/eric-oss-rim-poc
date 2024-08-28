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
package com.ericsson.oss.apps.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomMetricsConfiguration {

    public static final String APP = "app";
    public static final String ERIC_OSS_RIM_POC_APP = "eric.oss.rim.poc.app";

    private final MeterRegistry meterRegistry;

    @Bean
    public Counter numPmRopRecordsReceived() {
        return meterRegistry.counter("num.pm.rop.records.received", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numKpiRopRecordsProcessed() {
        return meterRegistry.counter("num.kpi.rop.records.processed", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellsAboveMinimumDeltaIPN() {
        return meterRegistry.counter("num.cells.above.min.deltaipn", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numConnectedComponents() {
        return meterRegistry.counter("num.connected.components.all", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numConnectedComponentsAboveMinSize() {
        return meterRegistry.counter("num.connected.components.above.min.size", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellsWithinDuctingConditions() {
        return meterRegistry.counter("num.cells.within.ducting.conditions", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellPairsValid() {
        return meterRegistry.counter("num.cell.pairs.valid", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellPairsOverMinDucting() {
        return meterRegistry.counter("num.cell.pairs.over.min.ducting", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellPairsTDDOverlap() {
        return meterRegistry.counter("num.cell.pairs.tdd.overlap", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellPairsFrequencyOverlap() {
        return meterRegistry.counter("num.cell.pairs.frequency.overlap", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numVictimCellsSelectedForMitigation() {
        return meterRegistry.counter("num.cell.selected.for.mitigation", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numNeighborCellsIntraFrequency() {
        return meterRegistry.counter("num.neighbor.cells.intra.frequency", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numNeighborCellsInterFrequencyBwCompliant() {
        return meterRegistry.counter("num.neighbor.cells.inter.frequency.bw.compliant", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numNeighborCellsDroppedInterFrequencyNonBwCompliant() {
        return meterRegistry.counter("num.neighbor.cells.dropped.inter.frequency.non.bw.compliant", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numNeighborCellsDroppedNoHandovers() {
        return meterRegistry.counter("num.neighbor.cells.dropped.no.handovers", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numNeighborCellsSelectedForKp0() {
        return meterRegistry.counter("num.neighbor.cells.selected.for.kp0", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numNeighborCellsSelectedForKcio() {
        return meterRegistry.counter("num.neighbor.cells.selected.for.kcio", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellP0MitigationVictimSucc() {
        return meterRegistry.counter("num.cell.p0.mitigation.victim.succ", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellP0MitigationVictimFailed() {
        return meterRegistry.counter("num.cell.p0.mitigation.victim.failed", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellP0MitigationNeighborSucc() {
        return meterRegistry.counter("num.cell.p0.mitigation.neighbor.succ", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellP0MitigationNeighborFailed() {
        return meterRegistry.counter("num.cell.p0.mitigation.neighbor.failed", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellP0RollbackSucc() {
        return meterRegistry.counter("num.cell.p0.rollback.succ", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellP0RollbackFailed() {
        return meterRegistry.counter("num.cell.p0.rollback.failed", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellCioMitigationNeighborSucc() {
        return meterRegistry.counter("num.cell.cio.mitigation.neighbor.succ", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellCioMitigationNeighborFailed() {
        return meterRegistry.counter("num.cell.cio.mitigation.neighbor.failed", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellCioRollbackNeighborSucc() {
        return meterRegistry.counter("num.cell.cio.rollback.neighbor.succ", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellCioRollbackNeighborFailed() {
        return meterRegistry.counter("num.cell.cio.rollback.neighbor.failed", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellCioMitigationNeighborBlocked() {
        return meterRegistry.counter("num.cell.cio.rollback.neighbor.blocked", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter numCellCioMitigationCellRegistered() {
        return meterRegistry.counter("num.cell.cio.rollback.cell.registered", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter kafkaNumberBatchesReceived() {
        return meterRegistry.counter("kafka.number.batches.received", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter kafkaNumberRecordsReceived() {
        return meterRegistry.counter("kafka.number.records.received", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter kafkaNumberRecordsInvalidMoType() {
        return meterRegistry.counter("kafka.number.records.invalid.mo.type", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter kafkaNumberRecordsProcessed() {
        return meterRegistry.counter("kafka.number.records.processed", APP, ERIC_OSS_RIM_POC_APP);
    }

    @Bean
    public Counter kafkaNumberRecordsDropped() {
        return meterRegistry.counter("kafka.number.records.dropped", APP, ERIC_OSS_RIM_POC_APP);
    }

}
