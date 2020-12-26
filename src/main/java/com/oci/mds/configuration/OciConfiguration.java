package com.oci.mds.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.mysql.DbBackupsClient;
import com.oracle.bmc.mysql.DbSystemClient;
import com.oracle.bmc.mysql.MysqlaasClient;
import io.dropwizard.Configuration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.io.FileInputStream;
import java.util.Properties;

@Slf4j
@Getter
@Setter
@JsonIgnoreProperties({
    "configWithProfile",
    "provider",
    "identityClient",
    "mysqlaasClient",
    "dbSystemClient",
    "dbBackupsClient",
    "analyticsClient",
    "availabilityDomain",
    "region",
    "tenantId"
    })

public class OciConfiguration extends Configuration {

    private String regionHost;

    private String stage;

    @NotNull
    private String realm;

    private String availabilityDomain;
    private String logicalADName;

    protected Properties props = new Properties();
    protected String ociConfigPath = null;
    protected String profileName = null;
    protected String clientTenancyId = null;
    protected String mysqlClientEndpoint;

    protected ConfigFileReader.ConfigFile configWithProfile = null;
    protected BasicAuthenticationDetailsProvider provider = null;

    protected IdentityClient identityClient;
    protected MysqlaasClient mysqlaasClient;
    protected DbSystemClient dbSystemClient;
    protected DbBackupsClient dbBackupsClient;

    OciConfiguration() {
        try {
            ociConfigPath = System.getProperty("testConfig");
            if (ociConfigPath == null) {
                // Try to load the desktop developer default
                ociConfigPath = System.getProperty("user.home") + "/.oci/config";
            }
            log.info("loading configuration from {}", ociConfigPath);
            props.load(new FileInputStream(ociConfigPath));

            profileName = System.getProperty("profileName");
            if (profileName == null) {
                profileName = "DEFAULT";
            }
            log.info("using profile {}", profileName);

            ConfigProfile configProfile = new ConfigProfile(ociConfigPath, profileName);
            configWithProfile = configProfile.getConfigWithProfile();
            provider = configProfile.getProvider();

            clientTenancyId = configWithProfile.get("tenancy");

            dbBackupsClient = new DbBackupsClient(provider);
            dbSystemClient = new DbSystemClient(provider);
            identityClient = new IdentityClient(provider);
            mysqlaasClient = new MysqlaasClient(provider);

        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }
}
