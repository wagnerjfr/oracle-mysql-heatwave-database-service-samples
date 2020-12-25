package com.oci.mds;

import com.oci.mds.configuration.ProjectConfiguration;
import com.oci.mds.configuration.reader.TypeSafeFileReader;
import com.oci.mds.configuration.reader.TypeSafeConfigProvider;
import com.oci.mds.sample.BackupSample;
import com.oci.mds.sample.DbSystemSample;

import com.oci.mds.util.ShutDownHook;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Runner extends Application<ProjectConfiguration> {

    @Override
    public void initialize(Bootstrap<ProjectConfiguration> bootstrap) {
        TypeSafeFileReader fileReader = new TypeSafeFileReader();
        bootstrap.setConfigurationSourceProvider(new TypeSafeConfigProvider(fileReader));
    }

    @Override
    public void run(ProjectConfiguration config, Environment environment) {
        boolean exceptionCaught = false;
        try {
            String sampleArg = System.getProperty("sampleOption", Option.DBSYSTEM.getName()).toUpperCase();
            Option option = Option.valueOf(sampleArg);

            // Configure Shutdown Hook
            ShutDownHook shutDownHook = new ShutDownHook(config);
            Runtime.getRuntime().addShutdownHook(shutDownHook);

            switch (option) {
                case DBSYSTEM:
                    new DbSystemSample(config).run();
                    break;
                case BACKUP:
                    new BackupSample(config).run();
                    break;
                default:
                    log.warn("Invalid option");
            }

            // Remove Shutdown Hook since the execution was not interrupted before
            Runtime.getRuntime().removeShutdownHook(shutDownHook);

        } catch (Exception e) {
            exceptionCaught = true;
            log.error("Error running samples", e);
        }

        System.exit(exceptionCaught ? 1 : 0);
    }

    public static void main(String[] args) throws Exception {
        log.info("Starting MDS Sample....");
        new Runner().run(args);
    }

    @Getter
    private enum Option {
        DBSYSTEM("DBSYSTEM"), BACKUP("BACKUP");

        private String name;

        Option(String name) {
            this.name = name;
        }
    }
}