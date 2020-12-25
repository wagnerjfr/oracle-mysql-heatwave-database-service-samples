package com.oci.mds.manager;

import com.google.common.collect.ImmutableMap;

import com.oci.mds.configuration.ProjectConfiguration;
import com.oci.mds.util.ConfigUtils;

import com.oracle.bmc.mysql.model.BackupPolicy;
import com.oracle.bmc.mysql.model.CreateBackupPolicyDetails;
import com.oracle.bmc.mysql.model.CreateDbSystemDetails;
import com.oracle.bmc.mysql.model.CreateDbSystemSourceDetails;

import lombok.Builder;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ToString
@Builder(builderMethodName = "hiddenBuilder")
public class DbSystemDetailsSetup {

    public static final String ADMIN_USERNAME = "TestAdmin";
    public static final String ADMIN_PASSWORD = "Secre7t?";
    private static final Integer STORAGE_SIZE = 50;
    public static final int DEFAULT_PORT = 3306;
    public static final int DEFAULT_XPORT = 33060;

    @ToString.Exclude
    ProjectConfiguration config;

    String displayName;
    String description;
    String compartmentId;
    String mysqlVersion;
    String logicalAdName;
    String subnetId;
    String instanceShape;
    String configurationId;
    String adminUsername;
    String adminPassword;
    Integer dataStorageSize;
    String hostnameLabel;
    String hostImageId;
    boolean isHighlyAvailable;

    Map<String, String> freeFormTags;
    Map<String, Map<String, Object>> definedTags;

    CreateBackupPolicyDetails createBackupPolicyDetails;
    BackupPolicy backupPolicy;
    CreateDbSystemSourceDetails sourceDetails;

    public static DbSystemDetailsSetupBuilder builder(ProjectConfiguration config, String displayName) {

        final ImmutableMap<String, String> freeFormTags =
            ImmutableMap.of("OCI-MDS", "DbSystemDetailsSetup");

        ImmutableMap<String, Map<String, Object>> definedTags = null;

        final String configurationId = new ConfigUtils(config)
            .getBuiltInConfigurationIdByShape(config.getCompartmentId(), config.getMysqlInstanceComputeShape());

        final CreateBackupPolicyDetails createBackupPolicyDetails =
            CreateBackupPolicyDetails.builder()
                .isEnabled(false)
                .build();

        return hiddenBuilder().config(config)
            .displayName(displayName)
            .description(displayName + " created by " + System.getProperty("user.name"))
            .compartmentId(config.getCompartmentId())
            .logicalAdName(config.getLogicalADName())
            .subnetId(config.getMysqlInstanceSubnetId())
            .instanceShape(config.getMysqlInstanceComputeShape())
            .configurationId(configurationId)
            .adminUsername(ADMIN_USERNAME)
            .adminPassword(ADMIN_PASSWORD)
            .dataStorageSize(STORAGE_SIZE)
            .freeFormTags(freeFormTags)
            .definedTags(definedTags)
            .createBackupPolicyDetails(createBackupPolicyDetails);
    }

    public CreateDbSystemDetails createDbSystemDetails() {

        dataStorageSize = (backupPolicy == null) ? dataStorageSize : null;

        return CreateDbSystemDetails.builder()
            .compartmentId(compartmentId)
            .availabilityDomain(logicalAdName)
            .displayName(displayName)
            .description(description)
            .adminUsername(ADMIN_USERNAME)
            .adminPassword(ADMIN_PASSWORD)
            .shapeName(instanceShape)
            .dataStorageSizeInGBs(dataStorageSize)
            .mysqlVersion(mysqlVersion)
            .compartmentId(compartmentId)
            .subnetId(subnetId)
            .configurationId(configurationId)
            .backupPolicy(createBackupPolicyDetails)
            .hostnameLabel(hostnameLabel)
            .definedTags(definedTags)
            .freeformTags(freeFormTags)
            .source(sourceDetails)
            .build();
    }

    public List<CreateDbSystemDetails> createDbSystemDetailsList(int number) {
        List<CreateDbSystemDetails> list = new ArrayList<>(number);
        String saveDisplayName = displayName;
        for (int i = 1; i <= number; i++) {
            displayName = String.format("%s.%d", saveDisplayName, i);
            list.add(createDbSystemDetails());
        }
        displayName = saveDisplayName;
        return list;
    }
}
