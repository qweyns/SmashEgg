package org.karton.smashegg;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.karton.smashegg.config.PluginSettings;

public final class TestSupport {
    private TestSupport() {}

    public static YamlConfiguration config() {
        return load("/config.yml");
    }

    public static YamlConfiguration lang() {
        return load("/lang/ru_RU.yml");
    }

    public static YamlConfiguration langEn() {
        return load("/lang/en_US.yml");
    }

    public static PluginSettings settings() {
        return PluginSettings.load(config(), lang(), ignored -> {});
    }

    public static YamlConfiguration load(String resource) {
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
