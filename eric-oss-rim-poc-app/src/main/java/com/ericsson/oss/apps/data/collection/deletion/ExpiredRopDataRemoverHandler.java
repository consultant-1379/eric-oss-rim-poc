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
package com.ericsson.oss.apps.data.collection.deletion;

import static com.ericsson.oss.apps.data.collection.RopScheduler.ROP_MILLIS;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredRopDataRemoverHandler implements FeatureHandler<FeatureContext> {

    @Value("${app.data.pm.rop.retentionRops}")
    private long retentionRops;

    @Autowired
    private final Collection<ExpiredRopDataRemover> expiredRopDataRemovers;

    @Override
    public void handle(FeatureContext context) {
        expiredRopDataRemovers.forEach(expiredRopDataRemover ->
                expiredRopDataRemover.deletePmRop(context.getRopTimeStamp() - retentionRops * ROP_MILLIS));
    }

    @Override
    public int getPriority() {
        return 3;
    }

}
