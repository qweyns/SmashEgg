package org.karton.smashegg;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class CommandHandler implements CommandExecutor, TabCompleter {
    private final SmashEgg plugin;

    public CommandHandler(SmashEgg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smashegg.use")) {
            plugin.message(sender, "no-permission");
        } else if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            plugin.message(sender, "usage");
        } else if (!sender.hasPermission("smashegg.reload")) {
            plugin.message(sender, "no-permission");
        } else {
            plugin.message(sender, plugin.reloadSettings() ? "reload-success" : "reload-failure");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("smashegg.use") && sender.hasPermission("smashegg.reload")
                && args.length == 1 && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("reload");
        }
        return List.of();
    }
}
