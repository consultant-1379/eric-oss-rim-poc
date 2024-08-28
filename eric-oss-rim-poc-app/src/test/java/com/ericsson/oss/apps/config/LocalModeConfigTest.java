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

import com.ericsson.oss.apps.client.CtsClient;
import com.ericsson.oss.apps.client.NcmpClient;
import com.ericsson.oss.apps.data.collection.features.handlers.ConfigChangeImplementor;
import com.ericsson.oss.apps.model.GeoData;
import com.ericsson.oss.apps.model.mitigation.CellRelationChange;
import com.ericsson.oss.apps.model.mom.ManagedElement;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.repositories.GeoDataRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.local=true")
public class LocalModeConfigTest {

    private static final String FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002";

    @Autowired
    private NcmpClient ncmpClient;
    @Autowired
    private CtsClient ctsClient;
    @Autowired
    private GeoDataRepo geoDataRepo;
    @Autowired
    private ConfigChangeImplementor changeImplementor;

    @Test
    void getCmHandleFromDnLocal() {
        ManagedObjectId objectId  = ManagedObjectId.of(FDN);
        assertTrue(ncmpClient.getCmResource(objectId, ManagedElement.class).isEmpty());
    }

    @Test
    void getNrCellGeoDataAlreadyInRepo() {
        geoDataRepo.save(GeoData.builder().fdn(FDN).build());
        Optional<GeoData> geoData = ctsClient.getNrCellGeoData(FDN);
        assertTrue(geoData.isPresent());
        assertEquals(FDN, geoData.get().getFdn());
    }

    @Test
    void implementConfigChanges() {
        assertTrue(changeImplementor.implementChange(new CellRelationChange()));
        assertTrue(changeImplementor.implementChange(new NRCellDU()));
    }
}
