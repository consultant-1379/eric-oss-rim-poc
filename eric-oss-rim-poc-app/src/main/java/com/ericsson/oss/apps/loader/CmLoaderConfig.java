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
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.loader.schema.*;
import com.ericsson.oss.apps.model.GeoData;
import com.ericsson.oss.apps.model.mom.*;
import com.ericsson.oss.apps.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.stream.Stream;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.datasource.exposed", havingValue = "true")
public class CmLoaderConfig {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private BdrClient bdrClient;
    @Autowired
    private BdrConfiguration bdrConfiguration;
    private static final String RESOURCE_FOLDER = "setup_files/cm/";
    @Value("${app.data.customerid}")
    private String customerId;

    @EventListener(ApplicationReadyEvent.class)
    public void loadDatabase() {
        log.info("=== DataLoading started ===");
        Stream.of("gNodeBDuLoader", "gNodeBCuCpLoader", "externalGNodeBCuCpLoader", "externalNRCellCuLoader", "nrCellCuLoader",
                        "nrCellRelationLoader", "nrSectorCarrierLoader", "nrCellDuLoader", "geoDataLoader")
                .peek(filename -> log.info("--- Loading Cm Data for {} ", filename))
                .map(x -> context.getBean(x, CmBaseLoader.class))
                .forEach(CmBaseLoader::load);
        log.info("=== DataLoading finished ===");
    }

