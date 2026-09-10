package org.karton.smashegg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SmashEgg extends JavaPlugin {
    private BukkitAudiences adventure;
    private PluginSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!reloadSettings()) {
            getLogger().severe("SmashEgg disabled: fix config.yml and restart the server.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        adventure = BukkitAudiences.create(this);
        CommandHandler handler = new CommandHandler(this);
        PluginCommand command = Objects.requireNonNull(getCommand("smashegg"), "Missing smashegg command in plugin.yml");
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(
                new EggListener(this, () -> ThreadLocalRandom.current().nextInt(100)), this);
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
    }

    boolean reloadSettings() {
        try (InputStream resource = Objects.requireNonNull(getResource("config.yml"), "Missing default config.yml")) {
            YamlConfiguration defaults = new YamlConfiguration();
            defaults.load(new InputStreamReader(resource, StandardCharsets.UTF_8));
            YamlConfiguration candidate = new YamlConfiguration();
            candidate.load(new File(getDataFolder(), "config.yml"));
            candidate.setDefaults(defaults);
            PluginSettings next = PluginSettings.load(candidate, getLogger()::warning);
            settings = next;
            return true;
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            getLogger().severe("Cannot load config.yml; active settings unchanged: " + e.getMessage());
            return false;
        }
    }

    PluginSettings settings() {
        return settings;
    }

    void message(CommandSender sender, String key) {
        Component message = settings.messages().get(key);
        if (message != null && !message.equals(Component.empty())) {
            adventure.sender(sender).sendMessage(message);
        }
    }

    void sound(Player player, String key) {
        Sound sound = settings.sounds().get(key);
        if (sound != null) player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
}
