package com.oci.mds.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.mysql.DbBackupsClient;
import com.oracle.bmc.mysql.DbSystemClient;
import com.oracle.bmc.mysql.MysqlaasClient;
import com.oracle.pic.commons.configuration.location.Location;
import com.oracle.pic.commons.configuration.location.LocationOverride;
import com.oracle.pic.commons.service.configuration.ServiceConfiguration;
import com.oracle.pic.commons.util.Realm;
import com.oracle.pic.commons.util.Region;
import lombok.AccessLevel;
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

public class OciConfiguration extends ServiceConfiguration {

    private String regionHost;

    @NotNull
    private Realm realm;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Location location;

    @Getter(AccessLevel.NONE)
    private LocationOverride locationOverride;

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

    public OciConfiguration() {
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

    /**
     * Allow config to override the region and AD so we don't look them up from /etc/region and
     * /etc/availability-domain.
     *
     * @return Location
     */
    private Location resolveLocation() {
        return (locationOverride == null) ? Location.fromEnvironmentFiles() : Location.fromLocationOverride(locationOverride);
    }

    private Location getLocation() {
        if (location == null) {
            location = resolveLocation();
        }
        return location;
    }

    public Region getRegion() {
        return getLocation().getRegion();
    }
}
