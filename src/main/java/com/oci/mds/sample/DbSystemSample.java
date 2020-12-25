package com.oci.mds.sample;

import com.oci.mds.configuration.ProjectConfiguration;
import com.oci.mds.exception.WaitForStateException;
import com.oci.mds.manager.DbSystemDetailsSetup;

import com.oracle.bmc.mysql.model.CreateDbSystemDetails;
import com.oracle.bmc.mysql.model.DbSystem;
import com.oracle.bmc.mysql.model.InnoDbShutdownMode;
import com.oracle.bmc.mysql.responses.CreateDbSystemResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DbSystemSample extends BaseSample {

    private String dbSystemId;

    public DbSystemSample(ProjectConfiguration config) {
        super(config);
    }

    public void run() {
        create();
        stop();
        start();
        delete();
    }

    private void create() {
        String displayName = "OCI-MDS.DbSystemSample" + "." + System.currentTimeMillis();
        DbSystemDetailsSetup dbSystemDetailsSetup = DbSystemDetailsSetup.builder(config, displayName).build();
        CreateDbSystemDetails createDetails = dbSystemDetailsSetup.createDbSystemDetails();
        log.info("Creating DbSystem");

        CreateDbSystemResponse createResponse = dbSystemManager.createDbSystem(createDetails);

        DbSystem dbSystem = createResponse.getDbSystem();
        dbSystemId = dbSystem.getId();

        try {
            dbSystemManager.waitForLifecycle(dbSystemId, DbSystem.LifecycleState.Active, dbSystemCreateTimeout);

        } catch (WaitForStateException e) {
            String errorMsg = "Exception while creating DbSystem " + dbSystemId;
            dbSystemId = null;
            log.error(errorMsg, e);
        }
    }

    private void stop() {
        if (dbSystemId == null) {
            log.warn("DbSystemId is null, skipping the stop method.");
        }

        assert dbSystemManager.getDbSystem(dbSystemId).getLifecycleState() == DbSystem.LifecycleState.Active;

        try {
            log.info("Stopping DbSystem");
            dbSystemManager.stopDbSystemAndWaitForState(dbSystemId, InnoDbShutdownMode.Fast);
        } catch (WaitForStateException e) {
            String errorMsg = "Exception while stopping DbSystem " + dbSystemId;
            log.error(errorMsg, e);
        }
    }

    private void start() {
        if (dbSystemId == null) {
            log.warn("DbSystemId is null, skipping the start method.");
        }

        assert dbSystemManager.getDbSystem(dbSystemId).getLifecycleState() == DbSystem.LifecycleState.Inactive;

        try {
            log.info("Starting DbSystem");
            dbSystemManager.startDbSystemAndWaitForState(dbSystemId);
        } catch (WaitForStateException e) {
            String errorMsg = "Exception while starting DbSystem " + dbSystemId;
            log.error(errorMsg, e);
        }
    }

    private void delete() {
        if (dbSystemId == null) {
            log.warn("DbSystemId is null, skipping the delete method.");
        }

        try {
            log.info("Deleting DbSystem");
            dbSystemManager.deleteDbSystemAndWaitForState(dbSystemId);
        } catch (WaitForStateException e) {
            String errorMsg = "Exception while deleting DbSystem " + dbSystemId;
            log.error(errorMsg, e);
        }
    }
}
