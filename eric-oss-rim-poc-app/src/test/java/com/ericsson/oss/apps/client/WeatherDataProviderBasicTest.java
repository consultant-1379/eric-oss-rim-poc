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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;

import com.ericsson.oss.apps.config.BdrConfiguration;
import com.ericsson.oss.apps.config.DuctDetectionConfig;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherDataProviderBasicTest {
    public static final String RIM = "rim";
    private static final String E_TAG = "fromUrl";
    private static final String E_TAG_BDR = "d41d8cd98f00b204e9800998ecf8427e-2";
    private static final String BASE_URL = "src/test/resources/tropo/WW/images";

    @Mock
    private DuctDetectionConfig ductDetectionConfig;

    @Mock
    private BdrConfiguration bdrConfiguration;

    @Mock
    private BdrClient bdrClient;

    @Mock
    private InputStream inputStream;

    @InjectMocks
    @Spy
    private WeatherDataProvider weatherDataProvider;

    //1655219700000  = Tuesday, June 14, 2022 3:15:00 PM => 1655218800000  Tuesday, June 14, 2022 15:00:00 => ww14-15.tif ==>  /geospatial/ducting/ww-1655218800000.tif
    private final String filePath = "/geospatial/ducting/ww-1655218800000.tif";
    private final long predictionTimestamp = 1655218800000L;

    @BeforeEach
    public void setUp() throws IOException {
        Mockito.when(ductDetectionConfig.isLoadFromUrl()).thenReturn(false);
        Mockito.lenient().when(ductDetectionConfig.getBaseUrlAndPath()).thenReturn(BASE_URL);
        Mockito.lenient().when(bdrConfiguration.getBucket()).thenReturn(RIM);
        Mockito.lenient().when(bdrClient.getETag(Mockito.anyString(), Mockito.anyString())).thenReturn(E_TAG_BDR);
        Mockito.lenient().when(bdrClient.getObjectInputStream(Mockito.anyString(), Mockito.anyString())).thenReturn(inputStream);
        weatherDataProvider.setAllowHttp(false);
    }


    @Test
    void bdrLoaderOKTest() throws IOException {
        assertThat(weatherDataProvider.getObjectInputStream(predictionTimestamp)).isNotNull();
        runAndAssert(filePath, predictionTimestamp, E_TAG_BDR);
    }


    @Test
    void bdrLoaderExceptionTest() throws IOException {
        Mockito.when(bdrClient.getObjectInputStream(Mockito.anyString(), Mockito.anyString())).thenThrow(AwsServiceException.builder().message("Test").build());
        assertThrows(AwsServiceException.class, () -> weatherDataProvider.getObjectInputStream(predictionTimestamp), "Expected AwsServiceException");
        runAndAssert(filePath, predictionTimestamp, E_TAG_BDR);
    }

    /**
     *  Happy Case test.
     *  1655219700000  = Tuesday, June 14, 2022 3:15:00 PM => 1655218800000  Tuesday, June 14, 2022 15:00:00 => ww14-15.tif ==>  /geospatial/ducting/ww-1655218800000.tif
     *  1655225100000  = Tuesday, June 14, 2022 4:45:00 PM => 1655229600000  Tuesday, June 14, 2022 18:00:00 => ww14-18.tif ==>  /geospatial/ducting/ww-1655229600000.tif
     */
    @ParameterizedTest
    @CsvSource(value = { "1655218800000, ww14-15.tif", "1655229600000, ww14-18.tif" })
    void urlLoaderOkTest(final long predictionTimestampPt, final String filename) throws IOException {
        Mockito.when(ductDetectionConfig.isLoadFromUrl()).thenReturn(true);
        weatherDataProvider.setAllowHttp(true);
        String urlFilePath = BASE_URL + "/" + filename;
        URLConnection urlConnection = Path.of(urlFilePath).toUri().toURL().openConnection();
        Mockito.doReturn(urlConnection).when(weatherDataProvider).getUrlConnection();
        assertThat(weatherDataProvider.getObjectInputStream(predictionTimestampPt)).isNotNull();
        runAndAssert(urlFilePath, predictionTimestampPt, E_TAG + predictionTimestampPt);
    }

    @Test
    void urlLoaderMalFormedUrlTest() throws IOException {
        Mockito.when(ductDetectionConfig.isLoadFromUrl()).thenReturn(true);
        String urlFilePath = BASE_URL + "/ww14-15.tif";
        assertThrows(java.net.MalformedURLException.class, () -> weatherDataProvider.getObjectInputStream(predictionTimestamp), "Expected MalformedURLException");
        runAndAssert(urlFilePath, predictionTimestamp, E_TAG + predictionTimestamp);
    }

    private void runAndAssert(String filePath, long predectionTimeStamp, String etag) throws IOException {
        verify(weatherDataProvider, times(1)).getObjectInputStream(predectionTimeStamp);
        assertThat(weatherDataProvider.getPathToWeatherData()).isEqualTo(filePath);
        assertThat(weatherDataProvider.getETag(predectionTimeStamp)).isEqualTo(etag);
    }
}