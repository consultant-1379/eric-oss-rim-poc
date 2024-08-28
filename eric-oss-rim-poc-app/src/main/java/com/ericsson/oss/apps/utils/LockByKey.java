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

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class to provide lock-by-key
 * we have a number of cases where we don't want to synchronize data from NCMP
 * for the same node twice, but we still want concurrent access. This can be solved
 * by sorting/grouping the requests in advance, however that makes the code more complex
 * and in certain cases the code that gets parallelized does not know which node needs synchronization
 * (e.g. relation resolution).
 * Given that the time to fetch data from NCMP dwarfs any DB lookup by three orders of magnitude
 * it's ok to block on the same node, even if it's cached locally.
 * It is imperative that locks are release in a finally block.
 * If this class is used a part of a long-lived object (e.g. a component) it should be replaced when the
 * processing requiring the locks is over to avoid potential memory leaks (locks are never deleted from the map).
 *
 * Code inspired by https://www.baeldung.com/java-acquire-lock-by-key
 */
@Slf4j
public class LockByKey<T> {

    private final ConcurrentHashMap<T, Lock> locks = new ConcurrentHashMap<>();

    public void lock(T key) {
        log.debug("attempting to lock {}", key);
        locks.computeIfAbsent(key, x -> new ReentrantLock()).lock();
        log.debug("locked {}", key);
    }

    public void unlock(T key) {
        log.debug("unlocking {}", key);
        locks.get(key).unlock();
        log.debug("unlocked {}", key);
    }

}