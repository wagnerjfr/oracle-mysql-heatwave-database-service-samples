package com.oci.mds.sample;

import com.oci.mds.configuration.ProjectConfiguration;
import com.oci.mds.manager.DbBackupsManager;
import com.oci.mds.manager.DbSystemManager;
import com.oci.mds.util.OciUtils;
import com.oci.mds.util.ShutDownHook;

import java.time.Duration;

public abstract class BaseSample {

    protected final ProjectConfiguration config;

    DbSystemManager dbSystemManager;
    DbBackupsManager dbBackupsManager;

    private String mysqlClientEndpoint;

    Duration dbSystemCreateTimeout;
    Duration dbSystemDeleteTimeout;
    Duration dbSystemUpdatingTimeout;
    Duration backupCreateTimeout;
    Duration backupDeleteTimeout;

    BaseSample(ProjectConfiguration config) {
        this.config = config;
        setup();
    }

    private void setup() {
        dbSystemManager = new DbSystemManager(config);
        dbBackupsManager = new DbBackupsManager(config);

        ShutDownHook.addDbSystemManager(ShutDownHook.Managers.builder()
            .dbSystemManager(dbSystemManager)
            .dbBackupsManager(dbBackupsManager)
            .build());

        OciUtils ociUtils = new OciUtils(config);

        // Set availability Domain based on configuration.
        config.setAvailabilityDomain(
            config.getLogicalADName() != null
                ? config.getLogicalADName() : ociUtils.getConfiguredAvailabilityDomain());

        mysqlClientEndpoint = config.getMysqlClientEndpoint();

        if (!mysqlClientEndpoint.isEmpty()) {
            config.getDbBackupsClient().setEndpoint(mysqlClientEndpoint);
            config.getDbSystemClient().setEndpoint(mysqlClientEndpoint);
            config.getMysqlaasClient().setEndpoint(mysqlClientEndpoint);
        }

        initTimeouts();
    }

    private void initTimeouts() {
        dbSystemCreateTimeout = Duration.ofSeconds(config.getCreateDbSystemTimeoutInSeconds());
        dbSystemDeleteTimeout = Duration.ofSeconds(config.getDeleteDbSystemTimeoutInSeconds());
        dbSystemUpdatingTimeout = Duration.ofSeconds(config.getUpdatingDbSystemTimeoutInSeconds());

        backupCreateTimeout = Duration.ofSeconds(config.getCreateBackupTimeoutInSeconds());
        backupDeleteTimeout = Duration.ofSeconds(config.getDeleteBackupTimeoutInSeconds());
    }
}
