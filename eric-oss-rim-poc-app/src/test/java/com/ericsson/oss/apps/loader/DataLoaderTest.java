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
package com.ericsson.oss.apps.loader;

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.model.GeoData;
import com.ericsson.oss.apps.model.mom.*;
import com.ericsson.oss.apps.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

@Slf4j
@SpringBootTest(properties = {"spring.datasource.exposed=true"})
public class DataLoaderTest {

    private static final String MANAGED_ELEMENT = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Atlanta,MeContext=M9AT2772B2,ManagedElement=M9AT2772B2";
    private static final ManagedObjectId EXTERNAL_CELL_ID = ManagedObjectId.of(MANAGED_ELEMENT,
            "GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=auto310_260_3_1597264,ExternalNRCellCU=auto6542393345");

    @Autowired
    private CmGNBDUFunctionRepo gnbduFunctionRepo;
    @Autowired
    private CmGNBCUCPFunctionRepo gnbcucpFunctionRepo;
    @Autowired
    private CmExternalGNBCUCPFunctionRepo externalGNBCUCPFunctionRepo;
    @Autowired
    private CmNrCellDuRepo cellDuRepo;
    @Autowired
    private CmNrCellCuRepo cellCuRepo;
    @Autowired
    private CmExternalNrCellCuRepo externalNrCellCuRepo;
    @Autowired
    private CmNrCellRelationRepo cellRelationRepo;
    @Autowired
    private GeoDataRepo geoDataRepo;

    @TestConfiguration
    public static class LoaderTestConfig {
        private final String BUCKET = "rim";
        private final String RESOURCE_PATH = "setup_files/cm/";
        private final String CUSTOMERID = "tmo001";

        @Bean
        @Primary
        public BdrClient mockDataLoader() {
            BdrClient mockedBdrClient = Mockito.mock(BdrClient.class);
            Stream.of("gnbdu_fn", "cell_relation", "external_gnbcucp_fn", "external_nrcell_cu", "geo_data", "gnbcucpfunction", "nrcell_cu", "nrcell_du", "nrsector_carrier")
                    .forEach(objectName -> {
                        String filename = getObjectPath(objectName);
                        Mockito.when(mockedBdrClient.getObjectInputStream(eq(BUCKET), eq(filename))).thenAnswer(I -> localFileLoader(filename));
                    });
            return mockedBdrClient;
        }

        private InputStream localFileLoader(String filename) throws IOException {
            return new ClassPathResource(filename).getInputStream();
        }

        private String getObjectPath(String fileName) {
            return String.format("%s%s-%s.csv.gz", RESOURCE_PATH, fileName, CUSTOMERID);
        }
    }

    @Test
    void testGNodeBDuLoading() {
        List<GNBDUFunction> nodes = gnbduFunctionRepo.findAll();
        assertEquals(1, nodes.size());

        GNBDUFunction node = nodes.get(0);
        assertEquals(ManagedObjectId.of(MANAGED_ELEMENT, "GNBDUFunction=1"), node.getObjectId());
        assertEquals(1605219, node.getGNBId());
        assertEquals(24, node.getGNBIdLength());
        assertEquals(0, node.getPLMNId().getMcc());
        assertEquals(0, node.getPLMNId().getMnc());
    }

    @Test
    void testGNodeBCuLoading() {
        List<GNBCUCPFunction> nodes = gnbcucpFunctionRepo.findAll();
        assertEquals(1, nodes.size());

        GNBCUCPFunction node = nodes.get(0);
        assertEquals(ManagedObjectId.of(MANAGED_ELEMENT, "GNBCUCPFunction=1"), node.getObjectId());
        assertEquals(1605219, node.getGNBId());
        assertEquals(24, node.getGNBIdLength());
        assertEquals(0, node.getPLMNId().getMcc());
        assertEquals(0, node.getPLMNId().getMnc());
    }

    @Test
    void testExternalGNodeBCuLoading() {
        List<ExternalGNBCUCPFunction> nodes = externalGNBCUCPFunctionRepo.findAll();
        assertEquals(1, nodes.size());

        ExternalGNBCUCPFunction node = nodes.get(0);
        assertEquals(ManagedObjectId.of(MANAGED_ELEMENT, "GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=auto310_260_3_1597264"),
                node.getObjectId());
        assertEquals(1611017, node.getGNBId());
        assertEquals(24, node.getGNBIdLength());
        assertEquals(0, node.getPLMNId().getMcc());
        assertEquals(0, node.getPLMNId().getMnc());
    }

