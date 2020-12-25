package com.oci.mds.configuration.reader;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@linkplain ConfigurationSourceProvider} that parses a TypeSafe config file.
 */
@Slf4j
public class TypeSafeConfigProvider implements ConfigurationSourceProvider {
    @NonNull private final TypeSafeReader<String> reader;

    /**
     * Constructs a new {@linkplain TypeSafeConfigProvider} that uses the given
     * {@linkplain TypeSafeReader} to read a TypeSafe configuration file.
     */
    public TypeSafeConfigProvider(TypeSafeReader<String> reader) {
        this.reader = reader;
    }

    /**
     * Reads the config file indicated by the given source, parses it as a TypeSafe config file,
     * and returns it as json byte stream, which Dropwizard supports out of the box.
     */
    @Override
    public InputStream open(@NonNull String source) throws IOException {
        String json = reader.read(source);
        log.debug("Running with configuration:\n{}", json);
        return new ByteArrayInputStream(json.getBytes("UTF-8"));
    }
}
