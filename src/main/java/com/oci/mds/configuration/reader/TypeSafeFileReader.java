package com.oci.mds.configuration.reader;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;

public class TypeSafeFileReader extends TypeSafeReader<String> {

    public TypeSafeFileReader(String rootKey) {
        super(rootKey);
    }

    protected Config getConfig(String path) {
        File file = new File(path);
        Preconditions.checkArgument(file.isFile(), "path %s does not exist or is not a file", new Object[]{path});
        return ConfigFactory.parseFile(file);
    }

    public TypeSafeFileReader() { }
}
