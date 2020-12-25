package com.oci.mds.util;

import com.google.common.base.Strings;

import com.oci.mds.configuration.ProjectConfiguration;

import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

@Slf4j
public class OciUtils {

    private IdentityClient identityClient;
    private ProjectConfiguration config;

    private static final String AVAILABILITY_DOMAIN = "AVAILABILITY_DOMAIN";

    public OciUtils(ProjectConfiguration config) {
        this.config = config;
        this.identityClient = config.getIdentityClient();
        this.identityClient.setEndpoint(config.getMysqlClientEndpoint());
    }

    public String getConfiguredAvailabilityDomain() {
        // If the AVAILABILITY_DOMAIN system property is set, use that,
        // otherwise return the first AD for the configured tenancy
        String ad = System.getenv(AVAILABILITY_DOMAIN);
        if (Strings.isNullOrEmpty(ad)) {
            ad = listAvailabilityDomains().get(0).getName();
        }
        return ad;
    }

    public List<AvailabilityDomain> listAvailabilityDomains() {
        val tenancyId = config.getClientTenancyId();
        log.debug("Fetching availability domains for tenancy {}", tenancyId);
        val request = ListAvailabilityDomainsRequest.builder().compartmentId(tenancyId).build();
        val availabilityDomains = identityClient.listAvailabilityDomains(request).getItems();
        log.debug("Found availability domains: {}", availabilityDomains);
        return availabilityDomains;
    }
}