    @Test
    void testNrCellDuLoading() {
        List<NRCellDU> cells = cellDuRepo.findAll();
        assertEquals(1, cells.size());

        NRCellDU cell = cells.get(0);
        assertEquals(ManagedObjectId.of(MANAGED_ELEMENT, "GNBDUFunction=1,NRCellDU=K9AT2772B11"), cell.getObjectId());
        assertEquals(ManagedObject.AdministrativeState.UNLOCKED, cell.getAdministrativeState());
        assertEquals(6574977025L, cell.getNCI());
        assertEquals(-100, cell.getPZeroNomPuschGrant());
        assertEquals(4, cell.getPZeroUePuschOffset256Qam());
        assertEquals(15, cell.getSubCarrierSpacing());
        assertEquals(ManagedObject.Toggle.DISABLED, cell.getTddBorderVersion());
        assertEquals(Collections.singletonList(71), cell.getBandList());
        assertEquals(Collections.emptyList(), cell.getBandListManual());
        assertEquals(NRCellDU.TddSpecialSlotPattern.TDD_SPECIAL_SLOT_PATTERN_00, cell.getTddSpecialSlotPattern());
        assertEquals(NRCellDU.TddUlDlPattern.TDD_ULDL_PATTERN_00, cell.getTddUlDlPattern());

        NRSectorCarrier carrier = cell.getNRSectorCarriers().get(0);
        assertEquals(ManagedObjectId.of(MANAGED_ELEMENT, "GNBDUFunction=1,NRSectorCarrier=6-01-01"), carrier.getObjectId());
        assertEquals(ManagedObject.AdministrativeState.UNLOCKED, carrier.getAdministrativeState());
        assertEquals(15, carrier.getBSChannelBwUL());
        assertEquals(15, carrier.getBSChannelBwDL());
        assertEquals(136100, carrier.getArfcnUL());
        assertEquals(126900, carrier.getArfcnDL());
        assertTrue(cell.getAdvancedDlSuMimoEnabled());
    }

    @Test
    void testNrCellCuLoading() {
        List<NRCellCU> cells = cellCuRepo.findAll();
        assertEquals(1, cells.size());

        NRCellCU cell = cells.get(0);
        assertEquals(ManagedObjectId.of(MANAGED_ELEMENT, "GNBCUCPFunction=1,NRCellCU=K9AT2772B11"), cell.getObjectId());
        assertEquals(6574977025L, cell.getNCI());
        assertEquals(1, cell.getCellLocalId());
    }

    @Test
    void testExternalNrCellCuLoading() {
        List<ExternalNRCellCU> cells = externalNrCellCuRepo.findAll();
        assertEquals(1, cells.size());

        ExternalNRCellCU cell = cells.get(0);
        assertEquals(ManagedObjectId.of(MANAGED_ELEMENT,
                "GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=auto310_260_3_1597264,ExternalNRCellCU=auto6542393345"), cell.getObjectId());
        assertEquals(303, cell.getCellLocalId());
    }

    @Test
    void testNrCellRelationLoading() {
        List<NRCellRelation> relations = cellRelationRepo.findAll();
        assertEquals(3, relations.size());

        NRCellRelation relation = relations.get(0);
        assertEquals(ManagedObjectId.of(MANAGED_ELEMENT, "GNBCUCPFunction=1,NRCellCU=K9AT2772B11,NRCellRelation=auto6542393345"),
                relation.getObjectId());
        assertEquals(0, relation.getCellIndividualOffsetNR());
        assertEquals(EXTERNAL_CELL_ID, relation.getNRCellRef());
        assertEquals(true, relation.isHoAllowed());

        NRCellRelation relation1 = relations.get(1);
        assertEquals(false, relation1.isHoAllowed());

        NRCellRelation relation2 = relations.get(2);
        assertEquals(true, relation2.isHoAllowed());

    }

    @Test
    void testGeoDataLoading() {
        List<GeoData> geoData = geoDataRepo.findAll();
        assertEquals(1, geoData.size());

        GeoData cellGeo = geoData.get(0);
        assertEquals("SubNetwork=OMCENM01,MeContext=X18844,ManagedElement=X18844,GNBDUFunction=1,NRCellDU=Y18844A", cellGeo.getFdn());
        assertEquals(0, cellGeo.getBearing());
        assertEquals(1.862217445, cellGeo.getCoordinate().getX());
        assertEquals(48.97027709, cellGeo.getCoordinate().getY());
        assertEquals(4, cellGeo.getE_dtilts());
        assertEquals(1, cellGeo.getM_dtilts());
    }
}
