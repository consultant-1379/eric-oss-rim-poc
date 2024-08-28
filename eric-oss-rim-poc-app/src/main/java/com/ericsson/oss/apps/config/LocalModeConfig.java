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

import com.ericsson.oss.apps.model.GeoData;
import com.ericsson.oss.apps.repositories.GeoDataRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Slf4j
@Aspect
@Configuration
@ConditionalOnProperty(prefix = "app", name = "local", havingValue = "true")
public class LocalModeConfig {

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private GeoDataRepo geoDataRepo;

    @Pointcut("execution(public * com.ericsson.oss.apps.api.ncmp.NetworkCmProxyApi.*(..))")
    private void networkCmProxyApi(){ throw new UnsupportedOperationException(); }

    @Pointcut("execution(public * com.ericsson.oss.apps.client.CtsClient.getNrCellGeoData(..))")
    private void getGeoData(){ throw new UnsupportedOperationException(); }

    @Pointcut("execution(public * com.ericsson.oss.apps.client.CtsClient.getGNBCmHandleByCGI(..))")
    private void getGNBCmHandleByCGI(){ throw new UnsupportedOperationException(); }

    @Pointcut("execution(public * com.ericsson.oss.apps.data.collection.features.handlers.ConfigChangeImplementor.*(..))")
    private void implementChange(){ throw new UnsupportedOperationException(); }


    @Around("networkCmProxyApi()")
    public JsonNode emptyNodeResponse(ProceedingJoinPoint joinPoint) {
        return mapper.createObjectNode();
    }

    @Around("getGeoData()")
    public Optional<GeoData> getFromDB(ProceedingJoinPoint joinPoint) {
        Object[] methodArguments = joinPoint.getArgs();
        String fdn = (String) methodArguments[0];
        return geoDataRepo.findById(fdn);
    }

    @Around("getGNBCmHandleByCGI()")
    public Optional<String> getNoCmHandle(ProceedingJoinPoint joinPoint) {
        return Optional.empty();
    }

    @Around("implementChange()")
    public boolean sendNoPatch(ProceedingJoinPoint joinPoint) {
        return true;
    }
}
