package org.karton.smashegg;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;

/** Support class for tests – made public so other packages can use it. */
public class TestSupport {
    private TestSupport() {}

    static YamlConfiguration config() {
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(TestSupport.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(reader);
            return config;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static PluginSettings settings() {
        return PluginSettings.load(config(), ignored -> {});
    }
}