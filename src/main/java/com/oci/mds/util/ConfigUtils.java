package com.oci.mds.util;

import com.oci.mds.configuration.ProjectConfiguration;

import com.oracle.bmc.mysql.MysqlaasClient;
import com.oracle.bmc.mysql.model.Configuration;
import com.oracle.bmc.mysql.model.Configuration.LifecycleState;
import com.oracle.bmc.mysql.model.ConfigurationSummary;
import com.oracle.bmc.mysql.model.ConfigurationVariables;
import com.oracle.bmc.mysql.model.CreateConfigurationDetails;
import com.oracle.bmc.mysql.model.UpdateConfigurationDetails;
import com.oracle.bmc.mysql.requests.CreateConfigurationRequest;
import com.oracle.bmc.mysql.requests.DeleteConfigurationRequest;
import com.oracle.bmc.mysql.requests.GetConfigurationRequest;
import com.oracle.bmc.mysql.requests.ListConfigurationsRequest;
import com.oracle.bmc.mysql.requests.UpdateConfigurationRequest;
import com.oracle.bmc.mysql.responses.CreateConfigurationResponse;
import com.oracle.bmc.mysql.responses.DeleteConfigurationResponse;
import com.oracle.bmc.mysql.responses.GetConfigurationResponse;
import com.oracle.bmc.mysql.responses.UpdateConfigurationResponse;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ConfigUtils {
    private MysqlaasClient mysqlaasClient;
    private String compartmentId;
    private String shape;


    public ConfigUtils(ProjectConfiguration config) {
        this.mysqlaasClient = config.getMysqlaasClient();
        this.compartmentId = config.getCompartmentId();
        this.shape = config.getMysqlInstanceComputeShape();
    }

    public CreateConfigurationResponse createConfiguration(CreateConfigurationDetails createConfigurationDetails) {
        final CreateConfigurationRequest request =
            CreateConfigurationRequest.builder()
                .createConfigurationDetails(createConfigurationDetails)
                .build();
        return mysqlaasClient.createConfiguration(request);
    }

    public String createConfiguration() {
        final ConfigurationVariables variables = ConfigurationVariables.builder()
            .build();

        final CreateConfigurationDetails createConfigurationDetails = CreateConfigurationDetails.builder()
            .compartmentId(compartmentId)
            .shapeName(shape)
            .variables(variables)
            .build();

        final CreateConfigurationRequest request = CreateConfigurationRequest.builder()
            .createConfigurationDetails(createConfigurationDetails)
            .build();

        final CreateConfigurationResponse configuration = mysqlaasClient.createConfiguration(request);
        Configuration originalConfiguration = configuration.getConfiguration();

        return originalConfiguration.getId();
    }

    public UpdateConfigurationResponse updateConfiguration(UpdateConfigurationDetails updateConfigurationDetails, String configurationId) {
        final UpdateConfigurationRequest request =
            UpdateConfigurationRequest.builder()
                .updateConfigurationDetails(updateConfigurationDetails)
                .configurationId(configurationId)
                .build();
        return mysqlaasClient.updateConfiguration(request);
    }

    public Configuration getConfiguration(String configurationId) {
        GetConfigurationRequest configurationRequest =
            GetConfigurationRequest.builder()
                .configurationId(configurationId)
                .build();
        return mysqlaasClient.getConfiguration(configurationRequest).getConfiguration();
    }

    public LifecycleState getLifecycleState(String configurationId) {
        GetConfigurationRequest configurationRequest =
            GetConfigurationRequest.builder()
                .configurationId(configurationId)
                .build();
        return mysqlaasClient.getConfiguration(configurationRequest).getConfiguration().getLifecycleState();
    }

    public DeleteConfigurationResponse deleteConfiguration(String configurationId) {
        final DeleteConfigurationRequest request =
            DeleteConfigurationRequest.builder()
                .configurationId(configurationId)
                .build();
        return mysqlaasClient.deleteConfiguration(request);
    }

    public boolean isConfigurationInCompartmentList(String configurationId) {
        boolean inList = false;
        for (ConfigurationSummary configuration : getConfigurationsList()) {
            if (configurationId.contains(configuration.getId())) {
                inList = true;
                break;
            }
        }
        return inList;
    }

    public List<ConfigurationSummary> getConfigurationsList() {
        final ListConfigurationsRequest configurationRequest =
            ListConfigurationsRequest.builder()
                .compartmentId(compartmentId).build();
        return mysqlaasClient.listConfigurations(configurationRequest).getItems();
    }

    public List<ConfigurationSummary> getConfigurationsList(String userCompartmentId) {
        final ListConfigurationsRequest configurationRequest =
            ListConfigurationsRequest.builder()
                .compartmentId(userCompartmentId).build();
        return mysqlaasClient.listConfigurations(configurationRequest).getItems();
    }

    public List<ConfigurationSummary> getConfigurationsList(String displayName, int limit, LifecycleState lifecycleState) {
        final ListConfigurationsRequest configurationRequest =
            ListConfigurationsRequest.builder()
                .compartmentId(compartmentId)
                .displayName(displayName)
                .shapeName(shape)
                .lifecycleState(lifecycleState)
                .limit(limit)
                .build();
        return mysqlaasClient.listConfigurations(
            configurationRequest).getItems();
    }

    public GetConfigurationResponse getConfigurationResponse(String id) {
        final GetConfigurationRequest configurationRequest =
            GetConfigurationRequest.builder()
                .configurationId(id).build();
        return mysqlaasClient.getConfiguration(configurationRequest);
    }

    public String getBuiltInConfigurationIdByShape(String compartmentId, String shapeName) {
        return getBuiltInConfigurationByShape(compartmentId, shapeName).getId();
    }

    public Configuration getBuiltInConfigurationByShape(String compartmentId, String shapeName) {
        Configuration config = null;
        List<ConfigurationSummary> summaries = getConfigurationsList(compartmentId);
        for (ConfigurationSummary summary: summaries ) {
            GetConfigurationResponse cr = getConfigurationResponse(summary.getId());
            if (shapeName.equals(cr.getConfiguration().getShapeName())) {
                config = cr.getConfiguration();
                break;
            }
        }
        return config;
    }
}
