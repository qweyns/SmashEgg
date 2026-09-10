package org.karton.smashegg;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;

final class TestSupport {
    private TestSupport() {}

    static YamlConfiguration config() {
        return load("/config.yml");
    }

    static YamlConfiguration lang() {
        return load("/lang/ru_RU.yml");
    }

    static PluginSettings settings() {
        return PluginSettings.load(config(), lang(), ignored -> {});
    }

    private static YamlConfiguration load(String resource) {
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(TestSupport.class.getResourceAsStream(resource), resource),
                StandardCharsets.UTF_8)) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(reader);
            return config;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
