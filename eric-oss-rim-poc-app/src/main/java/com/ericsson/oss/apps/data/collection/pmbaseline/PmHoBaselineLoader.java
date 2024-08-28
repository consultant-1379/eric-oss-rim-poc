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
package com.ericsson.oss.apps.data.collection.pmbaseline;

import com.ericsson.oss.apps.client.BdrClient;
import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.data.collection.FileTracker;
import com.ericsson.oss.apps.data.collection.pmbaseline.counters.PmBaselineHoCoefficient;
import com.ericsson.oss.apps.model.CGI;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PmHoBaselineLoader extends PmBaselineLoader<PmBaselineHoCoefficient> {

    @Value("${app.data.gNBIdLength:24}")
    @Setter
    private int gNBIdLength;

    @Value("${app.data.netsim}")
    @Setter
    private boolean retainHoFdnAsIs;

    private static final String MCC = "mcc";
    private static final String MNC = "mnc";
    private static final String G_NBID = "gNBId";
    private static final String CELL_LOCAL_ID = "cellLocalId";

    public PmHoBaselineLoader(
            Class<PmBaselineHoCoefficient> clazz,
            JpaRepository<PmBaselineHoCoefficient, String> repository,
            BdrClient bdrClient,
            BdrConfiguration bdrConfiguration,
            FileTracker fileTracker
    ) {
        super(clazz, repository, bdrClient, bdrConfiguration, fileTracker);
    }

    Pattern pattern = Pattern.compile("^((SubNetwork=[\\-\\w]+,)+MeContext=[\\-\\w]+,)?ManagedElement=[\\-\\w]+,GNBCUCPFunction=[\\-\\w]+,NRCellCU=[\\-\\w]+,NRCellRelation=" +
            "(?<" + MCC + ">\\d+)" +
            "\\_(?<" + MNC + ">\\d+)" +
            "\\_(?<" + G_NBID + ">\\d+)" +
            "\\_(?<" + CELL_LOCAL_ID + ">\\d+)$");

    @Override
    protected long loadCsvData(String objectPath) {
        // EBS generated relation fdns need remapping
        Function<PmBaselineHoCoefficient, Optional<PmBaselineHoCoefficient>> entityFinalizer = this::mapFdnFromEBSToCMFormat;
        // netsim does not use the NCI format for fdns e.g. auto<NCI>
        // so the fdns for relations are taken as they are
        if (retainHoFdnAsIs) {
            entityFinalizer = Optional::of;
        }
        return loadCsvData(objectPath, entityFinalizer);
    }

    /**
     * EBS uses a dash-separated format for the relation name e.g. mcc-mnc-etc. while
     * cm use autoNCI (NCI is calculated from the same parameters). If we want to match the data
     * we have to remap them.
     *
     * @param pmBaselineHoCoefficient input PmBaselineHoCoefficient
     * @return remapped PmBaselineHoCoefficient is matching fdn EBS naming or empty if it doesn't
     */
    @NotNull
    private Optional<PmBaselineHoCoefficient> mapFdnFromEBSToCMFormat(PmBaselineHoCoefficient pmBaselineHoCoefficient) {
        Matcher matcher = pattern.matcher(pmBaselineHoCoefficient.getFdnNrCellRelation());
        if (pmBaselineHoCoefficient.getNumberHandovers() == null || !matcher.matches()) {
            return Optional.empty();
        }
        try {
            CGI cgi = getCGIFromMatcher(matcher);
            String autoNci = "auto" + cgi.getAutonNci();
            String cgiRelationFdn = pmBaselineHoCoefficient.getFdnNrCellRelation();
            pmBaselineHoCoefficient.setFdnNrCellRelation(cgiRelationFdn.substring(0, cgiRelationFdn.lastIndexOf("=") + 1) + autoNci);
            return Optional.of(pmBaselineHoCoefficient);
        } catch (NumberFormatException numberFormatException) {
            log.error("Cannot parse cgi data {} because of {}", matcher.group(0), numberFormatException);
            return Optional.empty();
        }
    }

    private CGI getCGIFromMatcher(Matcher matcher) {
        return CGI.builder()
                .mcc(Integer.parseInt(matcher.group(MCC)))
                .mnc(Integer.parseInt(matcher.group(MNC)))
                .gNBId(Long.parseLong(matcher.group(G_NBID)))
                .cellLocalId(Integer.parseInt(matcher.group(CELL_LOCAL_ID)))
                .gNBIdLength(gNBIdLength)
                .build();
    }
}
