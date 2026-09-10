package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommandHandlerTest {
    private SmashEgg plugin;
    private CommandSender sender;
    private CommandHandler handler;
    private Stats stats;

    @BeforeEach
    void setup() {
        plugin = mock(SmashEgg.class);
        sender = mock(CommandSender.class);
        stats = new Stats();
        when(sender.hasPermission("smashegg.use")).thenReturn(true);
        when(plugin.stats()).thenReturn(stats);
        when(plugin.version()).thenReturn("4.0.0");
        when(plugin.settings()).thenReturn(TestSupport.settings());
        handler = new CommandHandler(plugin);
    }

    @Test
    void unauthorizedReloadDoesNotTouchConfiguration() {
        handler.onCommand(sender, null, "smashegg", new String[]{"reload"});
        verify(plugin).message(sender, "no-permission");
        verify(plugin, never()).reloadSettings();
    }

    @Test
    void missingUsePermissionIsDenied() {
        when(sender.hasPermission("smashegg.use")).thenReturn(false);
        when(sender.hasPermission("smashegg.reload")).thenReturn(true);
        handler.onCommand(sender, null, "smashegg", new String[]{"reload"});
        verify(plugin).message(sender, "no-permission");
        verify(plugin, never()).reloadSettings();
    }

    @Test
    void reloadIsCaseInsensitiveAndReportsActualResult() {
        when(sender.hasPermission("smashegg.reload")).thenReturn(true);
        when(plugin.reloadSettings()).thenReturn(true, false);
        handler.onCommand(sender, null, "smashegg", new String[]{"RELOAD"});
        handler.onCommand(sender, null, "smashegg", new String[]{"reload"});
        verify(plugin).message(sender, "reload-success");
        verify(plugin).message(sender, "reload-failure");
    }

    @Test
    void missingArgumentsShowUsageAndUnknownOnesAreReported() {
        handler.onCommand(sender, null, "smashegg", new String[0]);
        verify(plugin).message(sender, "usage");
        handler.onCommand(sender, null, "smashegg", new String[]{"help"});
        verify(plugin).message(sender, "unknown");
        verify(plugin, never()).reloadSettings();
    }

    @Test
    @SuppressWarnings("unchecked")
    void infoShowsTheGlobalRulesWithoutArguments() {
        when(sender.hasPermission("smashegg.info")).thenReturn(true);
        handler.onCommand(sender, null, "smashegg", new String[]{"info"});
        ArgumentCaptor<Map<String, String>> context = ArgumentCaptor.forClass(Map.class);
        verify(plugin).message(eq(sender), eq("info"), context.capture());
        assertEquals("4.0.0", context.getValue().get("version"));
        assertEquals("-", context.getValue().get("world"));
        assertEquals("-", context.getValue().get("entity"));
        assertEquals("30", context.getValue().get("break"));
        assertEquals("70", context.getValue().get("ground"));
        assertEquals("on", context.getValue().get("spawner"));
        assertEquals("no", context.getValue().get("creative"));
        assertEquals("blacklist", context.getValue().get("filter"));
        assertEquals("1", context.getValue().get("cooldown"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void infoResolvesWorldAndEntityOverrides() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world_nether");
        when(player.getWorld()).thenReturn(world);
        when(player.hasPermission("smashegg.use")).thenReturn(true);
        when(player.hasPermission("smashegg.info")).thenReturn(true);
        YamlConfiguration config = TestSupport.config();
        config.set("settings.worlds.world_nether.ground-spawn-chance", 100);
        config.set("settings.entities.zombie.ground-spawn-chance", 5);
        config.set("settings.cooldown-ticks", 20);
        when(plugin.settings()).thenReturn(PluginSettings.load(config, TestSupport.lang(), ignored -> {}));

        handler.onCommand(player, null, "smashegg", new String[]{"INFO", "world_nether", "zombie"});
        ArgumentCaptor<Map<String, String>> context = ArgumentCaptor.forClass(Map.class);
        verify(plugin).message(eq(player), eq("info"), context.capture());
        assertEquals("world_nether", context.getValue().get("world"));
        assertEquals("ZOMBIE", context.getValue().get("entity"));
        assertEquals("5", context.getValue().get("ground"));
        assertEquals("20", context.getValue().get("cooldown"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void singleArgumentIsAnEntityWhenNoLoadedWorldHasThatName() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world_nether");
        when(player.getWorld()).thenReturn(world);
        when(player.hasPermission("smashegg.use")).thenReturn(true);
        when(player.hasPermission("smashegg.info")).thenReturn(true);
        when(plugin.worldNames()).thenReturn(List.of("world", "world_nether"));
        YamlConfiguration config = TestSupport.config();
        config.set("settings.worlds.world_nether.ground-spawn-chance", 100);
        config.set("settings.entities.zombie.ground-spawn-chance", 5);
        when(plugin.settings()).thenReturn(PluginSettings.load(config, TestSupport.lang(), ignored -> {}));

        handler.onCommand(player, null, "smashegg", new String[]{"info", "zombie"});
        ArgumentCaptor<Map<String, String>> context = ArgumentCaptor.forClass(Map.class);
        verify(plugin).message(eq(player), eq("info"), context.capture());
        assertEquals("world_nether", context.getValue().get("world"));
        assertEquals("ZOMBIE", context.getValue().get("entity"));
        assertEquals("5", context.getValue().get("ground"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void singleArgumentIsAWorldWhenSuchWorldIsLoaded() {
        when(sender.hasPermission("smashegg.info")).thenReturn(true);
        when(plugin.worldNames()).thenReturn(List.of("world", "world_nether"));
        YamlConfiguration config = TestSupport.config();
        config.set("settings.worlds.world_nether.ground-spawn-chance", 15);
        when(plugin.settings()).thenReturn(PluginSettings.load(config, TestSupport.lang(), ignored -> {}));

        handler.onCommand(sender, null, "smashegg", new String[]{"info", "WORLD_NETHER"});
        ArgumentCaptor<Map<String, String>> context = ArgumentCaptor.forClass(Map.class);
        verify(plugin).message(eq(sender), eq("info"), context.capture());
        assertEquals("WORLD_NETHER", context.getValue().get("world"));
        assertEquals("-", context.getValue().get("entity"));
        assertEquals("15", context.getValue().get("ground"));
    }

    @Test
    void infoNeedsItsOwnPermission() {
        handler.onCommand(sender, null, "smashegg", new String[]{"info"});
        verify(plugin).message(sender, "no-permission");
        verify(plugin, never()).message(any(), eq("info"), any());
    }

    @Test
    void statsShowsCountersAndResetNeedsItsOwnPermission() {
        when(sender.hasPermission("smashegg.stats")).thenReturn(true);
        stats.recordUsed();
        stats.recordEffect("egg-break");

        handler.onCommand(sender, null, "smashegg", new String[]{"stats"});
        verify(plugin).message(eq(sender), eq("stats"), eq(Map.of(
                "used", "1", "broken", "1", "failed", "0", "denied", "0", "succeeded", "0")));

        handler.onCommand(sender, null, "smashegg", new String[]{"stats", "reset"});
        verify(plugin).message(sender, "no-permission");
        assertEquals(1, stats.get("used"));

        when(sender.hasPermission("smashegg.stats.reset")).thenReturn(true);
        handler.onCommand(sender, null, "smashegg", new String[]{"stats", "RESET"});
        verify(plugin).message(sender, "stats-reset");
        assertEquals(0, stats.get("used"));
        assertEquals(0, stats.get("broken"));
    }

    @Test
    void tabCompletionFollowsPermissions() {
        assertTrue(handler.onTabComplete(sender, null, "smashegg", new String[]{""}).isEmpty());

        when(sender.hasPermission("smashegg.info")).thenReturn(true);
        assertEquals(List.of("info"), handler.onTabComplete(sender, null, "smashegg", new String[]{""}));
        when(plugin.worldNames()).thenReturn(List.of("world", "world_nether"));
        assertEquals(List.of("world", "world_nether"),
                handler.onTabComplete(sender, null, "smashegg", new String[]{"info", "world"}));
        assertEquals(List.of("world_nether"),
                handler.onTabComplete(sender, null, "smashegg", new String[]{"info", "world_n"}));

        when(sender.hasPermission("smashegg.reload")).thenReturn(true);
        when(sender.hasPermission("smashegg.stats")).thenReturn(true);
        assertEquals(List.of("reload", "info", "stats"),
                handler.onTabComplete(sender, null, "smashegg", new String[]{""}));
        assertEquals(List.of("reload"), handler.onTabComplete(sender, null, "smashegg", new String[]{"RE"}));
        assertTrue(handler.onTabComplete(sender, null, "smashegg", new String[]{"reload", ""}).isEmpty());
        assertTrue(handler.onTabComplete(sender, null, "smashegg", new String[]{"stats", ""}).isEmpty());

        when(sender.hasPermission("smashegg.stats.reset")).thenReturn(true);
        assertEquals(List.of("reset"), handler.onTabComplete(sender, null, "smashegg", new String[]{"stats", "r"}));

        when(sender.hasPermission("smashegg.use")).thenReturn(false);
        assertTrue(handler.onTabComplete(sender, null, "smashegg", new String[]{""}).isEmpty());
    }
}
