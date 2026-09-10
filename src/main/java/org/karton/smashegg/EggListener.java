package org.karton.smashegg;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class EggListener implements Listener {
    private final SmashEgg plugin;
    private final IntSupplier roll;
    private final Set<UUID> blockedThisTick = new HashSet<>();

    EggListener(SmashEgg plugin, IntSupplier roll) {
        this.plugin = plugin;
        this.roll = roll;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerUseEgg(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand();
        if (event.isCancelled() || event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || item == null
                || item.getAmount() <= 0 || !EggTypes.isEgg(item.getType())
                || (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND)
                || event.useItemInHand() == Event.Result.DENY
                || event.useInteractedBlock() == Event.Result.DENY) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        // Cancelling the main-hand action must not charge a second egg from the off hand.
        if (blockedThisTick.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        boolean spawner = block.getType() == Material.SPAWNER;
        // Be conservative: opening a chest/door/etc. is not an attempt to use an egg.
        if (!spawner && block.getType().isInteractable() && !player.isSneaking()) return;

        if (!player.hasPermission("smashegg.use")) {
            reject(event, "no-permission", "denied", false);
            return;
        }

        PluginSettings settings = plugin.settings();
        String entity = EggTypes.fromMaterial(item.getType());
        if (spawner && settings.blacklist().contains(entity)) {
            reject(event, "denied", "denied", false);
            return;
        }

        boolean bypass = player.hasPermission("smashegg.bypass")
                || (player.getGameMode() == GameMode.CREATIVE && !settings.affectCreative());
        boolean failure = !bypass && (spawner
                ? settings.breakOnSpawner() && roll.getAsInt() < settings.breakChance()
                : roll.getAsInt() >= settings.groundChance());
        if (failure) {
            reject(event, spawner ? "egg-break" : "ground-failure", spawner ? "egg-break" : "failure", true);
        } else if (spawner) {
            confirmSpawnerChange(event, block, entity);
        }
    }

    private void reject(PlayerInteractEvent event, String message, String sound, boolean consume) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (blockedThisTick.add(id)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> blockedThisTick.remove(id));
        }
        if (consume) {
            PlayerInventory inventory = player.getInventory();
            ItemStack held = inventory.getItem(event.getHand());
            // Use the inventory slot, not event.getItem(): the event stack can be a copy.
            if (held != null && held.isSimilar(event.getItem()) && held.getAmount() > 0) {
                ItemStack remaining = held.clone();
                remaining.setAmount(held.getAmount() - 1);
                inventory.setItem(event.getHand(), remaining.getAmount() == 0 ? null : remaining);
            }
        }
        plugin.message(player, message);
        plugin.sound(player, sound);
    }

    private static boolean matchesEntity(CreatureSpawner spawner, String entity) {
        // Modern servers may represent an empty spawner with a null spawned type.
        return spawner.getSpawnedType() != null
                && EggTypes.normalize(spawner.getSpawnedType().name()).equals(entity);
    }

    private void confirmSpawnerChange(PlayerInteractEvent event, Block block, String expectedEntity) {
        if (!(block.getState() instanceof CreatureSpawner before)
                || matchesEntity(before, expectedEntity)) return;
        // Vanilla changes the spawner after the event. Do not claim success before that happens.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || event.useItemInHand() == Event.Result.DENY
                    || event.useInteractedBlock() == Event.Result.DENY || !event.getPlayer().isOnline()
                    || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return;
            if (block.getState() instanceof CreatureSpawner after
                    && matchesEntity(after, expectedEntity)) {
                plugin.sound(event.getPlayer(), "success");
            }
        });
    }
}
