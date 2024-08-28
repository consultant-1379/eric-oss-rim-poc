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

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.data.collection.AppDataConfig;
import com.ericsson.oss.apps.data.collection.allowlist.AllowedNrCellDu;
import com.ericsson.oss.apps.data.collection.allowlist.AllowedMoLoader;
import com.ericsson.oss.apps.data.collection.pmbaseline.PmBaselineLoader;
import com.ericsson.oss.apps.data.collection.pmbaseline.PmHoBaselineLoader;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineHoCoefficient;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineNRCellDU;
import com.ericsson.oss.apps.data.collection.pmrop.PmRopLoader;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.kafka.KafkaProducerRim;
import com.ericsson.oss.apps.repositories.AllowedNrCellDuRepo;
import com.ericsson.oss.apps.repositories.PmBaselineHoCoefficientRepo;
import com.ericsson.oss.apps.repositories.PmBaselineNrCellDuRepo;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class LoaderConfiguration {
    private final BdrClient bdrClient;
    private final BdrConfiguration bdrConfiguration;
    private final FileTracker fileTracker;
    private final Counter numPmRopRecordsReceived;
    private final AppDataConfig appDataConfig;

    @Bean
    @ConditionalOnProperty(prefix = "spring.kafka.mode", name = "enabled", havingValue = "false")
    public PmRopLoader<PmRopNRCellDU> nrCellDuDBRopLoader(final PmRopNrCellDuRepo pmRopNrCellDuRepo) {
        return new PmRopLoader<>(PmRopNRCellDU.class, pmRopNrCellDuRepo, bdrClient, bdrConfiguration, numPmRopRecordsReceived, fileTracker, appDataConfig);
    }

    @Bean
    public PmBaselineLoader<PmBaselineNRCellDU> nrCellDuBaselineLoader(final PmBaselineNrCellDuRepo pmBaselineNrCellDuRepo) {
        return new PmBaselineLoader<>(PmBaselineNRCellDU.class, pmBaselineNrCellDuRepo, bdrClient, bdrConfiguration, fileTracker);
    }

    @Bean
    public PmBaselineLoader<PmBaselineHoCoefficient> hoCoefficientBaselineLoader(final PmBaselineHoCoefficientRepo pmBaselineHoCoefficientRepo) {
        return new PmHoBaselineLoader(PmBaselineHoCoefficient.class, pmBaselineHoCoefficientRepo, bdrClient, bdrConfiguration, fileTracker);
    }

    @Bean
    public AllowedMoLoader<AllowedNrCellDu> allowedNrCellDuLoader(final AllowedNrCellDuRepo allowedNrCellDuRepo) {
        return new AllowedMoLoader<>(AllowedNrCellDu.class, allowedNrCellDuRepo, bdrClient, bdrConfiguration, fileTracker);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.kafka.mode", name = "enabled", havingValue = "true")
    public PmRopLoader<PmRopNRCellDU> nrCellDuRopKafkaLoader(final KafkaProducerRim kafkaProducerRim) {
        return new PmRopLoader<>(PmRopNRCellDU.class, kafkaProducerRim, bdrClient, bdrConfiguration, numPmRopRecordsReceived, fileTracker, appDataConfig);
    }

}
