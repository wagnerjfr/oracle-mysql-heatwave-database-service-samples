package com.oci.mds.sample;

import com.oci.mds.configuration.ProjectConfiguration;
import com.oci.mds.exception.WaitForStateException;
import com.oci.mds.manager.DbSystemDetailsSetup;

import com.oracle.bmc.mysql.model.Backup;
import com.oracle.bmc.mysql.model.CreateBackupDetails;
import com.oracle.bmc.mysql.model.CreateDbSystemDetails;
import com.oracle.bmc.mysql.model.DbSystem;
import com.oracle.bmc.mysql.responses.CreateBackupResponse;
import com.oracle.bmc.mysql.responses.CreateDbSystemResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackupSample extends BaseSample {

    private String dbSystemId;
    private String backupId;

    public BackupSample(ProjectConfiguration config) {
        super(config);
    }

    public void run() {
        createDbSystem();
        createBackup();
        //restoreDbSystem();
        delete();
    }

    private void createDbSystem() {
        String displayName = "OCI-MDS.BackupSample" + "." + System.currentTimeMillis() + ".DbSystem";
        DbSystemDetailsSetup dbSystemDetailsSetup = DbSystemDetailsSetup.builder(config, displayName).build();
        CreateDbSystemDetails createDetails = dbSystemDetailsSetup.createDbSystemDetails();
        log.info("Creating DbSystem");

        try {
            CreateDbSystemResponse createDbSystemResponse = dbSystemManager.createDbSystemAndWaitForState(createDetails);

            DbSystem dbSystem = createDbSystemResponse.getDbSystem();
            dbSystemId = dbSystem.getId();

        } catch (WaitForStateException e) {
            String errorMsg = "Exception while creating DbSystem " + dbSystemId;
            dbSystemId = null;
            log.error(errorMsg, e);
        }
    }

    private void createBackup() {
        if (dbSystemId == null) {
            log.warn("DbSystemId is null, skipping the restore method.");
        }

        String displayName = "OCI-MDS.BackupSample" + "." + System.currentTimeMillis() + ".Backup";

        final CreateBackupDetails createBackupDetails = CreateBackupDetails.builder()
            .backupType(CreateBackupDetails.BackupType.Full)
            .displayName(displayName)
            .description("Canary-Full Backup")
            .retentionInDays(1)
            .dbSystemId(dbSystemId)
            .build();

        try {
            log.info("Creating Backup");
            final CreateBackupResponse backupDbSysResp = dbBackupsManager.backupDbSystemAndWaitForState(createBackupDetails);
            Backup backup = backupDbSysResp.getBackup();
            backupId = backup.getId();

        } catch (WaitForStateException e) {
            String errorMsg = "Exception while creating backup";
            log.error(errorMsg, e);
        }
    }

    // TODO: Restore process
    private void restoreDbSystem() {

    }

    private void delete() {
        if (dbSystemId == null) {
            log.warn("DbSystemId is null, skipping the delete method.");
        } else {
            try {
                log.info("Deleting DbSystem");
                dbSystemManager.deleteDbSystemAndWaitForState(dbSystemId);
            } catch (WaitForStateException e) {
                String errorMsg = "Exception while deleting DbSystem " + dbSystemId;
                log.error(errorMsg, e);
            }
        }

        if (backupId == null) {
            log.warn("BackupId is null, skipping the delete method.");
        } else {
            try {
                log.info("Deleting Backup");
                dbBackupsManager.deleteDbBackupAndWaitForState(backupId);
            } catch (WaitForStateException e) {
                String errorMsg = "Exception while deleting Backup " + backupId;
                log.error(errorMsg, e);
            }
        }
    }
}
