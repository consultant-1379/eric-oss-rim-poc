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
package com.ericsson.oss.apps.data.collection.features.handlers.mobility;

import com.ericsson.oss.apps.classification.AllowedCellService;
import com.ericsson.oss.apps.classification.CellRelationService;
import com.ericsson.oss.apps.config.CellSelectionConfig;
import com.ericsson.oss.apps.config.MitigationConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.repositories.CellRelationChangeRepo;
import com.ericsson.oss.apps.repositories.NeighborDictionaryRepo;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MobilityMitigationPolicyBuilder {

    private final CellSelectionConfig cellSelectionConfig;
    private final MitigationConfig mitigationConfig;

    private final CellRelationService cellRelationService;
    private final MobilityMitigationAction mobilityMitigationAction;

    private final CellRelationChangeRepo changeRepo;
    private final NeighborDictionaryRepo neighborRepo;

    private final AllowedCellService allowedCellService;

    private final Counter numCellCioMitigationNeighborBlocked;
    private final Counter numCellCioMitigationCellRegistered;

    MobilityMitigationPolicy buildPolicy(FeatureContext context) {
        return new MobilityMitigationPolicy(
                context,
                cellSelectionConfig,
                mitigationConfig,
                cellRelationService,
                mobilityMitigationAction,
                changeRepo,
                neighborRepo,
                allowedCellService,
                numCellCioMitigationNeighborBlocked,
                numCellCioMitigationCellRegistered
        );
    }
}
