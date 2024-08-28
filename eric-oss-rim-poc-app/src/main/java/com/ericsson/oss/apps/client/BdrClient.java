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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BdrClient {

    private final S3Client s3Client;

    public InputStream getObjectInputStream(final String bucketName, String objectPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectPath)
                .build();
        return s3Client.getObject(getObjectRequest);
    }

    public boolean uploadInputStreamObject(String bucketName, String objectName, byte[] inputStream) {
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();
            s3Client.putObject(objectRequest, RequestBody.fromBytes(inputStream));
            return true;
        } catch (AwsServiceException | SdkClientException e) {
            log.error("Could not upload object: {} to BDR ", objectName, e);
        }
        return false;
    }

    /**
     * Get the S3Object MetaData of the requested data from BDR (Object store) & gets the eTag
     *
     * @param bucketName the name of the bucket the data is stored under
     * @param objectPath The file path.
     * @return String version of the etag
     */
    public String getETag(final String bucketName, String objectPath) {
        try {
            HeadObjectRequest objectAttributesRequest = HeadObjectRequest.builder()
                    .bucket(bucketName).key(objectPath).build();
            String eTag  = s3Client.headObject(objectAttributesRequest).eTag();
            if (eTag == null || eTag.isEmpty()) {
                log.error("Unable to get etag for '{}', Etag is null or empty - assigning a UUID", objectPath);
                return UUID.randomUUID().toString();
            }
            log.trace("Got etag: objectPath = '{}', eTag = '{}' ", objectPath, eTag);
            return eTag;
        } catch (RuntimeException e) {
            log.error("cannot get etag for {}, assigning a UUID", objectPath, e);
        }
        return UUID.randomUUID().toString();
    }
}