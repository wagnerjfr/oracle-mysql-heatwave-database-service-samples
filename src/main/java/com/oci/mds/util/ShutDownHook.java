package com.oci.mds.util;

import com.oci.mds.configuration.ProjectConfiguration;
import com.oci.mds.manager.DbBackupsManager;
import com.oci.mds.manager.DbSystemManager;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.mysql.model.Backup;
import com.oracle.bmc.mysql.model.DbSystem;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class ShutDownHook extends Thread {

    private static final long SLEEP_TIME_SEC = 10;

    private static Set<Managers> managersList = new HashSet<>();

    private ProjectConfiguration projectConfiguration;

    public ShutDownHook(ProjectConfiguration projectConfiguration) {
        this.projectConfiguration = projectConfiguration;
    }

    @Override
    public void run() {
        log.info("** Shutdown Hook called **");
        managersList.parallelStream().forEach(managers -> {
            cleanUpDbSystems(managers);
            cleanUpBackups(managers);
        });
    }

    public static synchronized void addDbSystemManager(Managers managers) {
        managersList.add(managers);
    }

    private void cleanUpDbSystems(Managers managers) {
        final DbSystemManager dbSystemManager = managers.getDbSystemManager();
        final DbBackupsManager dbBackupsManager = managers.getDbBackupsManager();

        // Getting just the DbSystem Ids in "Updating" state and with Backup attached
        List<String> dbSystemUpdatingIdsWithBackup = dbBackupsManager.getDbBackups().stream()
            .map(Backup::getDbSystemId)
            .filter(dbSystemId -> dbSystemManager.getLifecycleState(dbSystemId).equals(DbSystem.LifecycleState.Updating))
            .collect(Collectors.toList());

        if (!dbSystemUpdatingIdsWithBackup.isEmpty()) {
            long updatingTimeout = projectConfiguration.getUpdatingDbSystemTimeoutInSeconds();
            dbSystemUpdatingIdsWithBackup.forEach(id ->
                waitForAnyDifferentState(dbSystemManager, id, DbSystem.LifecycleState.Updating, updatingTimeout));
        }

        List<String> dbSystemIdList = getDbSystemNotDeleted(dbSystemManager);
        if (!dbSystemIdList.isEmpty()) {
            log.info("Cleaning up DbSystem(s): {}", dbSystemIdList);
            dbSystemManager.deleteDbSystem(dbSystemIdList);
        }
    }

    private void cleanUpBackups(Managers managers) {
        final DbSystemManager dbSystemManager = managers.getDbSystemManager();
        final DbBackupsManager dbBackupsManager = managers.getDbBackupsManager();

        if (dbBackupsManager.getDbBackupIds().isEmpty()) {
            return;
        }

        log.info("Cleaning up Backup(s): {} ", dbBackupsManager.getDbBackupIds());

        int maxAttempts = 30; // ~ 5min
        boolean deleteException = true;
        int attempt;
        for (attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                dbBackupsManager.deleteAllDbBackups();
                deleteException = false;
            } catch (BmcException e) {
                log.warn("==> Attempt [{}/{}] {}s interval {} \n", attempt, maxAttempts, SLEEP_TIME_SEC, e.getMessage());

                List<String> dbSystemIdList = getDbSystemNotDeleted(dbSystemManager);
                if (!dbSystemIdList.isEmpty()) {
                    log.warn("==> Deleting DbSystems: {}", dbSystemIdList);
                    dbSystemManager.deleteDbSystem(dbSystemIdList);
                }
            }
            if (!deleteException) {
                break;
            } else {
                sleepUninterruptibly(SLEEP_TIME_SEC, SECONDS);
            }
        }
        if (attempt == maxAttempts) {
            log.warn("Backup(s) '{}' could not be deleted. ", dbBackupsManager.getDbBackupIds());
        }
    }

    private void waitForAnyDifferentState(DbSystemManager dbSystemManager, String dbSystemId, DbSystem.LifecycleState state, long timeout) {
        final Instant waitStartTime = Instant.now();
        while (ChronoUnit.SECONDS.between(waitStartTime, Instant.now()) < timeout) {
            if (dbSystemManager.getLifecycleState(dbSystemId).equals(state)) {
                log.info("DbSystem '{}' state wasn't changed. Waiting for any different state from {}.", dbSystemId, state);
                sleepUninterruptibly(SLEEP_TIME_SEC, SECONDS);
            } else {
                break;
            }
        }
    }

    private List<String> getDbSystemNotDeleted(DbSystemManager dbSystemManager) {
        return dbSystemManager.getDbSystems().stream()
            .filter(dbSystem -> !dbSystem.getLifecycleState().equals(DbSystem.LifecycleState.Deleted) && !dbSystem.getLifecycleState().equals(DbSystem.LifecycleState.Deleting))
            .map(DbSystem::getId)
            .collect(Collectors.toList());
    }

    @Value
    @Builder
    public static class Managers {
        private DbSystemManager dbSystemManager;
        private DbBackupsManager dbBackupsManager;
    }
}
