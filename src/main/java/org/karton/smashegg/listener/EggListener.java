package org.karton.smashegg.listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.karton.smashegg.SmashEgg;
import org.karton.smashegg.config.FailureAction;
import org.karton.smashegg.config.Gameplay;
import org.karton.smashegg.config.PlaceholderWords;
import org.karton.smashegg.config.PluginSettings;
import org.karton.smashegg.config.Rules;
import org.karton.smashegg.stats.PlayerProgress;
import org.karton.smashegg.util.EggTypes;

public final class EggListener implements Listener {
    static final NamespacedKey CHANGES = new NamespacedKey("smashegg", "changes");
    static final NamespacedKey LOCKED = new NamespacedKey("smashegg", "locked");

    private final SmashEgg plugin;
    private final IntSupplier roll;
    private final Predicate<Material> interactable;
    private final Set<UUID> coolingDown = new HashSet<>();

    public EggListener(SmashEgg plugin, IntSupplier roll) {
        this(plugin, roll, Material::isInteractable);
    }

    /**
     * @param interactable seam for tests: {@code Material#isInteractable()} resolves block data
     *                     through {@code Registry.BLOCK} and needs a running server
     */
    public EggListener(SmashEgg plugin, IntSupplier roll, Predicate<Material> interactable) {
        this.plugin = plugin;
        this.roll = roll;
        this.interactable = interactable;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        coolingDown.remove(event.getPlayer().getUniqueId());
        plugin.clearPreview(event.getPlayer().getUniqueId());
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
        if (coolingDown.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        PluginSettings settings = plugin.settings();
        String world = block.getWorld().getName();
        if (settings.isDisabled(world)) return;

        boolean spawner = block.getType() == Material.SPAWNER;
        if (!spawner && interactable.test(block.getType()) && !player.isSneaking()) return;

        String entity = EggTypes.fromMaterial(item.getType());
        Rules rules = settings.rulesFor(world, entity);
        Gameplay gameplay = settings.gameplay();
        int chance = spawner ? rules.breakChance() : rules.groundChance();
        Map<String, String> context = context(player, world, entity, hand, spawner, chance, settings.words());

        if (!player.hasPermission("smashegg.use")) {
            reject(event, "no-permission", context, settings, false);
            return;
        }
        plugin.stats().recordUsed();

        if (spawner && settings.blocksEntity(entity)) {
            reject(event, "denied", context, settings, false);
            return;
        }

        CreatureSpawner spawnerState = null;
        if (spawner && block.getState() instanceof CreatureSpawner state) {
            spawnerState = state;
            if (blockedByLimit(state, entity, gameplay)) {
                reject(event, "spawner-locked", context, settings, false);
                return;
            }
        }

        boolean bypass = player.hasPermission("smashegg.bypass")
                || (player.getGameMode() == GameMode.CREATIVE && !rules.affectCreative());

        Gameplay.Luck luck = gameplay.luckFor(player::hasPermission);
        chance = Gameplay.clamp(spawner
                ? rules.breakChance() + luck.breakDelta()
                : rules.groundChance() + luck.groundDelta());

        EquipmentSlot other = hand == EquipmentSlot.HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        Gameplay.Catalyst catalyst = catalyst(player, other, gameplay);
        if (catalyst != null) {
            chance = Gameplay.clamp(chance + (spawner ? catalyst.breakDelta() : catalyst.groundDelta()));
        }

        boolean allIn = allIn(player, hand, item, gameplay);
        if (allIn && !gameplay.allIn().guarantee()) {
            chance = Gameplay.clamp(chance + (spawner ? gameplay.allIn().breakDelta() : gameplay.allIn().groundDelta()));
        }
        context.put("chance", Integer.toString(chance));

        PlayerProgress progress = plugin.progress();
        UUID id = player.getUniqueId();
        boolean grace = !bypass && gameplay.grace().enabled() && progress.hasGrace(id, gameplay.grace().first());
        boolean pity = !bypass && gameplay.pity().enabled()
                && progress.isReady(id, entity, gameplay.pity().perEntity(), gameplay.pity().after());

        boolean failure = !bypass && !grace && !pity && !(allIn && gameplay.allIn().guarantee()) && (spawner
                ? rules.breakOnSpawner() && roll.getAsInt() < chance
                : roll.getAsInt() >= chance);

        if (allIn && gameplay.allIn().payAlways()) take(player, hand, item, gameplay.allIn().extra());
        if (catalyst != null && catalyst.consume()) take(player, other, player.getInventory().getItem(other), 1);
        if (grace) progress.consumeGrace(id);

        if (failure) {
            if (gameplay.pity().enabled()) progress.recordFail(id, entity, gameplay.pity().perEntity());
            boolean critical = gameplay.criticalFail().enabled()
                    && gameplay.criticalFail().chance() > 0
                    && roll.getAsInt() < gameplay.criticalFail().chance();
            reject(event, spawner ? "egg-break" : "ground-failure", context, settings, true);
            if (critical) criticalFail(player, gameplay.criticalFail(), context);
            consolation(player, gameplay.consolation());
            return;
        }

        if (gameplay.pity().enabled()) progress.recordSuccess(id, entity, gameplay.pity().perEntity());
        if (allIn && !gameplay.allIn().payAlways()) take(player, hand, item, gameplay.allIn().extra());
        if (allIn) plugin.effect(player, "all-in", context);
        if (catalyst != null) plugin.effect(player, "catalyst", context);
        if (spawner && spawnerState != null) {
            confirmSpawnerChange(event, block, entity, context, gameplay, spawnerState);
        }
    }

    private boolean blockedByLimit(CreatureSpawner state, String entity, Gameplay gameplay) {
        if (matchesEntity(state, entity)) return false;
        if (gameplay.spawnerRisk().enabled()
                && Byte.valueOf((byte) 1).equals(state.getPersistentDataContainer().get(LOCKED, PersistentDataType.BYTE))) {
            return true;
        }
        if (!gameplay.changeLimit().enabled()) return false;
        Integer changes = state.getPersistentDataContainer().get(CHANGES, PersistentDataType.INTEGER);
        return changes != null && changes >= gameplay.changeLimit().max();
    }

    private Gameplay.Catalyst catalyst(Player player, EquipmentSlot other, Gameplay gameplay) {
        if (!gameplay.catalystsEnabled()) return null;
        ItemStack stack = player.getInventory().getItem(other);
        if (stack == null || stack.getAmount() <= 0) return null;
        return gameplay.catalysts().get(stack.getType().name());
    }

    private boolean allIn(Player player, EquipmentSlot hand, ItemStack item, Gameplay gameplay) {
        Gameplay.AllIn allIn = gameplay.allIn();
        if (!allIn.enabled()) return false;
        if (allIn.sneak() && !player.isSneaking()) return false;
        ItemStack held = player.getInventory().getItem(hand);
        int need = 1 + allIn.extra();
        return held != null && held.isSimilar(item) && held.getAmount() >= need;
    }

    private void reject(PlayerInteractEvent event, String effect, Map<String, String> context,
                        PluginSettings settings, boolean consume) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        startCooldown(player, settings.cooldownTicks(), context);
        if (consume) applyFailureAction(player, event, settings.failureAction());
        report(player, effect, context);
    }

