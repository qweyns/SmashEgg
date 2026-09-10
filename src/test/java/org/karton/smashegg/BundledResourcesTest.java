package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.karton.smashegg.config.PluginSettings;

class BundledResourcesTest {

    @Test
    void pluginDescriptorKeepsThePublicMainClassAndApiVersion() {
        YamlConfiguration plugin = TestSupport.load("/plugin.yml");
        assertEquals("org.karton.smashegg.SmashEgg", plugin.getString("main"));
        assertEquals("1.21", plugin.getString("api-version"));
        assertEquals(false, plugin.getBoolean("folia-supported"));
        assertEquals(true, plugin.getBoolean("permissions.smashegg.use.default"));
        assertEquals("op", plugin.getString("permissions.smashegg.reload.default"));
        assertEquals("op", plugin.getString("permissions.smashegg.info.default"));
        assertEquals("op", plugin.getString("permissions.smashegg.stats.default"));
        assertEquals("op", plugin.getString("permissions.smashegg.stats.reset.default"));
        assertEquals(false, plugin.getBoolean("permissions.smashegg.bypass.default"));
        assertTrue(plugin.getStringList("commands.smashegg.aliases").contains("segg"));
    }

    @Test
    void languageFilesParseAndSupplyEveryRequiredMessage() {
        for (String resource : List.of("/lang/ru_RU.yml", "/lang/en_US.yml")) {
            YamlConfiguration lang = TestSupport.load(resource);
            assertNotNull(lang.get("placeholders"), resource);
            assertEquals("-", lang.getString("placeholders.unset"), resource);
            for (String key : PluginSettings.MESSAGE_KEYS) {
                assertNotNull(lang.get("messages." + key), resource + " messages." + key);
            }
        }
    }

    @Test
    void bundledConfigHasNoUnknownKeys() {
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = PluginSettings.load(TestSupport.config(), TestSupport.lang(), warnings::add);
        assertTrue(warnings.stream().noneMatch(w -> w.contains("unknown config key")), warnings::toString);
        assertEquals(PluginSettings.CONFIG_VERSION, settings.configVersion());
        assertEquals("lang", settings.langDirectory());
        assertEquals("stats.yml", settings.statsFile());
    }
}
