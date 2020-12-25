package com.oci.mds.manager;

import com.oci.mds.configuration.ProjectConfiguration;
import com.oci.mds.exception.ExecutionException;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.mysql.DbBackupsClient;
import com.oracle.bmc.mysql.model.Backup;
import com.oracle.bmc.mysql.model.Backup.LifecycleState;
import com.oracle.bmc.mysql.model.BackupSummary;
import com.oracle.bmc.mysql.model.CreateBackupDetails;
import com.oracle.bmc.mysql.model.UpdateBackupDetails;
import com.oracle.bmc.mysql.requests.CreateBackupRequest;
import com.oracle.bmc.mysql.requests.DeleteBackupRequest;
import com.oracle.bmc.mysql.requests.GetBackupRequest;
import com.oracle.bmc.mysql.requests.ListBackupsRequest;
import com.oracle.bmc.mysql.requests.UpdateBackupRequest;
import com.oracle.bmc.mysql.responses.CreateBackupResponse;
import com.oracle.bmc.mysql.responses.DeleteBackupResponse;
import com.oracle.bmc.mysql.responses.GetBackupResponse;
import com.oracle.bmc.mysql.responses.ListBackupsResponse;
import com.oracle.bmc.mysql.responses.UpdateBackupResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class DbBackupsManager extends AbstractManager<Backup, LifecycleState> {

    private final DbBackupsClient dbBackupsClient;
    private final Duration backupCreateTimeout;
    private final Duration backupDeleteTimeout;
    private List<String> dbBackupIds;

    public DbBackupsManager(ProjectConfiguration config) {
        super(config);
        dbBackupsClient = config.getDbBackupsClient();
        dbBackupIds = new ArrayList<>();
        backupCreateTimeout = Duration.ofSeconds(config.getCreateBackupTimeoutInSeconds());
        backupDeleteTimeout = Duration.ofSeconds(config.getDeleteBackupTimeoutInSeconds());
    }

    /* CREATE Methods */

    public CreateBackupResponse backupDbSystem(CreateBackupDetails createBackupDetails) {
        final CreateBackupRequest backupDbSysRequest =
            CreateBackupRequest.builder()
                .createBackupDetails(createBackupDetails)
                .build();

        CreateBackupResponse createBackupResponse = dbBackupsClient.createBackup(backupDbSysRequest);
        dbBackupIds.add(createBackupResponse.getBackup().getId());
        return createBackupResponse;
    }

    public List<CreateBackupResponse> backupDbSystem(List<CreateBackupDetails> createBackupDetailsList) {
        List<CreateBackupRequest> requestList = createBackupDetailsList.stream()
            .map(bkp -> CreateBackupRequest.builder().createBackupDetails(bkp).build())
            .collect(Collectors.toList());

        List<CreateBackupResponse> responseList = new ArrayList<>();
        CreateBackupRequest createBackupRequest = null;
        boolean createException = false;
        try {
            for (CreateBackupRequest req : requestList) {
                createBackupRequest = req;
                responseList.add(dbBackupsClient.createBackup(req));
            }
        } catch (BmcException e) {
            CreateBackupDetails details = createBackupRequest == null ? null : createBackupRequest.getCreateBackupDetails();
            log.error("Can't create Backup with this request: \n{} \n{}", details, e);
            createException = true;
        }

        dbBackupIds.addAll(responseList.stream()
            .map(CreateBackupResponse::getBackup)
            .map(Backup::getId)
            .collect(Collectors.toList()));

        // The manager will delete all the backups it started creating because one of the requests had an exception
        if (createException) {
            deleteAllDbBackups();
            responseList = Collections.emptyList();
        }

        return responseList;
    }

    public CreateBackupResponse backupDbSystemAndWaitForState(CreateBackupDetails createBackupDetails) {
        List<CreateBackupResponse> createBackupResponseList = backupDbSystemAndWaitForState(Collections.singletonList(createBackupDetails));
        return createBackupResponseList.get(0);
    }

    public List<CreateBackupResponse> backupDbSystemAndWaitForState(List<CreateBackupDetails> createBackupDetailsList) {
        List<CreateBackupResponse> createBackupResponses = backupDbSystem(createBackupDetailsList);

        List<String> backupIdList = createBackupResponses.stream()
            .map(CreateBackupResponse::getBackup)
            .map(Backup::getId).collect(Collectors.toList());

        waitForLifecycle(backupIdList, LifecycleState.Active, backupCreateTimeout);

        return createBackupResponses;
    }

    /* DELETE Methods */

    public DeleteBackupResponse deleteDbBackup(DeleteBackupRequest deleteBackupRequest) {
        return dbBackupsClient.deleteBackup(deleteBackupRequest);
    }

    public DeleteBackupResponse deleteDbBackup(String dbBackupId) {
        DeleteBackupRequest deleteBackupRequest =
            DeleteBackupRequest.builder()
            .backupId(dbBackupId)
            .build();
        return deleteDbBackup(deleteBackupRequest);
    }

    public List<DeleteBackupResponse> deleteDbBackup(List<String> dbBackupIdList) {
        return dbBackupIdList.stream()
            .map(this::deleteDbBackup)
            .collect(Collectors.toList());
    }

    public void deleteDbBackupAndWaitForState(String dbBackupId) {
        deleteDbBackupAndWaitForState(Collections.singletonList(dbBackupId));
    }

    public void deleteDbBackupAndWaitForState(List<String> dbBackupIdList) {
        deleteDbBackup(dbBackupIdList);
        waitForLifecycle(dbBackupIdList, LifecycleState.Deleted, backupDeleteTimeout);
    }

    public List<DeleteBackupResponse> deleteAllDbBackups() {
        return deleteDbBackup(dbBackupIds);
    }

    public void deleteAllDbBackupsAndWaitForState() {
        deleteDbBackupAndWaitForState(dbBackupIds);
    }


    /* GET Methods */

    public GetBackupResponse getDbBackup(GetBackupRequest getBackupRequest) {
        return dbBackupsClient.getBackup(getBackupRequest);
    }

    public Backup getDbBackup(String backupId) {
        GetBackupRequest backupRequest = GetBackupRequest.builder().backupId(backupId).build();
        return getDbBackup(backupRequest).getBackup();
    }

    public LifecycleState getLifecycleState(String backupId) {
        return getDbBackup(backupId).getLifecycleState();
    }

    public List<Backup> getDbBackups() {
        return dbBackupIds.stream()
            .map(this::getDbBackup)
            .collect(Collectors.toList());
    }

    public List<String> getDbBackupIds() {
        return new ArrayList<>(dbBackupIds);
    }

    /* LIST methods */

    public List<BackupSummary> listBackups(String compartmentId, int limit) {
        final ListBackupsRequest listBackupsRequest = ListBackupsRequest.builder()
            .compartmentId(compartmentId)
            .limit(limit).build();
        return listBackups(listBackupsRequest).getItems();
    }

    public List<BackupSummary> listBackups(String compartmentId, LifecycleState state) {
        final ListBackupsRequest listBackupsRequest = ListBackupsRequest.builder()
            .compartmentId(compartmentId)
            .lifecycleState(state).build();
        return listBackups(listBackupsRequest).getItems();
    }

    public List<BackupSummary> listBackups(String compartmentId, String backupId) {
        final ListBackupsRequest listBackupsRequest = ListBackupsRequest.builder()
            .compartmentId(compartmentId)
            .backupId(backupId).build();
        return listBackups(listBackupsRequest).getItems();
    }

    public List<BackupSummary> listBackups(String compartmentId, String backupId, LifecycleState state) {
        final ListBackupsRequest listBackupsRequest = ListBackupsRequest.builder()
            .compartmentId(compartmentId)
            .backupId(backupId)
            .lifecycleState(state).build();
        return listBackups(listBackupsRequest).getItems();
    }

    public ListBackupsResponse listBackups(ListBackupsRequest listBackupsRequest) {
        return dbBackupsClient.listBackups(listBackupsRequest);
    }

    public boolean isBackupInCompartmentList(String compartmentId, String backupId) {
        return listBackups(compartmentId, backupId).stream()
            .anyMatch(bkp -> bkp.getId().equals(backupId));
    }

    public boolean isBackupInCompartmentList(String compartmentId, String backupId, LifecycleState state) {
        return listBackups(compartmentId, backupId, state).stream()
            .anyMatch(bkp -> bkp.getId().equals(backupId));
    }

    public List<BackupSummary> listBackupsForDbSystem(String compartmentId, String dbSystemId) {
        ListBackupsRequest listBackupsRequest = ListBackupsRequest
            .builder()
            .compartmentId(compartmentId)
            .dbSystemId(dbSystemId)
            .build();
        return listBackups(listBackupsRequest).getItems();
    }

    /* UPDATE methods */

    public UpdateBackupResponse updateBackup(String backupId, UpdateBackupDetails updateBackupDetails) {
        return dbBackupsClient.updateBackup(UpdateBackupRequest.builder()
            .backupId(backupId)
            .updateBackupDetails(updateBackupDetails)
            .build());
    }

    /* WAIT Methods */

    public List<BackupSummary> waitForListBackupSize(ListBackupsRequest listBackupsRequest, int expectedListSize, Duration timeoutInSeconds) {
        List<BackupSummary> listBackups = Collections.emptyList();
        final long iterationSleepTime = 6;

        Instant waitTill = Instant.now().plus(timeoutInSeconds);
        while (Instant.now().isBefore(waitTill)) {
            listBackups = dbBackupsClient.listBackups(listBackupsRequest).getItems();
            if (listBackups.size() == expectedListSize) {
                break;
            }
            log.info("Waiting for {} backup(s).", expectedListSize);
            sleepUninterruptibly(iterationSleepTime, SECONDS);
        }

        if (expectedListSize != listBackups.size()) {
            String msgException = String.format("Number of items in the list [%d] doesn't match with the expected size [%d]", listBackups.size(), expectedListSize);
            throw new ExecutionException(msgException);
        }
        return listBackups;
    }

    /* Overridden helper methods to be used in 'waitForLifecycle' methods from super class */

    @Override
    String getResourceId(Backup resource) {
        return resource.getId();
    }

    @Override
    Backup getResource(String resourceId) {
        return getDbBackup(resourceId);
    }

    @Override
    LifecycleState getResourceLifeCycleState(Backup resource) {
        return resource.getLifecycleState();
    }

    @Override
    String getResourceDisplayName(Backup resource) {
        return resource.getDisplayName();
    }

    @Override
    Class getResourceClass(Backup resource) {
        return resource.getClass();
    }

    @Override
    Collection<LifecycleState> getFaultyStates() {
        return Collections.singletonList(LifecycleState.Failed);
    }
}
