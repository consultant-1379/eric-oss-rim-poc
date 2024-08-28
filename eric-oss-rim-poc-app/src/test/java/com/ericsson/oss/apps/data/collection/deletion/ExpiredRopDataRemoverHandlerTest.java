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
package com.ericsson.oss.apps.data.collection.deletion;

import static com.ericsson.oss.apps.data.collection.RopScheduler.ROP_MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.ericsson.oss.apps.data.collection.features.handlers.FeatureContext;
import com.ericsson.oss.apps.repositories.PmRopNrCellDuRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest(properties = {"app.data.pm.rop.retentionRops=1"})
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:pm/rop/pmropnrcelldu.sql")
})
class ExpiredRopDataRemoverHandlerTest {

    @Autowired
    ExpiredRopDataRemoverHandler expiredRopDataRemoverHandler;

    @Autowired
    PmRopNrCellDuRepo pmRopNrCellDuRepo;

    @Test
    void handle() {
        FeatureContext context = new FeatureContext(ROP_MILLIS * 4);
        expiredRopDataRemoverHandler.handle(context);
        Set<Long> expectedTimeStamps = Set.of(ROP_MILLIS*4L, ROP_MILLIS*3L);
        var retainedTimestamps = pmRopNrCellDuRepo.findAll().stream()
                .map(pmRopNrCellDu -> pmRopNrCellDu.getMoRopId().getRopTime()).collect(Collectors.toSet());
        assertEquals(expectedTimeStamps, retainedTimestamps);
    }

}