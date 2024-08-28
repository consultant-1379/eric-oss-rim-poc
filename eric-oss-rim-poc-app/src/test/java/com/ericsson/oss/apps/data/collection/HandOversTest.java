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
package com.ericsson.oss.apps.data.collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandOversTest {

    @Test
    public void HandOvers_test() {
        HandOvers ho1 = new HandOvers("fdn1", 10, 100);
        assertEquals(10.0, ho1.getHoCoefficient());

        HandOvers ho2 = new HandOvers("fdn2", null, 100);
        assertEquals(0.0, ho2.getHoCoefficient());

        HandOvers ho3 = new HandOvers("fdn3", 10, null);
        assertEquals(0.0, ho3.getHoCoefficient());

        HandOvers ho4 = new HandOvers("fdn4", 10, 0);
        assertEquals(0.0, ho4.getHoCoefficient());

        assertEquals(ho1, ho1);
        assertNotNull(ho1);

        HandOvers ho5 = new HandOvers("fdn5", 10, 100);
        HandOvers ho5a = new HandOvers("fdn5", 10, 10000);
        HandOvers ho5b = new HandOvers("fdn5", 1000, 100);

        assertEquals(10.0, ho1.getHoCoefficient());
        assertEquals(ho5, ho5);
        assertNotEquals(ho5, ho5a);
        assertNotEquals(ho5, ho5b);

        assertTrue(ho5.hashCode() != 0);
    }
}