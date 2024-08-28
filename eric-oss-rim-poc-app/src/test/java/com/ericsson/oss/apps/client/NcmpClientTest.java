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
package com.ericsson.oss.apps.client;

import static com.ericsson.oss.apps.client.NcmpClient.OPTION_FIELDS_VALUE;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.cloud.contract.spec.internal.MediaTypes.APPLICATION_JSON;
import static org.springframework.cloud.contract.spec.internal.MediaTypes.TEXT_PLAIN;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import com.ericsson.oss.apps.model.Constants;
import com.ericsson.oss.apps.model.mom.ManagedObjectId;
import com.ericsson.oss.apps.model.mom.NRCellCU;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.model.mom.NRCellRelation;
import com.ericsson.oss.apps.model.mom.NRFrequency;
import com.ericsson.oss.apps.model.mom.NRSectorCarrier;
import com.ericsson.oss.apps.repositories.CmGNBCUCPFunctionRepo;
import com.ericsson.oss.apps.repositories.CmNrCellCuRepo;
import com.ericsson.oss.apps.repositories.CmNrCellDuRepo;
import com.ericsson.oss.apps.repositories.CmNrCellRelationRepo;
import com.ericsson.oss.apps.repositories.CmNrFrequencyRepo;
import com.ericsson.oss.apps.repositories.CmNrSectorCarrierRepo;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "client.ncmp.base-path=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(port = 0)
public class NcmpClientTest {

    private static final String CM_HANDLE = "92F1CB35798FD7D13BCC6FF825D89CD6";
    private static final String REFERENCE = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002";
    private static final String PATH = String.format("/ncmp/v1/ch/%s/data/ds/ncmp-datastore:passthrough-running", CM_HANDLE);
    private static final ManagedObjectId CELL_RESOURCE = new ManagedObjectId(REFERENCE, "GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00002-1");
    private static final ManagedObjectId SECTOR_CARRIER_RESOURCE = new ManagedObjectId(REFERENCE, "GNBDUFunction=1,NRSectorCarrier=1");
    private static final ManagedObjectId CU_CELL_RESOURCE = new ManagedObjectId(REFERENCE, "GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1");
    private static final ManagedObjectId NRFREQUENCY_RESOURCE = new ManagedObjectId(REFERENCE, "GNBCUCPFunction=1,NRNetwork=1,NRFrequency=1");
    private static final ManagedObjectId CELL_RELATION_RESOURCE = new ManagedObjectId(REFERENCE, "GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1,NRCellRelation=1");
    private static final ManagedObjectId NORE_RESOURCE = CELL_RESOURCE.fetchParentId();
    private static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    private static final String OPTIONS = "options";

    @Autowired
    private NcmpClient ncmpClient;
    @Autowired
    private CmNrCellDuRepo cellDuRepo;
    @Autowired
    private CmNrSectorCarrierRepo sectorCarrierRepo;
    @Autowired
    private CmGNBCUCPFunctionRepo gnbcucpFunctionRepo;
    @Autowired
    private CmNrCellCuRepo cellCuRepo;
    @Autowired
    private CmNrCellRelationRepo cellRelationRepo;
    @Autowired
    private CmNrFrequencyRepo frequencyRepo;

    @SpyBean
    MoSaver moSaver;

    @BeforeEach
    void setup() {
        stubFor(get(urlPathEqualTo("/cps/api/v1/dataspaces/NCMP-Admin/anchors/ncmp-dmi-registry/nodes/query"))
                .withQueryParam("cps-path", equalTo("//additional-properties[@name='targetDnPrefix']/value[text()='SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002']/ancestor::cm-handles"))
                .withQueryParam("include-descendants", equalTo(Boolean.TRUE.toString()))
                .willReturn(WireMock.aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBodyFile("ncmp/cm-handle-details.json")));
        stubFor(post(urlPathEqualTo("/auth/v1/login"))
                .willReturn(WireMock.aResponse()
                        .withHeader(CONTENT_TYPE, TEXT_PLAIN)
                        .withBody("253204e8-793b-4060-9bb3-78c078423afd")));
    }

    @Test
    void getCmResourceCmHandleFailure() {
        chHandleFailedStub();
        assertTrue(ncmpClient.getCmResource(CELL_RESOURCE, NRCellDU.class).isEmpty());
    }