    @Bean
    CmBaseLoader<Node, GNBDUFunction> gNodeBDuLoader(CmGNBDUFunctionRepo cmGNBDUFunctionRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), Node.class, getObjectPath("gnbdu_fn")) {
            @Override
            public GNBDUFunction transform(Node dataBean) {
                GNBDUFunction node = new GNBDUFunction();
                configureNode(node, dataBean);
                return node;
            }
        };
    }

    @Bean
    CmBaseLoader<Node, GNBCUCPFunction> gNodeBCuCpLoader(CmGNBCUCPFunctionRepo cmGNBCUCPFunctionRepo) {
        return new CmBaseLoader<>(entityManager,bdrClient, bdrConfiguration.getBucket(), Node.class, getObjectPath("gnbcucpfunction")) {
            @Override
            public GNBCUCPFunction transform(Node dataBean) {
                GNBCUCPFunction node = new GNBCUCPFunction();
                configureNode(node, dataBean);
                return node;
            }
        };
    }

    @Bean
    CmBaseLoader<Node, ExternalGNBCUCPFunction> externalGNodeBCuCpLoader(CmExternalGNBCUCPFunctionRepo cmExternalGNBCUCPFunctionRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), Node.class, getObjectPath("external_gnbcucp_fn")) {
            @Override
            public ExternalGNBCUCPFunction transform(Node dataBean) {
                ExternalGNBCUCPFunction node = new ExternalGNBCUCPFunction();
                configureNode(node, dataBean);
                return node;
            }
        };
    }

    private void configureNode(GNodeB gNodeB, Node dataBean) {
        gNodeB.setObjectId(new ManagedObjectId(dataBean.getFdn()));
        gNodeB.setGNBId(dataBean.getGNBId());
        gNodeB.setGNBIdLength(dataBean.getGNBIdLength());
        PLMNId plmn = new PLMNId(dataBean.getMcc(), dataBean.getMnc());
        gNodeB.setPLMNId(plmn);
    }

    @Bean
    CmBaseLoader<Frequency, NRFrequency> nrFrequencyLoader(CmNrFrequencyRepo cmNrFrequencyRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), Frequency.class, getObjectPath("nr_frequency")) {
            @Override
            public NRFrequency transform(Frequency dataBean) {
                NRFrequency frequency = new NRFrequency();
                frequency.setObjectId(new ManagedObjectId(dataBean.getFdn()));
                frequency.setArfcnValueNRDl(dataBean.getArfcnValueNRDl());
                return frequency;
            }
        };
    }

    @Bean
    CmBaseLoader<CellCu, ExternalNRCellCU> externalNRCellCuLoader(CmExternalNrCellCuRepo cmExternalNrCellCuRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), CellCu.class, getObjectPath("external_nrcell_cu")) {
            @Override
            public ExternalNRCellCU transform(CellCu dataBean) {
                ExternalNRCellCU cell = new ExternalNRCellCU();
                cell.setObjectId(new ManagedObjectId(dataBean.getFdn()));
                cell.setCellLocalId(dataBean.getCellLocalId());
                return cell;
            }
        };
    }

    @Bean
    CmBaseLoader<CellCu, NRCellCU> nrCellCuLoader(CmNrFrequencyRepo cmNrFrequencyRepo, CmNrCellCuRepo cmNrCellCuRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), CellCu.class, getObjectPath("nrcell_cu")) {
            @Override
            public NRCellCU transform(CellCu dataBean) {
                NRCellCU cell = new NRCellCU();
                cell.setObjectId(new ManagedObjectId(dataBean.getFdn()));
                cell.setCellLocalId(dataBean.getCellLocalId());
                cell.setNCI(dataBean.getNCI());
                cmNrFrequencyRepo.findById(ManagedObjectId.of(dataBean.getNRFrequencyRef())).ifPresent(cell::setNRFrequency);
                return cell;
            }
        };
    }

    @Bean
    CmBaseLoader<CellRelation, NRCellRelation> nrCellRelationLoader(CmNrCellRelationRepo cmNrCellRelationRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), CellRelation.class, getObjectPath("cell_relation")) {
            @Override
            public NRCellRelation transform(CellRelation dataBean) {
                NRCellRelation cellRelation = new NRCellRelation();
                cellRelation.setObjectId(new ManagedObjectId(dataBean.getFdn()));
                cellRelation.setCellIndividualOffsetNR(dataBean.getCellIndividualOffsetNR());
                // Override null values to true as original CM Data has no entry for isHoAllowed
                cellRelation.isHoAllowed(dataBean.isHoAllowed() == null || dataBean.isHoAllowed());
                cellRelation.setNRCellRef(new ManagedObjectId(dataBean.getNRCellRefId()));
                return cellRelation;
            }
        };
    }

    @Bean
    CmBaseLoader<SectorCarrier, NRSectorCarrier> nrSectorCarrierLoader(CmNrSectorCarrierRepo cmNrSectorCarrierRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), SectorCarrier.class, getObjectPath("nrsector_carrier")) {
            @Override
            public NRSectorCarrier transform(SectorCarrier dataBean) {
                NRSectorCarrier sectorCarrier = new NRSectorCarrier();
                sectorCarrier.setObjectId(new ManagedObjectId(dataBean.getFdn()));
                sectorCarrier.setAdministrativeState(dataBean.getAdministrativeState());
                sectorCarrier.setBSChannelBwUL(dataBean.getBSChannelBwUL());
                sectorCarrier.setBSChannelBwDL(dataBean.getBSChannelBwUL());
                sectorCarrier.setArfcnUL(dataBean.getArfcnUL());
                sectorCarrier.setArfcnDL(dataBean.getArfcnDL());
                return sectorCarrier;
            }
        };
    }

    @Bean
    CmBaseLoader<CellDu, NRCellDU> nrCellDuLoader(CmNrSectorCarrierRepo cmNrSectorCarrierRepo, CmNrCellDuRepo cmNrCellDuRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), CellDu.class, getObjectPath("nrcell_du")) {
            @Override
            public NRCellDU transform(CellDu dataBean) {
                NRCellDU cell = new NRCellDU();
                cell.setObjectId(new ManagedObjectId(dataBean.getFdn()));
                cell.setAdministrativeState(dataBean.getAdministrativeState());
                cell.setCellLocalId(dataBean.getCellLocalId());
                cell.setNCI(dataBean.getNCI());
                cell.setPZeroNomPuschGrant(dataBean.getPZeroNomPuschGrant());
                cell.setPZeroUePuschOffset256Qam(dataBean.getPZeroUePuschOffset256Qam());
                cell.setSubCarrierSpacing(dataBean.getSubCarrierSpacing());
                cell.setTddBorderVersion(dataBean.getTddBorderVersion());
                cell.setBandList(dataBean.getBandList());
                cell.setBandListManual(dataBean.getBandListManual());
                cell.setTddSpecialSlotPattern(dataBean.getTddSpecialSlotPattern());
                cell.setTddUlDlPattern(dataBean.getTddUlDlPattern());
                cell.setAdvancedDlSuMimoEnabled(dataBean.getAdvancedDlSuMimoEnabled()!=null && dataBean.getAdvancedDlSuMimoEnabled());
                cmNrSectorCarrierRepo.findById(ManagedObjectId.of(dataBean.getNRSectorCarrierRef())).map(Collections::singletonList)
                        .ifPresent(cell::setNRSectorCarriers);
                return cell;
            }
        };
    }

    @Bean
    CmBaseLoader<Geo, GeoData> geoDataLoader(GeoDataRepo geoDataRepo) {
        return new CmBaseLoader<>(entityManager, bdrClient, bdrConfiguration.getBucket(), Geo.class, getObjectPath("geo_data")) {
            @Override
            public GeoData transform(Geo dataBean) {
                if (dataBean.getLon() == null || dataBean.getLat() == null) {
                    log.warn("cell {} has no geo location information!", dataBean.getFdn());
                    return null;
                }
                return GeoData.builder()
                        .fdn(dataBean.getFdn())
                        .bearing(dataBean.getBearing() != null ? (int) (dataBean.getBearing() * 10) : null)
                        .coordinate(new Coordinate(dataBean.getLon(), dataBean.getLat()))
                        .m_dtilts(dataBean.getM_dtilts())
                        .e_dtilts(dataBean.getE_dtilts())
                        .build();
            }
        };
    }

    private String getObjectPath(String fileName) {
        return String.format("%s%s-%s.csv.gz", RESOURCE_FOLDER, fileName, customerId);
    }
}
