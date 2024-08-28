/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "tropoduct.baseUrlAndPath=http://localhost:${wiremock.server.port}/WW/images",
    "tropoduct.loadFromUrl=true",
    "bdr.bucket=rim",
    "tropoduct.forecastRopMinutes=180",
    "tropoduct.forecastAdvanceMinutes=90",
    "tropoduct.retryUrlTimes=2", "tropoduct.backoffRetryUrlMilliSeconds=1000" })

@AutoConfigureWireMock(port = 0)
class WeatherDataProviderUrlTest {
    public static final String RIM = "rim";
    private static final String E_TAG = "fromUrl";

    @Mock
    private InputStream inputStream;

    @Autowired
    @SpyBean
    private WeatherDataProvider weatherDataProvider;

    @Value("${wiremock.server.port}")
    private String wireMockServerPort;

    @BeforeEach
    public void setUp() throws IOException {
        weatherDataProvider.setAllowHttp(true);
    }

    //655219700000  = Tuesday, June 14, 2022 3:15:00 PM => 1655218800000  Tuesday, June 14, 2022 15:00:00 => ww14-15.tif ==>  /geospatial/ducting/ww-1655218800000.tif
    private long predectionTimeStamp = 1655218800000L;
    private String stubPath = "/WW/images/ww14-15.tif";

    /**
     *  Happy Case test.
     *  1655219700000  = Tuesday, June 14, 2022 3:15:00 PM => 1655218800000  Tuesday, June 14, 2022 15:00:00 => ww14-15.tif ==>  /geospatial/ducting/ww-1655218800000.tif
     *  1655225100000  = Tuesday, June 14, 2022 4:45:00 PM => 1655229600000  Tuesday, June 14, 2022 18:00:00 => ww14-18.tif ==>  /geospatial/ducting/ww-1655229600000.tif
     */
    @ParameterizedTest
    @CsvSource(value = { "1655218800000, ww14-15.tif", "1655229600000, ww14-18.tif" })
    void urlLoaderOKTest(final long predictionTimestampPt, final String filename) throws IOException {
        stubFor(get(urlPathEqualTo("/WW/images/" + filename)).willReturn(WireMock.aResponse().withStatus(HttpStatus.OK.value())));
        String urlFilePath = "http://localhost:" + wireMockServerPort + "/WW/images/" + filename;
        assertThat(weatherDataProvider.getObjectInputStream(predictionTimestampPt)).isNotNull();
        verify(weatherDataProvider, times(1)).getObjectInputStream(predictionTimestampPt);
        assertThat(weatherDataProvider.getPathToWeatherData()).isEqualTo(urlFilePath);
        assertThat(weatherDataProvider.getETag(predictionTimestampPt)).isEqualTo(E_TAG + predictionTimestampPt);
    }

    @Test
    void urlLoaderHttpTest() throws IOException {
        weatherDataProvider.setAllowHttp(false);
        stubFor(get(urlPathEqualTo(stubPath)).willReturn(WireMock.aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
        String filePath = "http://localhost:" + wireMockServerPort + "/WW/images/ww14-15.tif";
        assertThat(weatherDataProvider.getObjectInputStream(predectionTimeStamp)).isNull();
        verify(weatherDataProvider, times(1)).getObjectInputStream(predectionTimeStamp);
        assertThat(weatherDataProvider.getPathToWeatherData()).isEqualTo(filePath);
    }


    @Test
    void urlLoaderNoRetryTest() throws IOException {
        stubFor(get(urlPathEqualTo(stubPath)).willReturn(WireMock.aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
        String filePath = "http://localhost:" + wireMockServerPort + "/WW/images/ww14-15.tif";
        assertThat(weatherDataProvider.getObjectInputStream(predectionTimeStamp)).isNull();
        verify(weatherDataProvider, times(1)).getObjectInputStream(predectionTimeStamp);
        assertThat(weatherDataProvider.getPathToWeatherData()).isEqualTo(filePath);
    }

    @Test
    void urlLoaderRetryTest() throws IOException {
        stubFor(get(urlPathEqualTo(stubPath)).willReturn(WireMock.aResponse().withStatus(HttpStatus.REQUEST_TIMEOUT.value())));
        String filePath = "http://localhost:" + wireMockServerPort + "/WW/images/ww14-15.tif";
        assertThat(weatherDataProvider.getObjectInputStream(predectionTimeStamp)).isNull();
        verify(weatherDataProvider, times(2)).getObjectInputStream(predectionTimeStamp);
        assertThat(weatherDataProvider.getPathToWeatherData()).isEqualTo(filePath);
    }

}