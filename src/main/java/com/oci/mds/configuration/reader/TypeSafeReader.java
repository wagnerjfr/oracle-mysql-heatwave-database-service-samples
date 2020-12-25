package com.oci.mds.configuration.reader;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigResolveOptions;

public abstract class TypeSafeReader<T> {
    private static final String DEFAULT_ROOT_KEY = "config";
    private final String rootKey;
    private static final ConfigResolveOptions ALLOW_UNRESOLVED = ConfigResolveOptions.defaults().setAllowUnresolved(true);
    private static final ConfigResolveOptions NO_ENV_VARS = ConfigResolveOptions.defaults().setUseSystemEnvironment(false);

    TypeSafeReader() {
        this.rootKey = DEFAULT_ROOT_KEY;
    }

    TypeSafeReader(String rootKey) {
        this.rootKey = rootKey;
    }

    String read(T source) {
        return this.toJson(this.getResolvedConfig(source));
    }

    private Config getResolvedConfig(T source) {
        Config config = this.getConfig(source);
        return this.applySystemPropertyOverrides(config);
    }

    protected abstract Config getConfig(T var1);

    private String toJson(Config config) {
        return config.root().render(ConfigRenderOptions.concise().setFormatted(true).setJson(true));
    }

    private Config applySystemPropertyOverrides(Config config) {
        ConfigFactory.invalidateCaches();
        Config rooted = ConfigFactory.empty().withValue(this.rootKey, config.root());
        Config rootedWithOverrides = ConfigFactory.load(rooted, ALLOW_UNRESOLVED);

        return rootedWithOverrides.getConfig(this.rootKey).resolve(NO_ENV_VARS);
    }
}
