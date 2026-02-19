package org.karton.smashegg;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final SmashEgg plugin;

    public CommandHandler(SmashEgg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Audience audience = this.plugin.adventure().sender(sender);
        FileConfiguration config = this.plugin.getConfig();

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            ColorUtil.sendMessage(audience, config.getString("messages.usage", "<red>Использование: /smashegg reload"));
            return true;
        }
        
        if (sender.hasPermission("smashegg.reload")) {
            this.plugin.reloadConfig();
            ColorUtil.sendMessage(audience, this.plugin.getConfig().getString("messages.reload-success", "<green>Конфигурация успешно перезагружена!"));
        } else {
            ColorUtil.sendMessage(audience, config.getString("messages.no-permission", "<red>У вас нет прав для этого."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase())) {
            completions.add("reload");
        }
        return completions;
    }
}