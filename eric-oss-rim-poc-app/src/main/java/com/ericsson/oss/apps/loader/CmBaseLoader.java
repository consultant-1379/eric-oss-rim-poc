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
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

@Slf4j
@RequiredArgsConstructor
public abstract class CmBaseLoader<T, U> {
    private final EntityManager entityManager;
    private final BdrClient bdrClient;
    private final String bucketName;
    private final Class<T> tClass;
    private final String filename;

    @Transactional
    public void load() {
        try (GZIPInputStream inputStream = new GZIPInputStream(bdrClient.getObjectInputStream(bucketName, filename), 131072);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);) {
            CsvToBean<T> dataBeans = new CsvToBeanBuilder<T>(reader).withType(tClass).withSeparator(',').withIgnoreLeadingWhiteSpace(true).build();
            dataBeans.stream().map(this::transform).filter(Objects::nonNull).forEach(entityManager::merge);
        } catch (RuntimeException | IOException runtimeException) {
            log.info("Error parsing cm data, cannot parse'{}'", filename, runtimeException);
        }
    }

    protected abstract U transform(T dataBean);
}
