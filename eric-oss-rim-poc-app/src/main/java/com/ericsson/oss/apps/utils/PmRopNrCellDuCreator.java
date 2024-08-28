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
package com.ericsson.oss.apps.utils;

import static com.ericsson.oss.apps.utils.PmConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.data.collection.MoRopId;
import com.ericsson.oss.apps.data.collection.pmrop.counters.PmRopNRCellDU;
import com.ericsson.oss.apps.kafka.PmCounterType;

import NR.RAN.PM_COUNTERS.NRCellDU_GNBDU_1;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class PmRopNrCellDuCreator.
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@Component
public class PmRopNrCellDuCreator {


    /**
     * New Instance.
     *
     * @return the pm rop nr cell du creator
     */
    public static PmRopNrCellDuCreator of() {
        return new PmRopNrCellDuCreator();
    }

    /**
     * Creates the new PmRopNRCellDU object from the received NRCellDU_GNBDU_1 counter information in the Consumer Record 'value'.
     * Wrapper for method below with usePreCalculatedAvgDeltaIpN = false.
     *
     * @param pmCounterAvro
     *     the NRCellDU_GNBDU_1 PM Counter information.
     * @param ropEndTimeEpoch
     *     the rop end time in epoch format.
     *
     * @return a new PmRopNRCellDU object.
     */
    public PmRopNRCellDU createNewPmRopNRCellDU(NRCellDU_GNBDU_1 pmCounterAvro, long ropEndTimeEpoch) {
        return createNewPmRopNRCellDU(pmCounterAvro, ropEndTimeEpoch, false);
    }

    /**
     * Creates the new PmRopNRCellDU object from the received NRCellDU_GNBDU_1 counter information in Consumer Record 'value'.
     *
     * @param pmCounterAvro
     *     the NRCellDU_GNBDU_1 PM Counter information.
     * @param ropEndTimeEpoch
     *     the rop end time in epoch format.
     * @param usePreCalculatedAvgDeltaIpN
     *     usePreCalculatedAvgDeltaIpN, true or false.
     *
     * @return a new PmRopNRCellDU object.
     */
    public PmRopNRCellDU createNewPmRopNRCellDU(NRCellDU_GNBDU_1 pmCounterAvro, long ropEndTimeEpoch, boolean usePreCalculatedAvgDeltaIpN) {
        MoRopId moRopId = new MoRopId(pmCounterAvro.getNodeFDN().toString(), ropEndTimeEpoch);
        PmRopNRCellDU pmRopNRCellDU = new PmRopNRCellDU();
        pmRopNRCellDU.setMoRopId(moRopId);

        NR.RAN.PM_COUNTERS.pmCounters nrCellDuPmCtrs = pmCounterAvro.getPmCounters();

        Field[] pmRopNeCellDUFields = pmRopNRCellDU.getClass().getDeclaredFields();
        Arrays.asList(pmRopNeCellDUFields).forEach(pmRopNeCellDUField -> {
            if (!pmRopNeCellDUField.getName().startsWith("pm") && !pmRopNeCellDUField.getName().equals("avgDeltaIpNPreCalculated")) {
                return;
            }
            log.trace("PmRopNrCellDuCreator: Processing PmRopNrCellDU Counter {}", pmRopNeCellDUField.getName());

            try {
                String setMethodName = "set" + pmRopNeCellDUField.getName().substring(0, 1).toUpperCase(Locale.ENGLISH) + pmRopNeCellDUField.getName().substring(1);
                Field f = pmRopNRCellDU.getClass().getDeclaredField(pmRopNeCellDUField.getName());
                Method m = pmRopNRCellDU.getClass().getDeclaredMethod(setMethodName, f.getType());

                Object pmCounter = PropertyUtils.getProperty(nrCellDuPmCtrs, pmRopNeCellDUField.getName());
                processPmCounter(pmCounterAvro, pmRopNRCellDU, pmRopNeCellDUField, f, m, pmCounter);

            } catch (Exception e) {
                String message = "PmRopNrCellDuCreator: Error Processing PM Counter Avro; DROPPING COUNTER,(Deserialization Error; "
                    + "Invalid counter name/type/values; cannot invoke setter method on PmRopNRCellDU" + pmRopNeCellDUField.getName()
                    + ") for record with fdn "
                    + pmCounterAvro.getNodeFDN() + ", pmCounterAvro = " + pmCounterAvro + "\n" + "Exception :" + e;
                log.error(message);
            }
        });
        pmRopNRCellDU.setAvgDeltaIpNPreCalculated(resetLongToDouble("avgDeltaIpNPreCalculated", pmRopNRCellDU.getAvgDeltaIpNPreCalculated()));
        if (usePreCalculatedAvgDeltaIpN) {
            pmRopNRCellDU.setAvgDeltaIpN(pmRopNRCellDU.getAvgDeltaIpNPreCalculated());
            pmRopNRCellDU.setUsePreCalculatedInCsv(true);
        }
        return pmRopNRCellDU;
    }