    @Test
    void getCmResourceGetCmDataFailure() {
        stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam(RESOURCE_IDENTIFIER, equalTo(Constants.SLASH))
                .withQueryParam(OPTIONS, equalTo(OPTION_FIELDS_VALUE))
                .willReturn(WireMock.aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertTrue(ncmpClient.getCmResource(CELL_RESOURCE, NRCellDU.class).isEmpty());
    }

    @Test
    void patchCmResourceCmHandleFailure() {
        chHandleFailedStub();
        NRCellDU cellDU = NRCellDU.builder().pZeroNomPuschGrant(-54).build();
        cellDU.setObjectId(new ManagedObjectId(REFERENCE, CELL_RESOURCE.getRefId()));
        assertFalse(ncmpClient.patchCmResource(cellDU));
    }

    @Test
    void patchCmResourceCmDataFailure() {
        NRCellDU cellDU = NRCellDU.builder().pZeroNomPuschGrant(-54).build();
        cellDU.setObjectId(new ManagedObjectId(REFERENCE, CELL_RESOURCE.getRefId()));

        stubFor(patch(urlPathEqualTo(PATH))
                .withQueryParam(RESOURCE_IDENTIFIER, equalTo(NcmpClient.toNcmpRefId(NORE_RESOURCE.getRefId())))
                .withRequestBody(equalToJson("{\"NRCellDU\":[{\"id\":\"NR03gNodeBRadio00002-1\",\"attributes\":{\"pZeroNomPuschGrant\":-54}}]}"))
                .willReturn(WireMock.aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertFalse(ncmpClient.patchCmResource(cellDU));
    }

    private void chHandleFailedStub() {
        stubFor(get(urlPathEqualTo("/cps/api/v1/dataspaces/NCMP-Admin/anchors/ncmp-dmi-registry/nodes/query"))
                .withQueryParam("cps-path", equalTo("//additional-properties[@name='targetDnPrefix']/value[text()='SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002']/ancestor::cm-handles"))
                .withQueryParam("include-descendants", equalTo(Boolean.TRUE.toString()))
                .willReturn(WireMock.aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    @Test
    void getCmResource() {
        stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam(RESOURCE_IDENTIFIER, equalTo(Constants.SLASH))
                .withQueryParam(OPTIONS, equalTo(OPTION_FIELDS_VALUE))
                .willReturn(WireMock.aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBodyFile("ncmp/resources.json")));

        //NRCellDU
        Optional<NRCellDU> optionalCellDU = ncmpClient.getCmResource(CELL_RESOURCE, NRCellDU.class);
        assertTrue(optionalCellDU.isPresent());
        NRCellDU cellDU = optionalCellDU.get();
        assertEquals(REFERENCE, cellDU.getObjectId().getMeFdn());
        assertEquals(CELL_RESOURCE.getRefId(), cellDU.getObjectId().getRefId());
        assertEquals(2, cellDuRepo.findAll().size());

        //NRSectorCarrier
        NRSectorCarrier sectorCarrier = cellDU.getNRSectorCarriers().get(0);
        assertEquals(REFERENCE, sectorCarrier.getObjectId().getMeFdn());
        assertEquals(SECTOR_CARRIER_RESOURCE.getRefId(), sectorCarrier.getObjectId().getRefId());
        assertEquals(2, sectorCarrierRepo.findAll().size());

        //NRCellCU
        Optional<NRCellCU> optionalCellCU = cellCuRepo.findById(CU_CELL_RESOURCE);
        assertTrue(optionalCellCU.isPresent());
        NRCellCU cellCU = optionalCellCU.get();
        assertEquals(REFERENCE, cellCU.getObjectId().getMeFdn());
        assertEquals(CU_CELL_RESOURCE.getRefId(), cellCU.getObjectId().getRefId());
        assertEquals(1, cellCuRepo.findAll().size());

        //NRFrequency
        NRFrequency nrFrequency = cellCU.getNRFrequency();
        assertEquals(REFERENCE, nrFrequency.getObjectId().getMeFdn());
        assertEquals(NRFREQUENCY_RESOURCE.getRefId(), nrFrequency.getObjectId().getRefId());
        assertEquals(2, frequencyRepo.findAll().size());

        //NRCellRelation
        NRCellRelation cellRelation = cellRelationRepo.getReferenceById(CELL_RELATION_RESOURCE);
        assertEquals(REFERENCE, cellRelation.getObjectId().getMeFdn());
        assertEquals(CELL_RELATION_RESOURCE.getRefId(), cellRelation.getObjectId().getRefId());
        assertEquals(1, cellRelationRepo.findAll().size());
    }

    @Test
    void getCmResourceTransactional() {
        stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam(RESOURCE_IDENTIFIER, equalTo(Constants.SLASH))
                .withQueryParam(OPTIONS, equalTo(OPTION_FIELDS_VALUE))
                .willReturn(WireMock.aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBodyFile("ncmp/resources.trans.json")));
        int maxInvocations = 3;
        AtomicInteger invocations = new AtomicInteger(0);
        doAnswer(invocation -> {
            int invoked = invocations.incrementAndGet();
            if (invoked > maxInvocations) {
                throw new NullPointerException();
            }
            return invocation.callRealMethod();
        }).when(moSaver).convertMOToType(any());
        assertThrows(NullPointerException.class, () -> ncmpClient.getCmResource(CELL_RESOURCE, NRCellDU.class));
        assertTrue(cellCuRepo.findAll().isEmpty());
    }

    @Test
    void patchCmResource() {
        NRCellDU cellDU = NRCellDU.builder().pZeroNomPuschGrant(-54).build();
        cellDU.setObjectId(new ManagedObjectId(REFERENCE, CELL_RESOURCE.getRefId()));

        stubFor(patch(urlPathEqualTo(PATH))
                .withQueryParam(RESOURCE_IDENTIFIER, equalTo(NcmpClient.toNcmpRefId(NORE_RESOURCE.getRefId())))
                .withRequestBody(equalToJson("{\"NRCellDU\":[{\"id\":\"NR03gNodeBRadio00002-1\",\"attributes\":{\"pZeroNomPuschGrant\":-54}}]}"))
                .willReturn(WireMock.aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBodyFile("ncmp/patch.json")));

        assertTrue(ncmpClient.patchCmResource(cellDU));
    }
}
