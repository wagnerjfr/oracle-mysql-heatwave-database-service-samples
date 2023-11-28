# oracle-heatwave-database-service-samples
This repository shows two simple examples how to create [Oracle HeatWave Database Service (MDS)](https://www.oracle.com/mysql/)
resources "DbSystem" and "Backup" using [SDK for Java](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/javasdk.htm).

## Documentation
- [Oracle HeatWave Database Service](https://docs.oracle.com/en-us/iaas/mysql-database/)
- [Oracle Cloud Infrastructure SDK for Java - GitHub](https://github.com/oracle/oci-java-sdk) 
- For basic set up, see [Getting Started](https://docs.cloud.oracle.com/iaas/Content/API/SDKDocs/javasdkgettingstarted.htm).
- For details on compatibility, advanced configurations, and add-ons, see [Configuration](https://docs.cloud.oracle.com/iaas/Content/API/SDKDocs/javasdkconfig.htm).

## Prerequisite
- [SDK and CLI Configuration File](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm#SDK_and_CLI_Configuration_File)

Oracle Cloud Infrastructure SDKs and CLI require basic configuration information, like user credentials and tenancy OCID. You can provide this information by:
- Using a configuration file
- Declaring a configuration at runtime

This repository uses the configuration file, located at "_/home/<my-username>/.oci/config_" and it's similar to:
```
[DEFAULT]
user=ocid1.user.oc1..<unique_ID>
fingerprint=<your_fingerprint>
key_file=~/.oci/oci_api_key.pem
tenancy=ocid1.tenancy.oc1..<unique_ID>
region=us-ashburn-1
```

## Setting up the project
1. Fork (or clone) this project
2. Change the config file ***"iad-ad-1.conf"*** replacing the ***"<unique_ID>"*** to your OCIDs.
3. Build the project by running `mvn clean package`

## Running

The project has 2 samples: DbSystem and Backup.

- **DbSystem**: create DbSystem, stop DbSystem, start DbSystem and delete DbSystem
- **Backup**: create DbSystem, create Backup, delete DbSystem and delete Backup 

They can be executed separately by setting the parameter `-DsampleOption` which can be `DbSystem` or `Backup`.

After the build is successful, trigger:
```
$ java -DprofileName=DEFAULT \
  -DtestConfig=/home/<your-username>/.oci/config -DsampleOption=<DbSystem | Backup> \
  -Dsun.net.http.allowRestrictedHeaders=true \
  -classpath target/oci-mds-samples-1.0-SNAPSHOT.jar com.oci.mds.Runner \
  server config/iad-ad-1.conf
```

### IntelliJ Configuration

In the menu bar, click in **Run** and then **Edit Configurations...**.

Click in the **+** button to add a new configuration and select **Application**.

**Name:** Runner _(or whatever name you want ...)_

**Main class:** com.oci.mds.Runner

**VM Options:** -DprofileName=**YOUR-PROFILE** -DtestConfig=/home/**your-username**/.oci/config -DsampleOption=**DbSystem|Backup**

**Program arguments:** server config/**config-file**

**Working directory:** /home/**your-folder**/oci-mds


## Execution logs

- DbSystem [[full logs]](https://github.com/wagnerjfr/oracle-mysql-database-service-java-sdk/blob/main/logs/dbsystemSample.log)
```
....
INFO  2020-12-24 19:16:20 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 626 seconds (timeout: 1800s)
| DbSystem [Creating]: ocid1.mysqldbsystem.oc1.iad.aaaaaaaa2c7wy7mwhakmbvhu56sy6zglj7cxy3ranj4wgsiqvomggpdlh5qq - 'OCI-MDS.DbSystemSample.1608847538891' 

INFO  2020-12-24 19:16:33 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 639 seconds (timeout: 1800s)
| DbSystem [Creating]: ocid1.mysqldbsystem.oc1.iad.aaaaaaaa2c7wy7mwhakmbvhu56sy6zglj7cxy3ranj4wgsiqvomggpdlh5qq - 'OCI-MDS.DbSystemSample.1608847538891' 

INFO  2020-12-24 19:16:45 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 651 seconds (timeout: 1800s)
| DbSystem [Creating]: ocid1.mysqldbsystem.oc1.iad.aaaaaaaa2c7wy7mwhakmbvhu56sy6zglj7cxy3ranj4wgsiqvomggpdlh5qq - 'OCI-MDS.DbSystemSample.1608847538891' 

INFO  2020-12-24 19:16:56 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 663 seconds (timeout: 1800s)
| DbSystem [Active]: ocid1.mysqldbsystem.oc1.iad.aaaaaaaa2c7wy7mwhakmbvhu56sy6zglj7cxy3ranj4wgsiqvomggpdlh5qq - 'OCI-MDS.DbSystemSample.1608847538891' 

INFO  2020-12-24 19:17:06 c.o.m.s.DbSystemSample: Stopping DbSystem
INFO  2020-12-24 19:17:10 c.o.m.m.AbstractManager: Waiting to become [Inactive] ... executed time 0 seconds (timeout: 1800s)
| DbSystem [Updating]: ocid1.mysqldbsystem.oc1.iad.aaaaaaaa2c7wy7mwhakmbvhu56sy6zglj7cxy3ranj4wgsiqvomggpdlh5qq - 'OCI-MDS.DbSystemSample.1608847538891' 

INFO  2020-12-24 19:17:22 c.o.m.m.AbstractManager: Waiting to become [Inactive] ... executed time 12 seconds (timeout: 1800s)
| DbSystem [Updating]: ocid1.mysqldbsystem.oc1.iad.aaaaaaaa2c7wy7mwhakmbvhu56sy6zglj7cxy3ranj4wgsiqvomggpdlh5qq - 'OCI-MDS.DbSystemSample.1608847538891' 
....
```

- Backup [[full logs]](https://github.com/wagnerjfr/oracle-mysql-database-service-java-sdk/blob/main/logs/backupSample.log)
```
....
INFO  2020-12-24 19:41:35 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 688 seconds (timeout: 1800s)
| DbSystem [Active]: ocid1.mysqldbsystem.oc1.iad.aaaaaaaalsajeidser3dcriohf4c2k4cm2oit5gbkfv54xtbtc6n4oq5d5ha - 'OCI-MDS.BackupSample.1608848992973.DbSystem' 

INFO  2020-12-24 19:41:45 c.o.m.s.BackupSample: Creating Backup
INFO  2020-12-24 19:41:47 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 0 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:42:00 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 12 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:42:12 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 24 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:42:24 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 36 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:42:36 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 48 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:42:48 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 60 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:43:00 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 72 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:43:12 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 84 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:43:24 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 96 seconds (timeout: 600s)
| Backup [Creating]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:43:36 c.o.m.m.AbstractManager: Waiting to become [Active] ... executed time 109 seconds (timeout: 600s)
| Backup [Active]: ocid1.mysqlbackup.oc1.iad.aaaaaaaab6wu3vwrgwbytwhen5addwyqz2tanjfroua4ymyucshr7kn27era - 'OCI-MDS.BackupSample.1608849705176.Backup' 

INFO  2020-12-24 19:43:46 c.o.m.s.BackupSample: Deleting DbSystem
INFO  2020-12-24 19:44:01 c.o.m.m.AbstractManager: Waiting to become [Deleted] ... executed time 1 seconds (timeout: 1800s)
| DbSystem [Deleting]: ocid1.mysqldbsystem.oc1.iad.aaaaaaaalsajeidser3dcriohf4c2k4cm2oit5gbkfv54xtbtc6n4oq5d5ha - 'OCI-MDS.BackupSample.1608848992973.DbSystem'
....
```
