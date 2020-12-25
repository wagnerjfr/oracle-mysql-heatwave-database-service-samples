package com.oci.mds.manager;

import com.oci.mds.configuration.ProjectConfiguration;
import com.oci.mds.exception.WaitForStateException;
import com.oracle.bmc.model.BmcException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public abstract class AbstractManager<T, S> {

    public static final Integer DEFAULT_PORT = 3306;
    public static final Integer DEFAULT_XPORT = 33060;

    private static final long SLEEP_TIME = 10;
    private static final int EXCEPTION_LIMIT = 5;

    private static final String TIMEOUT_MSG = "Timed-out waiting for state %s, ID(s) %s";
    private static final String FAILED_MSG = "Faulty state %s found while waiting for state %s, ID(s) %s";
    private static final String WAIT_MESSAGE = "Waiting to become [{}] ... executed time {} seconds (timeout: {}s)\n{}";
    private static final String TO_STRING_FORMAT = "| %s [%s]: %s - '%s' %n";

    protected ProjectConfiguration config;

    private Collection<T> resources;

    AbstractManager(ProjectConfiguration config) {
        this.config = config;
    }

    abstract String getResourceId(T resource);

    abstract T getResource(String resourceId);

    abstract S getResourceLifeCycleState(T resource);

    abstract String getResourceDisplayName(T resource);

    abstract Class getResourceClass(T resource);

    abstract Collection<S> getFaultyStates();

    private void updateResourcesList(List<String> resourceIds) {
        resources = resourceIds.stream()
            .map(this::getResource)
            .collect(Collectors.toList());
    }

    private List<String> getIdListWithFaultyStates() {
        return resources.stream()
            .filter(resource -> getFaultyStates().contains(getResourceLifeCycleState(resource)))
            .map(this::getResourceId)
            .collect(Collectors.toList());
    }

    private Collection<String> geIdListNotInTargetState(S state) {
        return resources.stream()
            .filter(resource -> !getResourceLifeCycleState(resource).equals(state))
            .map(this::getResourceId)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return resources.stream()
            .map(resource -> printLine(getResourceClass(resource), getResourceLifeCycleState(resource), getResourceId(resource), getResourceDisplayName(resource)))
            .collect(Collectors.joining());
    }

    private String printLine(Class<?> name, S state, String resourceId, String displayName) {
        return  String.format(TO_STRING_FORMAT, name.getSimpleName(), state, resourceId, displayName);
    }

    /**
     * This method waits for a specified target state for the resources.
     * @param resourceIds  - resource ids
     * @param targetState - resource target state
     * @param timeoutInSeconds - timeout in seconds
     * @throws WaitForStateException - if the target state is not reached
     */
    public void waitForLifecycle(List<String> resourceIds, S targetState, Duration timeoutInSeconds) {
        final Instant startTime = Instant.now();
        final Instant timeout = startTime.plus(timeoutInSeconds);
        boolean hasFaultyState = false;
        int exceptionLimit = 0;

        do {
            try {
                updateResourcesList(resourceIds);
                exceptionLimit = 0;
            } catch (BmcException e) {
                log.error(String.format("%s - exception limit %d/%d", e.getMessage(), exceptionLimit, EXCEPTION_LIMIT));
                exceptionLimit++;
            }

            if (!getIdListWithFaultyStates().isEmpty()) {
                hasFaultyState = true;
                break;
            }

            long executedTime = startTime.until(Instant.now(), ChronoUnit.SECONDS);
            log.info(WAIT_MESSAGE, targetState, executedTime, timeoutInSeconds.getSeconds(), this);
            sleepUninterruptibly(SLEEP_TIME, SECONDS);

        } while (Instant.now().isBefore(timeout) && exceptionLimit < EXCEPTION_LIMIT && !geIdListNotInTargetState(targetState).isEmpty());

        if (!geIdListNotInTargetState(targetState).isEmpty()) {
            String message;

            if (hasFaultyState) {
                message = String.format(FAILED_MSG, getFaultyStates(), targetState, getIdListWithFaultyStates());
                throw new WaitForStateException(message);
            } else {
                message = String.format(TIMEOUT_MSG, targetState, geIdListNotInTargetState(targetState));
                throw new WaitForStateException(message, true);
            }
        }
    }

    /**
     * This method waits for a specified target state for an resource.
     * @param resourceId  - resource id
     * @param targetState - resource target state
     * @param timeoutInSeconds - timeout in seconds
     * @throws WaitForStateException - if the target state is not reached
     */
    public void waitForLifecycle(String resourceId, S targetState, Duration timeoutInSeconds) {
        waitForLifecycle(Collections.singletonList(resourceId), targetState, timeoutInSeconds);
    }
}
