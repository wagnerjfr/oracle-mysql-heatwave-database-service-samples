package com.oci.mds.manager;

import com.oci.mds.configuration.ProjectConfiguration;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.mysql.DbSystemClient;
import com.oracle.bmc.mysql.model.CreateDbSystemDetails;
import com.oracle.bmc.mysql.model.DbSystem;
import com.oracle.bmc.mysql.model.DbSystem.LifecycleState;
import com.oracle.bmc.mysql.model.DbSystemSummary;
import com.oracle.bmc.mysql.model.InnoDbShutdownMode;
import com.oracle.bmc.mysql.model.RestartDbSystemDetails;
import com.oracle.bmc.mysql.model.StopDbSystemDetails;
import com.oracle.bmc.mysql.model.UpdateDbSystemDetails;
import com.oracle.bmc.mysql.requests.CreateDbSystemRequest;
import com.oracle.bmc.mysql.requests.DeleteDbSystemRequest;
import com.oracle.bmc.mysql.requests.GetDbSystemRequest;
import com.oracle.bmc.mysql.requests.ListDbSystemsRequest;
import com.oracle.bmc.mysql.requests.RestartDbSystemRequest;
import com.oracle.bmc.mysql.requests.StartDbSystemRequest;
import com.oracle.bmc.mysql.requests.StopDbSystemRequest;
import com.oracle.bmc.mysql.requests.UpdateDbSystemRequest;
import com.oracle.bmc.mysql.responses.CreateDbSystemResponse;
import com.oracle.bmc.mysql.responses.DeleteDbSystemResponse;
import com.oracle.bmc.mysql.responses.GetDbSystemResponse;
import com.oracle.bmc.mysql.responses.ListDbSystemsResponse;
import com.oracle.bmc.mysql.responses.RestartDbSystemResponse;
import com.oracle.bmc.mysql.responses.StartDbSystemResponse;
import com.oracle.bmc.mysql.responses.StopDbSystemResponse;
import com.oracle.bmc.mysql.responses.UpdateDbSystemResponse;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class DbSystemManager extends AbstractManager<DbSystem, LifecycleState> {

    private final DbSystemClient dbSystemClient;
    private final Duration dbSystemCreateTimeout;
    private final Duration dbSystemDeleteTimeout;
    private final Duration dbSystemUpdatingTimeout;
    private List<String> dbSystemIds;

    public DbSystemManager(ProjectConfiguration config) {
        super(config);
        dbSystemClient = config.getDbSystemClient();
        dbSystemIds = new ArrayList<>();
        dbSystemCreateTimeout = Duration.ofSeconds(config.getCreateDbSystemTimeoutInSeconds());
        dbSystemDeleteTimeout = Duration.ofSeconds(config.getDeleteDbSystemTimeoutInSeconds());
        dbSystemUpdatingTimeout = Duration.ofSeconds(config.getUpdatingDbSystemTimeoutInSeconds());
    }

    /* CREATE Methods */

    public CreateDbSystemResponse createDbSystem(CreateDbSystemDetails createDbSystemDetails) {
        final CreateDbSystemRequest request =
            CreateDbSystemRequest.builder()
                .createDbSystemDetails(createDbSystemDetails)
                .build();

        CreateDbSystemResponse createDbSystemResponse = dbSystemClient.createDbSystem(request);
        dbSystemIds.add(createDbSystemResponse.getDbSystem().getId());
        return createDbSystemResponse;
    }

    public List<CreateDbSystemResponse> createDbSystems(List<CreateDbSystemDetails> dbSystemDetailsList) {
        List<CreateDbSystemRequest> requestList = dbSystemDetailsList.stream()
            .map(db -> CreateDbSystemRequest
                .builder().createDbSystemDetails(db)
                .build())
            .collect(Collectors.toList());

        List<CreateDbSystemResponse> responseList = new ArrayList<>();
        CreateDbSystemRequest createDbSystemRequest = null;
        boolean createException = false;
        try {
            for (CreateDbSystemRequest req : requestList) {
                createDbSystemRequest = req;
                responseList.add(dbSystemClient.createDbSystem(req));
            }
        } catch (BmcException e) {
            final CreateDbSystemDetails details = createDbSystemRequest == null ? null : createDbSystemRequest.getCreateDbSystemDetails();
            log.error("Can't create DbSystem with this request: \n{} \n{}", details, e);
            createException = true;
        }

        dbSystemIds.addAll(responseList.stream()
            .map(CreateDbSystemResponse::getDbSystem)
            .map(DbSystem::getId)
            .collect(Collectors.toList()));

        // The manager will delete all the dbsystems it started creating because one of the requests had an exception
        if (createException) {
            deleteAllDbSystems();
            responseList = Collections.emptyList();
        }

        return responseList;
    }

    public CreateDbSystemResponse createDbSystemAndWaitForState(CreateDbSystemDetails createDbSystemDetails) {
        List<CreateDbSystemResponse> createDbSystemResponses = createDbSystemsAndWaitForState(Collections.singletonList(createDbSystemDetails));
        return createDbSystemResponses.get(0);
    }

    public List<CreateDbSystemResponse> createDbSystemsAndWaitForState(List<CreateDbSystemDetails> dbSystemDetailsList) {
        List<CreateDbSystemResponse> createDbSystemResponses = createDbSystems(dbSystemDetailsList);

        List<String> dbSystemIdList = createDbSystemResponses.stream()
            .map(CreateDbSystemResponse::getDbSystem)
            .map(DbSystem::getId).collect(Collectors.toList());

        waitForLifecycle(dbSystemIdList, LifecycleState.Active, dbSystemCreateTimeout);

        return createDbSystemResponses;
    }

    /* DELETE Methods */

    public DeleteDbSystemResponse deleteDbSystem(DeleteDbSystemRequest deleteDbSystemRequest) {
        return dbSystemClient.deleteDbSystem(deleteDbSystemRequest);
    }

    public DeleteDbSystemResponse deleteDbSystem(String dbSystemId) {
        final DeleteDbSystemRequest deleteDbSystemRequest =
            DeleteDbSystemRequest.builder()
                .dbSystemId(dbSystemId)
                .build();
        return deleteDbSystem(deleteDbSystemRequest);
    }

    public List<DeleteDbSystemResponse> deleteDbSystem(List<String> dbSystemIdList) {
        return dbSystemIdList.stream()
            .map(this::deleteDbSystem)
            .collect(Collectors.toList());
    }

    public void deleteDbSystemsAndWaitForState() {
        deleteDbSystemAndWaitForState(dbSystemIds);
    }

    public void deleteDbSystemAndWaitForState(String dbSystemId) {
        deleteDbSystemAndWaitForState(Collections.singletonList(dbSystemId));
    }

    public void deleteDbSystemAndWaitForState(List<String> dbSystemIdList) {
        deleteDbSystem(dbSystemIdList);
        waitForLifecycle(dbSystemIdList, LifecycleState.Deleted, dbSystemDeleteTimeout);
    }

    public List<DeleteDbSystemResponse> deleteAllDbSystems() {
        return deleteDbSystem(dbSystemIds);
    }

    public void deleteAllDbSystemsAndWaitForState() {
        deleteDbSystemAndWaitForState(dbSystemIds);
    }

    /* GET Methods */

    public GetDbSystemResponse getDbSystem(GetDbSystemRequest getDbSystemRequest) {
        return dbSystemClient.getDbSystem(getDbSystemRequest);
    }

    public DbSystem getDbSystem(String dbSystemId) {
        GetDbSystemRequest getDbSystemRequest = GetDbSystemRequest.builder().dbSystemId(dbSystemId).build();
        return getDbSystem(getDbSystemRequest).getDbSystem();
    }

    public LifecycleState getLifecycleState(String dbSystemId) {
        return getDbSystem(dbSystemId).getLifecycleState();
    }

    public List<DbSystem> getDbSystems() {
        return dbSystemIds.stream()
            .map(this::getDbSystem)
            .collect(Collectors.toList());
    }

    public List<String> getDbSystemIds() {
        return new ArrayList<>(dbSystemIds);
    }

    /* LIST Methods */

    public List<DbSystemSummary> listDbSystems(String compartmentId) {
        final ListDbSystemsRequest listDbSystemsRequest = ListDbSystemsRequest.builder()
            .compartmentId(compartmentId).build();
        final ListDbSystemsResponse dbr = listDbSystems(listDbSystemsRequest);
        return dbr.getItems();
    }

    public ListDbSystemsResponse listDbSystems(ListDbSystemsRequest listDbSystemsRequest) {
        return dbSystemClient.listDbSystems(listDbSystemsRequest);
    }

    public Optional<DbSystemSummary> listDbSystem(String compartmentId, String dbSystemId) {
        return listDbSystems(compartmentId).stream()
            .filter(db -> db.getId().equals(dbSystemId))
            .findFirst();
    }

    public boolean isDbSystemInCompartmentList(String compartmentId, String dbSystemId) {
        return listDbSystem(compartmentId, dbSystemId).isPresent();
    }

    /* STOP Methods */

    public StopDbSystemResponse stopDbSystem(String dbSystemId, InnoDbShutdownMode shutdownMode) {
        final StopDbSystemDetails stopDbSystemDetails =
            StopDbSystemDetails.builder()
                .shutdownType(shutdownMode)
                .build();
        return stopDbSystem(dbSystemId, stopDbSystemDetails);
    }

    public StopDbSystemResponse stopDbSystem(String dbSystemId, StopDbSystemDetails stopDbSystemDetails) {
        final StopDbSystemRequest dbSystemRequest =
            StopDbSystemRequest.builder()
                .dbSystemId(dbSystemId)
                .stopDbSystemDetails(stopDbSystemDetails)
                .build();
        return dbSystemClient.stopDbSystem(dbSystemRequest);
    }

    public List<StopDbSystemResponse> stopDbSystem(List<String> dbSystemIdList, InnoDbShutdownMode shutdownMode) {
        return dbSystemIdList.stream()
            .map(id -> stopDbSystem(id, shutdownMode))
            .collect(Collectors.toList());
    }

    public List<StopDbSystemResponse> stopDbSystem(List<String> dbSystemIdList, StopDbSystemDetails stopDbSystemDetails) {
        return dbSystemIdList.stream()
            .map(id -> stopDbSystem(id, stopDbSystemDetails))
            .collect(Collectors.toList());
    }

    public void stopDbSystemAndWaitForState(String dbSystemId, InnoDbShutdownMode shutdownMode) {
        stopDbSystemAndWaitForState(Collections.singletonList(dbSystemId), shutdownMode);
    }

    public void stopDbSystemAndWaitForState(String dbSystemId, StopDbSystemDetails stopDbSystemDetails) {
        stopDbSystemAndWaitForState(Collections.singletonList(dbSystemId), stopDbSystemDetails);
    }

    public void stopDbSystemAndWaitForState(List<String> dbSystemIdList, InnoDbShutdownMode shutdownMode) {
        stopDbSystem(dbSystemIdList, shutdownMode);
        waitForLifecycle(dbSystemIdList, LifecycleState.Inactive, dbSystemUpdatingTimeout);
    }

    public void stopDbSystemAndWaitForState(List<String> dbSystemIdList, StopDbSystemDetails stopDbSystemDetails) {
        stopDbSystem(dbSystemIdList, stopDbSystemDetails);
        waitForLifecycle(dbSystemIdList, LifecycleState.Inactive, dbSystemUpdatingTimeout);
    }

    public List<StopDbSystemResponse> stopAllDbSystems(StopDbSystemDetails stopDbSystemDetails) {
        return stopDbSystem(dbSystemIds, stopDbSystemDetails);
    }

    public void stopAllDbSystemsAndWaitForState(StopDbSystemDetails stopDbSystemDetails) {
        stopDbSystemAndWaitForState(dbSystemIds, stopDbSystemDetails);
    }

    /* START Methods */

    public StartDbSystemResponse startDbSystem(String dbSystemId) {
        final StartDbSystemRequest dbSystemRequest =
            StartDbSystemRequest.builder()
                .dbSystemId(dbSystemId)
                .build();
        return dbSystemClient.startDbSystem(dbSystemRequest);
    }

    public List<StartDbSystemResponse> startDbSystem(List<String> dbSystemIdList) {
        return dbSystemIdList.stream()
            .map(this::startDbSystem)
            .collect(Collectors.toList());
    }

    public void startDbSystemAndWaitForState(String dbSystemId) {
        startDbSystemAndWaitForState(Collections.singletonList(dbSystemId));
    }

    public void startDbSystemAndWaitForState(List<String> dbSystemIdList) {
        startDbSystem(dbSystemIdList);
        waitForLifecycle(dbSystemIdList, LifecycleState.Active, dbSystemUpdatingTimeout);
    }

    public List<StartDbSystemResponse> startAllDbSystems() {
        return startDbSystem(dbSystemIds);
    }

    public void startAllDbSystemsAndWaitForState() {
        startDbSystemAndWaitForState(dbSystemIds);
    }

    /* RESTART Methods */

    public RestartDbSystemResponse restartDbSystem(String dbSystemId, InnoDbShutdownMode shutdownMode) {
        final RestartDbSystemDetails restartDbSystemDetails =
            RestartDbSystemDetails.builder()
                .shutdownType(shutdownMode)
                .build();
        return restartDbSystem(dbSystemId, restartDbSystemDetails);
    }

    public RestartDbSystemResponse restartDbSystem(String dbSystemId, RestartDbSystemDetails restartDbSystemDetails) {
        final RestartDbSystemRequest dbSystemRequest =
            RestartDbSystemRequest.builder()
                .dbSystemId(dbSystemId)
                .restartDbSystemDetails(restartDbSystemDetails)
                .build();
        return dbSystemClient.restartDbSystem(dbSystemRequest);
    }

    public List<RestartDbSystemResponse> restartDbSystem(List<String> dbSystemIdList, InnoDbShutdownMode shutdownMode) {
        return dbSystemIdList.stream()
            .map(id -> restartDbSystem(id, shutdownMode))
            .collect(Collectors.toList());
    }

    public List<RestartDbSystemResponse> restartDbSystem(List<String> dbSystemIdList, RestartDbSystemDetails restartDbSystemDetails) {
        return dbSystemIdList.stream()
            .map(id -> restartDbSystem(id, restartDbSystemDetails))
            .collect(Collectors.toList());
    }

    public void restartDbSystemAndWaitForState(String dbSystemId, InnoDbShutdownMode shutdownMode) {
        restartDbSystemAndWaitForState(Collections.singletonList(dbSystemId), shutdownMode);
    }

    public void restartDbSystemAndWaitForState(String dbSystemId, RestartDbSystemDetails restartDbSystemDetails) {
        restartDbSystemAndWaitForState(Collections.singletonList(dbSystemId), restartDbSystemDetails);
    }

    public void restartDbSystemAndWaitForState(List<String> dbSystemIdList, InnoDbShutdownMode shutdownMode) {
        restartDbSystem(dbSystemIdList, shutdownMode);
        waitForLifecycle(dbSystemIdList, LifecycleState.Active, dbSystemUpdatingTimeout);
    }

    public void restartDbSystemAndWaitForState(List<String> dbSystemIdList, RestartDbSystemDetails restartDbSystemDetails) {
        restartDbSystem(dbSystemIdList, restartDbSystemDetails);
        waitForLifecycle(dbSystemIdList, LifecycleState.Active, dbSystemUpdatingTimeout);
    }

    public List<RestartDbSystemResponse> restartAllDbSystems(RestartDbSystemDetails restartDbSystemDetails) {
        return restartDbSystem(dbSystemIds, restartDbSystemDetails);
    }

    public void restartAllDbSystemsAndWaitForState(RestartDbSystemDetails restartDbSystemDetails) {
        restartDbSystemAndWaitForState(dbSystemIds, restartDbSystemDetails);
    }

    /* UPDATE Methods */

    public UpdateDbSystemResponse updateDbSystem(String dbSystemId, UpdateDbSystemDetails updateDbSystemDetails) {
        final UpdateDbSystemRequest request =
            UpdateDbSystemRequest.builder()
                .dbSystemId(dbSystemId)
                .updateDbSystemDetails(updateDbSystemDetails)
                .build();
        return dbSystemClient.updateDbSystem(request);
    }

    public List<UpdateDbSystemResponse> updateDbSystem(List<String> dbSystemIdList, UpdateDbSystemDetails updateDbSystemDetails) {
        return dbSystemIdList.stream()
            .map(id -> updateDbSystem(id, updateDbSystemDetails))
            .collect(Collectors.toList());
    }

    public void updateDbSystemAndWaitForState(List<String> dbSystemIdList, UpdateDbSystemDetails updateDbSystemDetails) {
        updateDbSystem(dbSystemIdList, updateDbSystemDetails);
        waitForLifecycle(dbSystemIdList, LifecycleState.Active, dbSystemUpdatingTimeout);
    }

    /* Overridden helper methods to be used in 'waitForLifecycle' methods from super class */

    @Override
    String getResourceId(DbSystem resource) {
        return resource.getId();
    }

    @Override
    DbSystem getResource(String resourceId) {
        return getDbSystem(resourceId);
    }

    @Override
    LifecycleState getResourceLifeCycleState(DbSystem resource) {
        return resource.getLifecycleState();
    }

    @Override
    String getResourceDisplayName(DbSystem resource) {
        return resource.getDisplayName();
    }

    @Override
    Class getResourceClass(DbSystem resource) {
        return resource.getClass();
    }

    @Override
    Collection<LifecycleState> getFaultyStates() {
        return Collections.singletonList(LifecycleState.Failed);
    }
}