    private void startCooldown(Player player, int ticks, Map<String, String> context) {
        UUID id = player.getUniqueId();
        if (ticks <= 0 || !coolingDown.add(id)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> coolingDown.remove(id), ticks);
        if (plugin.settings().gameplay().cooldownDisplay().enabled() && ticks > 1) {
            context.put("ticks", Integer.toString(ticks));
            plugin.message(player, "cooldown", context);
        }
    }

    private void applyFailureAction(Player player, PlayerInteractEvent event, FailureAction action) {
        if (action == FailureAction.KEEP) return;
        if (!take(player, event.getHand(), event.getItem(), 1)) return;
        if (action == FailureAction.DROP) {
            ItemStack dropped = event.getItem().clone();
            dropped.setAmount(1);
            player.getWorld().dropItem(player.getLocation(), dropped);
        }
    }

    private boolean take(Player player, EquipmentSlot slot, ItemStack expected, int amount) {
        if (amount <= 0 || expected == null) return true;
        PlayerInventory inventory = player.getInventory();
        ItemStack held = inventory.getItem(slot);
        if (held == null || held.getAmount() < amount) return false;
        if (held != expected && !held.isSimilar(expected)) return false;
        int left = held.getAmount() - amount;
        ItemStack remaining = held.clone();
        remaining.setAmount(left);
        inventory.setItem(slot, left == 0 ? null : remaining);
        return true;
    }

