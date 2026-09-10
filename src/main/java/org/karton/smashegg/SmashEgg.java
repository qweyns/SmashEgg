package org.karton.smashegg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SmashEgg extends JavaPlugin {
    private final Stats stats = new Stats();
    /** Particle names already reported as unknown; static like the particle cache it belongs to. */
    private static final Set<String> warnedParticles = ConcurrentHashMap.newKeySet();
    private PluginSettings settings;
    private String version = "unknown";
    private boolean enabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!reloadSettings()) {
            getLogger().severe("SmashEgg disabled: fix config.yml and restart the server.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        version = readVersion();
        stats.load(statsFile(), getLogger());
        // Paper implements Adventure's Audience on CommandSender and Player, so no bridge is needed.
        CommandHandler handler = new CommandHandler(this);
        PluginCommand command = Objects.requireNonNull(getCommand("smashegg"), "Missing smashegg command in plugin.yml");
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(
                new EggListener(this, () -> ThreadLocalRandom.current().nextInt(100)), this);
        enabled = true;
    }

    @Override
    public void onDisable() {
        if (enabled) saveStats();
    }

    /**
     * Attaches the bundled defaults to a user configuration and makes lookups fall back to them, so
     * a config written by an older SmashEgg — or one where a line was deleted to get the default —
     * still supplies every key. Without {@code copyDefaults} the section views returned by
     * {@code getValues} would only contain the keys the user actually wrote.
     */
    static YamlConfiguration withDefaults(YamlConfiguration user, YamlConfiguration defaults) {
        user.setDefaults(defaults);
        user.options().copyDefaults(true);
        return user;
    }

    boolean reloadSettings() {
        try (InputStream resource = Objects.requireNonNull(getResource("config.yml"), "Missing default config.yml")) {
            YamlConfiguration defaults = new YamlConfiguration();
            defaults.load(new InputStreamReader(resource, StandardCharsets.UTF_8));
            YamlConfiguration candidate = new YamlConfiguration();
            candidate.load(new File(getDataFolder(), "config.yml"));
            withDefaults(candidate, defaults);
            Consumer<String> warning = getLogger()::warning;
            YamlConfiguration lang = loadLanguage(candidate, warning);
            settings = PluginSettings.load(candidate, lang, warning);
            Particles.resetCache();
            warnedParticles.clear();
            return true;
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            getLogger().severe("Cannot load config.yml; active settings unchanged: " + e.getMessage());
            return false;
        }
    }

    /**
     * Language file lookup: {@code plugins/SmashEgg/lang/<language>.yml} wins over the bundled copy,
     * so admins can translate without touching the jar. An unknown language falls back to the
     * bundled default instead of disabling every message.
     */
    private YamlConfiguration loadLanguage(FileConfiguration config, Consumer<String> warning)
            throws IOException, InvalidConfigurationException {
        String language = config.getString("settings.language", PluginSettings.DEFAULT_LANGUAGE);
        if (!language.matches("[A-Za-z0-9_\\-]+")) {
            throw ConfigNodes.invalid("settings.language", "invalid language name: " + language);
        }
        YamlConfiguration lang = readLanguage(language);
        if (lang != null) {
            if (getResource("lang/" + language + ".yml") != null) saveResource("lang/" + language + ".yml", false);
            return lang;
        }
        warning.accept("settings.language: lang/" + language + ".yml not found; falling back to "
                + PluginSettings.DEFAULT_LANGUAGE + ".");
        YamlConfiguration fallback = readLanguage(PluginSettings.DEFAULT_LANGUAGE);
        return fallback == null ? new YamlConfiguration() : fallback;
    }

    private YamlConfiguration readLanguage(String language) throws IOException, InvalidConfigurationException {
        String path = "lang/" + language + ".yml";
        File userFile = new File(getDataFolder(), path);
        YamlConfiguration lang = new YamlConfiguration();
        if (userFile.isFile()) {
            lang.load(userFile);
            return lang;
        }
        try (InputStream in = getResource(path)) {
            if (in == null) return null;
            lang.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return lang;
        }
    }

    /** Read from the filtered plugin.yml inside the jar; avoids version-specific metadata API. */
    private String readVersion() {
        try (InputStream in = getResource("plugin.yml")) {
            if (in == null) return "unknown";
            YamlConfiguration description = new YamlConfiguration();
            description.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return description.getString("version", "unknown");
        } catch (IOException | InvalidConfigurationException e) {
            return "unknown";
        }
    }

    private File statsFile() {
        return new File(getDataFolder(), "stats.yml");
    }

    PluginSettings settings() {
        return settings;
    }

    Stats stats() {
        return stats;
    }

    /** Writes stats.yml right away; a manual reset must survive a crash. */
    void saveStats() {
        stats.save(statsFile(), getLogger());
    }

    String version() {
        return version;
    }

    /**
     * Names of the loaded worlds, for tab completion. A seam of its own because
     * {@code JavaPlugin#getServer()} is final and cannot be stubbed without the inline mock maker.
     */
    List<String> worldNames() {
        return getServer().getWorlds().stream().map(World::getName).toList();
    }

    /** Message, sound and particles belonging to one effect key. */
    void effect(Player player, String key, Map<String, String> context) {
        PluginSettings current = settings;
        send(player, current.messages().get(key), context);
        Sound sound = current.sounds().get(key);
        if (sound != null) player.playSound(sound);
        ParticleSpec particle = current.particles().get(key);
        if (particle != null) spawnParticles(player, particle);
    }

    void message(CommandSender sender, String key) {
        message(sender, key, Map.of());
    }

    void message(CommandSender sender, String key, Map<String, String> context) {
        send(sender, settings.messages().get(key), context);
    }

    void logEvent(String effect, Map<String, String> context) {
        if (!settings.logEvents()) return;
        getLogger().info(() -> "SmashEgg " + effect
                + " player=" + context.get("player")
                + " world=" + context.get("world")
                + " entity=" + context.get("entity")
                + " mode=" + context.get("mode")
                + " hand=" + context.get("hand"));
    }

    private void send(CommandSender sender, MessageSpec message, Map<String, String> context) {
        if (message == null || !message.enabled()) return;
        Component text = ColorUtil.parse(message.text(), context);
        switch (message.output()) {
            case NONE -> { }
            case ACTIONBAR -> {
                if (sender instanceof Player player) player.sendActionBar(text);
                else sender.sendMessage(text);
            }
            case TITLE -> {
                if (sender instanceof Player player) player.showTitle(Title.title(text, Component.empty()));
                else sender.sendMessage(text);
            }
            case CHAT -> sender.sendMessage(text);
        }
    }

    private void spawnParticles(Player player, ParticleSpec spec) {
        Particle particle = Particles.resolve(spec.name());
        if (particle == null) {
            if (warnedParticles.add(spec.name())) {
                getLogger().warning("particles: " + spec.name() + " is unknown on this server; effect skipped.");
            }
            return;
        }
        player.getWorld().spawnParticle(particle, player.getLocation().add(0.0, 1.0, 0.0),
                spec.count(), spec.spread(), spec.spread(), spec.spread(), spec.speed());
    }
}