    private void processPmCounter(NRCellDU_GNBDU_1 pmCounterAvro, PmRopNRCellDU pmRopNRCellDU, Field pmRopNeCellDUField, Field f, Method m,
                                  Object pmCounter)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if(pmCounter != null) {
            PmCounterObjects pmObjects = new PmCounterObjects(pmCounterAvro.getNodeFDN().toString(), pmRopNeCellDUField.getName(),pmCounter);
            log.trace("PmRopNrCellDuCreator: Processing {} ", pmObjects);
            if (!pmObjects.isValuePresent()) {
                getDefaultValue(pmRopNRCellDU, f, m);
                return;
            }
            if (pmObjects.isValidCounter()) {
                if(pmObjects.getPmCounterType() == PmCounterType.SINGLE) {
                    processSingleCounterType(pmRopNRCellDU, pmRopNeCellDUField, pmObjects.getCounterValueObject(), f, m);
                    return;
                }
                if(pmObjects.getPmCounterType() == PmCounterType.PDF || pmObjects.getPmCounterType() == PmCounterType.COMPRESSED_PDF) {
                    processPdfCounterType(pmRopNRCellDU, pmObjects.getCounterValueObject(), m);
                    return;
                }
            }
        }
        log.warn("PmRopNrCellDuCreator: Invalid PmCounter Avro values {} received for processing for counter '{}' from fdn '{}', cannot process this counter, SETTING DEFAULT VALUES",
            pmCounter, pmRopNeCellDUField.getName(), pmCounterAvro.getNodeFDN().toString() );
        getDefaultValue(pmRopNRCellDU, f, m);
    }

    private void processPdfCounterType(PmRopNRCellDU pmRopNRCellDU, Object counterValueObject, Method m)
        throws IllegalAccessException, InvocationTargetException {
        // Issue with Kafka producing a GenericData Array type.
        ArrayList<Long> counterValuesLongList = (ArrayList<Long>) ((List<?>) counterValueObject).stream()
            .map(obj -> Long.valueOf(obj.toString()))
            .collect(Collectors.toList());
        m.invoke(pmRopNRCellDU, counterValuesLongList);
    }

    private void processSingleCounterType(PmRopNRCellDU pmRopNRCellDU, Field pmRopNeCellDUField, Object counterValueObject, Field f,
                                          Method m)
        throws IllegalAccessException, InvocationTargetException {
        // Check for counter is of correct type have been moved to isSingleCounter()
        Long counterValueLong = (long) counterValueObject;

        if (pmRopNeCellDUField.getType().equals(Double.class)) {
            Double counterValueDouble = counterValueLong.doubleValue();

            // There are no counters of type double; but pmRopNRCellDU has a number of them 'converted' to double
            // Presumption is that any counter with Long.Min_Value is a Double.NaN
            // Should only be one affected counter for "avgDeltaIpNPreCalculated" with Long.Min_Value explicitly set. But keeping the logic
            // in case parsers send in a counter with Long.Min_Value set in future.
            if (counterValueLong == Long.MIN_VALUE) {
                if (!f.getName().equals("avgDeltaIpNPreCalculated")) {
                    log.warn("PmRopNrCellDuCreator: Resetting {} from Long.Min_Value '{}' to Double.Nan '{}'", f.getName(), Long.MIN_VALUE,
                        Double.NaN);
                }
                counterValueDouble = Double.NaN;
            }
            m.invoke(pmRopNRCellDU, counterValueDouble);
        } else {
            m.invoke(pmRopNRCellDU, counterValueLong);
        }
    }

    private Double resetLongToDouble(String parameter, Double value) {
        Double valueBefore = value;
        value = (value != null && !Double.isNaN(value) && (Math.round(value)) != Long.MIN_VALUE) ? (value / PmConstants.DOUBLE_TO_LONG_CONSTANT)
            : Double.NaN;
        log.trace("PmRopNrCellDuCreator: Resetting {} from '{}' to '{}'", parameter, valueBefore, value);
        return value;
    }

    private void getDefaultValue(PmRopNRCellDU pmRopNRCellDU, Field f, Method m) throws IllegalAccessException, InvocationTargetException {
        if (f.getType().equals(ArrayList.class)) {
            m.invoke(pmRopNRCellDU, new ArrayList<>());
        } else {
            m.invoke(pmRopNRCellDU, getDefaultValue(f.getType()));
        }
    }

    private Number getDefaultValue(Class<?> class1) {
        if (class1.equals(Double.class)) {
            return Double.NaN;
        } else if (class1.equals(Long.class)) {
            return Long.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }


    /**
     * Holder class for PM Counter info read from PM Counter Avro.
     */
    @ToString
    @Getter
    class PmCounterObjects {
        private final String nodeName;
        private final String counterName;
        private final Object counterTypeObject;
        private final Object counterValueObject;
        private final Object isValuePresentObject;
        private PmCounterType pmCounterType = PmCounterType.NONE;

        /**
         * Instantiates a new pm counter objects.
         *
         * @param nodeName
         *     the node name
         * @param counterName
         *     the counter name
         * @param pmCounter
         *     the pm counter
         *
         * @throws IllegalAccessException
         *     the illegal access exception
         * @throws InvocationTargetException
         *     the invocation target exception
         * @throws NoSuchMethodException
         *     the no such method exception
         */
        PmCounterObjects(String nodeName, String counterName, Object pmCounter) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException{
            this.counterTypeObject = PropertyUtils.getProperty(pmCounter, COUNTER_TYPE);
            this.isValuePresentObject = PropertyUtils.getProperty(pmCounter, IS_VALUE_PRESENT);
            this.counterValueObject = PropertyUtils.getProperty(pmCounter, COUNTER_VALUE);
            this.nodeName = nodeName;
            this.counterName = counterName;
        }

        /**
         * Checks if counter has a value present.
         *
         * @return true, if is isValuePresentObject is set.
         */
        boolean isValuePresent() {
            return (boolean) this.isValuePresentObject;
        }

        /**
         * Checks if counter is valid.
         *
         * @return true, if is valid counter
         */
        boolean isValidCounter() {
            if (this.counterValueObject == null || this.counterTypeObject == null) {
                log.error("PmRopNrCellDuCreator : Error Processing PM Counter Avro; NULL Counter Value or Type, for counter {} from fdn {}; isValuePresent is '{}' , PmCounter = {} ", (boolean) this.isValuePresentObject, counterName, nodeName, this);
                return false;
            }
            if (this.counterTypeObject.toString().equalsIgnoreCase(COUNTER_TYPE_SINGLE) && counterValueObject instanceof Long) {
                pmCounterType = PmCounterType.SINGLE;
                return true;
            }

            if (this.counterTypeObject.toString().equalsIgnoreCase(COUNTER_TYPE_PDF) && (this.counterValueObject instanceof List)) {
                pmCounterType = PmCounterType.PDF;
                return true;
            }
            if (this.counterTypeObject.toString().equalsIgnoreCase(COUNTER_TYPE_COMPRESSED_PDF) && (this.counterValueObject instanceof List)) {
                pmCounterType = PmCounterType.COMPRESSED_PDF;
                return true;
            }

            log.error("PmRopNrCellDuCreator : Error Processing PM Counter Avro; Unsupported Counter Type: CounterName = {}, counterType is {}, counterValue {}, "
                + "counterValue is an instance of '{}' isValuePresent is '{}' from fdn {}; ", counterName, this.counterTypeObject
                    .toString(), this.counterValueObject
                        .toString(), this.counterValueObject.getClass().getName(), (boolean) this.isValuePresentObject, nodeName);
            return false;
        }
    }
}