    private void criticalFail(Player player, Gameplay.CriticalFail critical, Map<String, String> context) {
        if (critical.knockback() > 0) {
            Vector direction = player.getLocation().getDirection();
            if (direction != null) {
                player.setVelocity(direction.multiply(-critical.knockback()).setY(0.25));
            }
        }
        if (!critical.potion().isEmpty()) {
            try {
                PotionEffectType type = PotionEffectType.getByName(critical.potion());
                if (type != null) {
                    player.addPotionEffect(new PotionEffect(type, critical.potionTicks(), critical.potionAmplifier()));
                }
            } catch (LinkageError | RuntimeException ignored) {
                // Potion registry is not available in unit tests.
            }
        }
        report(player, "critical-fail", context);
    }

    private void consolation(Player player, Gameplay.Consolation consolation) {
        if (!consolation.enabled() || consolation.chance() <= 0) return;
        if (consolation.chance() < 100 && roll.getAsInt() >= consolation.chance()) return;
        ItemStack stack = plugin.item(consolation.material(), consolation.amount());
        if (stack != null) player.getWorld().dropItem(player.getLocation(), stack);
        plugin.effect(player, "consolation", Map.of("player", player.getName()));
    }

    private void report(Player player, String effect, Map<String, String> context) {
        plugin.effect(player, effect, context);
        plugin.stats().recordEffect(effect);
        plugin.logEvent(effect, context);
    }

    private static Map<String, String> context(Player player, String world, String entity, EquipmentSlot hand,
                                               boolean spawner, int chance, PlaceholderWords words) {
        Map<String, String> values = new HashMap<>(8);
        values.put("player", player.getName());
        values.put("world", world);
        values.put("entity", entity);
        values.put("chance", Integer.toString(chance));
        values.put("hand", words.hand(hand == EquipmentSlot.OFF_HAND));
        values.put("mode", words.mode(spawner));
        return values;
    }

    private static boolean matchesEntity(CreatureSpawner spawner, String entity) {
        return spawner.getSpawnedType() != null
                && EggTypes.normalize(spawner.getSpawnedType().name()).equals(entity);
    }

    private void confirmSpawnerChange(PlayerInteractEvent event, Block block, String expectedEntity,
                                      Map<String, String> context, Gameplay gameplay, CreatureSpawner before) {
        if (matchesEntity(before, expectedEntity)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || event.useItemInHand() == Event.Result.DENY
                    || event.useInteractedBlock() == Event.Result.DENY || !event.getPlayer().isOnline()
                    || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return;
            if (block.getState() instanceof CreatureSpawner after
                    && matchesEntity(after, expectedEntity)) {
                Player player = event.getPlayer();
                markChange(after, gameplay);
                report(player, "success", context);
                plugin.announce(player, context);
                spawnerRisk(player, after, gameplay, context);
            }
        });
    }

    private void markChange(CreatureSpawner spawner, Gameplay gameplay) {
        if (!gameplay.changeLimit().enabled()) return;
        Integer current = spawner.getPersistentDataContainer().get(CHANGES, PersistentDataType.INTEGER);
        int next = (current == null ? 0 : current) + 1;
        spawner.getPersistentDataContainer().set(CHANGES, PersistentDataType.INTEGER, next);
        spawner.update();
    }

    private void spawnerRisk(Player player, CreatureSpawner spawner, Gameplay gameplay, Map<String, String> context) {
        Gameplay.SpawnerRisk risk = gameplay.spawnerRisk();
        if (!risk.enabled() || risk.chance() <= 0 || roll.getAsInt() >= risk.chance()) return;
        switch (risk.action()) {
            case "lock" -> {
                spawner.getPersistentDataContainer().set(LOCKED, PersistentDataType.BYTE, (byte) 1);
                spawner.update();
            }
            case "reset" -> {
                if (!risk.resetTo().isEmpty()) {
                    try {
                        spawner.setSpawnedType(EntityType.valueOf(risk.resetTo()));
                        spawner.update();
                    } catch (IllegalArgumentException | LinkageError ignored) {
                        return;
                    }
                }
            }
            default -> { return; }
        }
        report(player, "spawner-risk", context);
    }
}
