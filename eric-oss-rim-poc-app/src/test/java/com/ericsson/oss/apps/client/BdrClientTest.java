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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
class BdrClientTest {

    private static final String BUCKET_NAME = "rim";
    private static final String KEY_NAME = "testfile.xml";
    private static final String EXPECTED_ETAG = "expectedEtag";

    @Mock
    private S3Client amazonS3;
    @InjectMocks
    private BdrClient bdrClient;

    @Captor
    ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor;

    @Captor
    ArgumentCaptor<GetObjectRequest> getObjectRequestArgumentCaptor;

    @Captor
    ArgumentCaptor<RequestBody> requestBodyArgumentCaptor;

    @Captor
    ArgumentCaptor<HeadObjectRequest> headObjectRequestArgumentCaptor;

    @Mock
    HeadObjectResponse headObjectResponse;

    @Test
    void testUploadInputStreamObject() throws IOException {
        var bais = getByteArrayStream();
        bdrClient.uploadInputStreamObject(BUCKET_NAME, KEY_NAME, bais);
        Mockito.verify(amazonS3).putObject(putObjectRequestArgumentCaptor.capture(), requestBodyArgumentCaptor.capture());
        verifyRequest(putObjectRequestArgumentCaptor.getValue().bucket(), putObjectRequestArgumentCaptor.getValue().key());
        assertArrayEquals(bais, requestBodyArgumentCaptor.getValue().contentStreamProvider().newStream().readAllBytes());
    }

    @Test
    void testGetInputStreamObject() {
        ResponseInputStream<GetObjectResponse> mockInputStream = mock(ResponseInputStream.class);
        Mockito.when(amazonS3.getObject(any(GetObjectRequest.class))).thenReturn(mockInputStream);
        assertEquals(mockInputStream, bdrClient.getObjectInputStream(BUCKET_NAME, KEY_NAME));
        Mockito.verify(amazonS3).getObject(getObjectRequestArgumentCaptor.capture());
        verifyRequest(getObjectRequestArgumentCaptor.getValue().bucket(), getObjectRequestArgumentCaptor.getValue().key());
    }

    @Test
    void testGetEtag() {
        Mockito.when(amazonS3.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);
        String etag = getEtagAndVerifyRequest(EXPECTED_ETAG);
        assertEquals(EXPECTED_ETAG, etag);
    }

    @Test
    void testGetEmptyEtag() {
        Mockito.when(amazonS3.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);
        String etag = getEtagAndVerifyRequest("");
        assertFalse(etag.isEmpty());
    }

    @Test
    void testGetNullEtag() {
        Mockito.when(amazonS3.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);
        String etag = getEtagAndVerifyRequest(null);
        assertFalse(etag.isEmpty());
    }


    @Test
    void testRuntimeExceptionEtag() {
        Mockito.when(amazonS3.headObject(any(HeadObjectRequest.class))).thenThrow(new RuntimeException());
        String etag = bdrClient.getETag(BUCKET_NAME, KEY_NAME);
        Mockito.verify(amazonS3).headObject(headObjectRequestArgumentCaptor.capture());
        verifyRequest(headObjectRequestArgumentCaptor.getValue().bucket(), headObjectRequestArgumentCaptor.getValue().key());
        assertFalse(etag.isEmpty());
        assertNotEquals(EXPECTED_ETAG, etag);
    }

    private String getEtagAndVerifyRequest(String returnEtag) {
        Mockito.when(headObjectResponse.eTag()).thenReturn(returnEtag);
        String etag = bdrClient.getETag(BUCKET_NAME, KEY_NAME);
        Mockito.verify(amazonS3).headObject(headObjectRequestArgumentCaptor.capture());
        verifyRequest(headObjectRequestArgumentCaptor.getValue().bucket(), headObjectRequestArgumentCaptor.getValue().key());
        return etag;
    }

    private void verifyRequest(String bucket, String key) {
        assertEquals(BUCKET_NAME, bucket);
        assertEquals(KEY_NAME, key);
    }

    @Test
    void testUploadInputStreamObjectFail() throws IOException {
        var bais = getByteArrayStream();
        Mockito.when(amazonS3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(AwsServiceException.builder().message("Test").build());
        assertFalse(bdrClient.uploadInputStreamObject(BUCKET_NAME, KEY_NAME, bais));
    }

    private byte[] getByteArrayStream() throws IOException {
        String testfile = Objects.requireNonNull(getClass().getClassLoader().getResource("bdr/" + KEY_NAME)).getFile();
        File localFile = new File(testfile);
        return Files.readAllBytes(localFile.toPath());
    }

}

