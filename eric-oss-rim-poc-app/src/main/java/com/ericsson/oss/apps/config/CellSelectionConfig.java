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

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * The Class CellSelectionConfig.
 */
@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "cell-selection-config")
public class CellSelectionConfig {

    @CsvBindByName
    private double maxDeltaIPNThresholdDb;
    @CsvBindByName
    private double minRemoteInterferenceVictimCellDb;
    @CsvBindByName
    private double maxRemoteInterferenceNeighborCellDb;
    @CsvBindByName
    private int maxRankSelectBestNeighborCells;
    @CsvBindByName
    private double acceptHandoversAboveHoPercent;
    @CsvBindByName
    private int p0RejectNumberHandoversBelowValue;
    @CsvBindByName
    private int cioAcceptTopRankedValue;
    @CsvBindByName
    private int cioRejectNumberHandoversBelowValue;
    @CsvBindByName
    private double cioRejectVictimBandwidthRatio;
}