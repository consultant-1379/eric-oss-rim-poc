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
package com.ericsson.oss.apps.data.collection.features.report.nrcelldu;

import com.ericsson.oss.apps.client.CtsClient;
import com.ericsson.oss.apps.config.ThreadingConfig;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.report.ReportSaver;
import com.ericsson.oss.apps.model.mom.NRCellDU;
import com.ericsson.oss.apps.repositories.CmNrCellDuRepo;
import com.ericsson.oss.apps.utils.Utils;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NRCellDUReporter{

    public static final String NR_CELL_DU = "NRCellDU";
    private final ReportSaver reportSaver;
    private final CmNrCellDuRepo cellDuRepo;
    private final CtsClient ctsClient;
    private final ThreadingConfig threadingConfig;

    public void handle(FeatureContext context) {
        List<NRCellDUReportingObject> nrCellDUReportingObjectList = Utils.of().processInThreadPool(cellDuRepo.findAll(), threadingConfig.getPoolSizeForCtsGeoQuery(), this::createNrCellDuReportList, context);
        if (!nrCellDUReportingObjectList.isEmpty()) {
            reportSaver.createReport(nrCellDUReportingObjectList, context.getRopTimeStamp(), NR_CELL_DU);
        }
    }

    @VisibleForTesting
    List<NRCellDUReportingObject> createNrCellDuReportList(Collection<NRCellDU> nrCellDUList, FeatureContext context) {
        return nrCellDUList.parallelStream().map(nrCellDU -> {
            NRCellDUReportingObject nrCellDUReportingObject = new NRCellDUReportingObject();

            BeanUtils.copyProperties(nrCellDU, nrCellDUReportingObject);
            nrCellDUReportingObject.setFdn(nrCellDU.getObjectId().toFdn());
            nrCellDUReportingObject.setRopTime(context.getRopTimeStamp());
            if (!nrCellDU.getNRSectorCarriers().isEmpty()) {
                nrCellDUReportingObject.setArfcnDL(nrCellDU.getNRSectorCarriers().get(0).getArfcnDL());
                nrCellDUReportingObject.setBSChannelBwDL(nrCellDU.getNRSectorCarriers().get(0).getBSChannelBwDL());
            }
            ctsClient.getNrCellGeoData(nrCellDU.getObjectId().toFdn()).stream()
                    .peek(fdn -> log.debug("Extracting geo location information from CTS for cell {}", fdn))
                    .filter(geoData -> Objects.nonNull(geoData.getCoordinate()))
                    .peek(geoData -> {
                        nrCellDUReportingObject.setLat(geoData.getCoordinate().getY());
                        nrCellDUReportingObject.setLon(geoData.getCoordinate().getX());
                    })
                    .filter(geoData -> Objects.nonNull(geoData.getBearing()))
                    .forEach(geoData -> nrCellDUReportingObject.setBearing(geoData.getBearing()));
            return nrCellDUReportingObject;
        }).collect(Collectors.toUnmodifiableList());
    }
}
