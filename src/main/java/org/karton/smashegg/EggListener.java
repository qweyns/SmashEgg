package org.karton.smashegg;

import java.util.HashSet;
import java.util.Map;
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
    private final Set<UUID> coolingDown = new HashSet<>();

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
        if (coolingDown.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        PluginSettings settings = plugin.settings();
        String world = block.getWorld().getName();
        if (settings.isDisabled(world)) return;

        boolean spawner = block.getType() == Material.SPAWNER;
        // Be conservative: opening a chest/door/etc. is not an attempt to use an egg.
        if (!spawner && block.getType().isInteractable() && !player.isSneaking()) return;

        String entity = EggTypes.fromMaterial(item.getType());
        Rules rules = settings.rulesFor(world, entity);
        Map<String, String> context = context(player, world, entity, hand, spawner,
                spawner ? rules.breakChance() : rules.groundChance());

        if (!player.hasPermission("smashegg.use")) {
            reject(event, "no-permission", context, settings, false);
            return;
        }
        plugin.stats().recordUsed();

        if (spawner && settings.blocksEntity(entity)) {
            reject(event, "denied", context, settings, false);
            return;
        }

        boolean bypass = player.hasPermission("smashegg.bypass")
                || (player.getGameMode() == GameMode.CREATIVE && !rules.affectCreative());
        boolean failure = !bypass && (spawner
                ? rules.breakOnSpawner() && roll.getAsInt() < rules.breakChance()
                : roll.getAsInt() >= rules.groundChance());
        if (failure) {
            reject(event, spawner ? "egg-break" : "ground-failure", context, settings, true);
        } else if (spawner) {
            confirmSpawnerChange(event, block, entity, context);
        }
    }

    private void reject(PlayerInteractEvent event, String effect, Map<String, String> context,
                        PluginSettings settings, boolean consume) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        startCooldown(player, settings.cooldownTicks());
        if (consume) applyFailureAction(player, event, settings.failureAction());
        report(player, effect, context);
    }

    /** Blocks further egg processing for this player for the configured cooldown. */
    private void startCooldown(Player player, int ticks) {
        UUID id = player.getUniqueId();
        if (ticks <= 0 || !coolingDown.add(id)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> coolingDown.remove(id), ticks);
    }

    private void applyFailureAction(Player player, PlayerInteractEvent event, FailureAction action) {
        if (action == FailureAction.KEEP) return;
        PlayerInventory inventory = player.getInventory();
        ItemStack held = inventory.getItem(event.getHand());
        // Use the inventory slot, not event.getItem(): the event stack can be a copy.
        if (held == null || !held.isSimilar(event.getItem()) || held.getAmount() <= 0) return;
        ItemStack remaining = held.clone();
        remaining.setAmount(held.getAmount() - 1);
        inventory.setItem(event.getHand(), remaining.getAmount() == 0 ? null : remaining);
        if (action == FailureAction.DROP) {
            ItemStack dropped = event.getItem().clone();
            dropped.setAmount(1);
            player.getWorld().dropItem(player.getLocation(), dropped);
        }
    }

    private void report(Player player, String effect, Map<String, String> context) {
        plugin.effect(player, effect, context);
        plugin.stats().recordEffect(effect);
        plugin.logEvent(effect, context);
    }

    private static Map<String, String> context(Player player, String world, String entity, EquipmentSlot hand,
                                               boolean spawner, int chance) {
        return Map.of(
                "player", player.getName(),
                "world", world,
                "entity", entity,
                "chance", Integer.toString(chance),
                "hand", hand == EquipmentSlot.OFF_HAND ? "off" : "main",
                "mode", spawner ? "spawner" : "ground");
    }

    private static boolean matchesEntity(CreatureSpawner spawner, String entity) {
        // Modern servers may represent an empty spawner with a null spawned type.
        return spawner.getSpawnedType() != null
                && EggTypes.normalize(spawner.getSpawnedType().name()).equals(entity);
    }

    private void confirmSpawnerChange(PlayerInteractEvent event, Block block, String expectedEntity,
                                      Map<String, String> context) {
        if (!(block.getState() instanceof CreatureSpawner before)
                || matchesEntity(before, expectedEntity)) return;
        // Vanilla changes the spawner after the event. Do not claim success before that happens.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || event.useItemInHand() == Event.Result.DENY
                    || event.useInteractedBlock() == Event.Result.DENY || !event.getPlayer().isOnline()
                    || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return;
            if (block.getState() instanceof CreatureSpawner after
                    && matchesEntity(after, expectedEntity)) {
                report(event.getPlayer(), "success", context);
            }
        });
    }
}
