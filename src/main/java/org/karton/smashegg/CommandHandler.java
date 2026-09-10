package org.karton.smashegg;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class CommandHandler implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("reload", "info", "stats");
    /** Shown by {@code /info} when a world or an entity was not given; matches no override. */
    private static final String NO_ARGUMENT = "-";
    private final SmashEgg plugin;

    public CommandHandler(SmashEgg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smashegg.use")) {
            plugin.message(sender, "no-permission");
        } else if (args.length == 0) {
            plugin.message(sender, "usage");
        } else {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> reload(sender);
                case "info" -> info(sender, args);
                case "stats" -> stats(sender, args);
                default -> plugin.message(sender, "unknown");
            }
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("smashegg.reload")) {
            plugin.message(sender, "no-permission");
            return;
        }
        plugin.message(sender, plugin.reloadSettings() ? "reload-success" : "reload-failure");
    }

    /**
     * Shows the rules that actually apply, so "why did my egg break?" is answerable in game.
     * Accepts {@code info}, {@code info <world>}, {@code info <entity>} and {@code info <world> <entity>}.
     */
    private void info(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smashegg.info")) {
            plugin.message(sender, "no-permission");
            return;
        }
        PluginSettings settings = plugin.settings();
        String world = senderWorld(sender);
        String entity = NO_ARGUMENT;
        if (args.length >= 3) {
            world = args[1];
            entity = EggTypes.normalize(args[2]);
        } else if (args.length == 2) {
            // A single argument is a world when a loaded world has that name, an entity otherwise.
            if (matchesWorld(args[1])) world = args[1];
            else entity = EggTypes.normalize(args[1]);
        }
        Rules rules = settings.rulesFor(world, entity);
        plugin.message(sender, "info", Map.of(
                "version", plugin.version(),
                "world", world,
                "entity", entity,
                "spawner", rules.breakOnSpawner() ? "on" : "off",
                "break", Integer.toString(rules.breakChance()),
                "ground", Integer.toString(rules.groundChance()),
                "creative", rules.affectCreative() ? "yes" : "no",
                "filter", settings.filterMode().name().toLowerCase(Locale.ROOT),
                "cooldown", Integer.toString(settings.cooldownTicks())));
    }

    /** The world the sender stands in; the console has none, so it gets {@link #NO_ARGUMENT}. */
    private static String senderWorld(CommandSender sender) {
        return sender instanceof Player player ? player.getWorld().getName() : NO_ARGUMENT;
    }

    private boolean matchesWorld(String name) {
        return plugin.worldNames().stream().anyMatch(world -> world.equalsIgnoreCase(name));
    }

    private void stats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smashegg.stats")) {
            plugin.message(sender, "no-permission");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("smashegg.stats.reset")) {
                plugin.message(sender, "no-permission");
                return;
            }
            plugin.stats().reset();
            plugin.message(sender, "stats-reset");
            return;
        }
        plugin.message(sender, "stats", plugin.stats().placeholders());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smashegg.use")) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                    .filter(name -> sender.hasPermission(permission(name)))
                    .filter(name -> name.startsWith(prefix))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")
                && sender.hasPermission("smashegg.stats.reset")
                && "reset".startsWith(args[1].toLowerCase(Locale.ROOT))) {
            return List.of("reset");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")
                && sender.hasPermission("smashegg.info")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.worldNames().stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    private static String permission(String subcommand) {
        return switch (subcommand) {
            case "reload" -> "smashegg.reload";
            case "info" -> "smashegg.info";
            case "stats" -> "smashegg.stats";
            default -> "smashegg.use";
        };
    }
}
