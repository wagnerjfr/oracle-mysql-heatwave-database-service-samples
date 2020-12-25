package com.oci.mds.configuration;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Getter
class ConfigProfile {

    private ConfigFileReader.ConfigFile configWithProfile;
    private BasicAuthenticationDetailsProvider provider;

    ConfigProfile(String ociConfigPath, String profileName) {
        try {
            configWithProfile = ConfigFileReader.parse(ociConfigPath, profileName);
            provider = new ConfigFileAuthenticationDetailsProvider(configWithProfile);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
