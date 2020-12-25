package com.oci.mds.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProjectConfiguration extends OciConfiguration {

    private String compartmentId;

    private String mysqlInstanceSubnetId;

    private String mysqlInstanceComputeShape;

    private String mysqlVersion;

    private int mysqlInstancePort;

    private long createDbSystemTimeoutInSeconds;

    private long updatingDbSystemTimeoutInSeconds;

    private long deleteDbSystemTimeoutInSeconds;

    private long createBackupTimeoutInSeconds;

    private long deleteBackupTimeoutInSeconds;
}
