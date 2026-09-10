package org.karton.smashegg.gameplay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.karton.smashegg.SmashEgg;
import org.karton.smashegg.config.Gameplay;
import org.karton.smashegg.config.PlaceholderWords;
import org.karton.smashegg.config.PluginSettings;
import org.karton.smashegg.config.Rules;
import org.karton.smashegg.util.EggTypes;

/**
 * Periodic look-ahead of the chance that would apply to the block in front of the player.
 * Runs only when {@code gameplay.preview} is enabled; identical repeats are not re-sent.
 */
public final class PreviewTask implements Runnable {
    private final SmashEgg plugin;
    private final ConcurrentHashMap<UUID, String> last = new ConcurrentHashMap<>();

    public PreviewTask(SmashEgg plugin) {
        this.plugin = plugin;
    }

    public void clear(UUID player) {
        last.remove(player);
    }

    @Override
    public void run() {
        PluginSettings settings = plugin.settings();
        if (settings == null) return;
        Gameplay.Preview preview = settings.gameplay().preview();
        if (!preview.enabled()) return;
        for (Player player : plugin.onlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR || !player.hasPermission("smashegg.use")) continue;
            ItemStack item = eggInHand(player);
            if (item == null) {
                last.remove(player.getUniqueId());
                continue;
            }
            Block target = player.getTargetBlockExact(preview.reach(), FluidCollisionMode.NEVER);
            if (target == null) {
                last.remove(player.getUniqueId());
                continue;
            }
            String world = target.getWorld().getName();
            if (settings.isDisabled(world)) continue;
            boolean spawner = target.getType() == Material.SPAWNER;
            String entity = EggTypes.fromMaterial(item.getType());
            Rules rules = settings.rulesFor(world, entity);
            Gameplay.Luck luck = settings.gameplay().luckFor(player::hasPermission);
            int chance = Gameplay.clamp(spawner
                    ? rules.breakChance() + luck.breakDelta()
                    : rules.groundChance() + luck.groundDelta());
            PlaceholderWords words = settings.words();
            String signature = world + '|' + entity + '|' + spawner + '|' + chance;
            if (signature.equals(last.put(player.getUniqueId(), signature))) continue;
            boolean offHand = player.getInventory().getItemInOffHand() == item;
            plugin.message(player, "preview", Map.of(
                    "player", player.getName(),
                    "world", world,
                    "entity", entity,
                    "chance", Integer.toString(chance),
                    "mode", words.mode(spawner),
                    "hand", words.hand(offHand)));
        }
    }

    private static ItemStack eggInHand(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && EggTypes.isEgg(main.getType())) return main;
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && EggTypes.isEgg(off.getType())) return off;
        return null;
    }
}
