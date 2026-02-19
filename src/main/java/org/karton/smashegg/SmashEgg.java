package org.karton.smashegg;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class SmashEgg extends JavaPlugin implements Listener {
    private BukkitAudiences adventure;
    private final Map<Material, EntityType> eggToEntityTypeMap = new HashMap<>();

    public BukkitAudiences adventure() {
        return this.adventure;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.adventure = BukkitAudiences.create(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        this.initializeEggToEntityTypeMap();
        this.getCommand("smashegg").setExecutor(new CommandHandler(this));

    }

    @Override
    public void onDisable() {
        if (this.adventure != null) this.adventure.close();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerUseEgg(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !(item.getItemMeta() instanceof SpawnEggMeta) || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        FileConfiguration config = this.getConfig();
        Random random = new Random();
        Material clickedBlockType = event.getClickedBlock().getType();

        if (clickedBlockType == Material.SPAWNER) {
            EntityType entityType = this.eggToEntityTypeMap.get(item.getType());
            if (entityType == null) return;

            if (config.getBoolean("settings.egg-break-on-spawner") &&
                    random.nextInt(100) < config.getInt("settings.egg-break-chance")) {

                this.playConfiguredSound(player, "sounds.egg-break", Sound.BLOCK_GLASS_BREAK);
                ColorUtil.sendMessage(this.adventure().player(player), config.getString("messages.egg-break"));
                item.setAmount(item.getAmount() - 1);
                event.setCancelled(true);
            }
        }

        else {
            int spawnChance = config.getInt("settings.ground-spawn-chance", 70);

            if (random.nextInt(100) >= spawnChance) {
                event.setCancelled(true);

                item.setAmount(item.getAmount() - 1);

                this.playConfiguredSound(player, "sounds.failure", Sound.ENTITY_VILLAGER_NO);
                ColorUtil.sendMessage(this.adventure().player(player), config.getString("messages.ground-failure"));
            }
        }
    }

    private void initializeEggToEntityTypeMap() {
        // Базовые мобы
        registerEgg("BAT_SPAWN_EGG", "BAT");
        registerEgg("BEE_SPAWN_EGG", "BEE");
        registerEgg("BLAZE_SPAWN_EGG", "BLAZE");
        registerEgg("CAT_SPAWN_EGG", "CAT");
        registerEgg("CAVE_SPIDER_SPAWN_EGG", "CAVE_SPIDER");
        registerEgg("CHICKEN_SPAWN_EGG", "CHICKEN");
        registerEgg("COD_SPAWN_EGG", "COD");
        registerEgg("COW_SPAWN_EGG", "COW");
        registerEgg("CREEPER_SPAWN_EGG", "CREEPER");
        registerEgg("DOLPHIN_SPAWN_EGG", "DOLPHIN");
        registerEgg("DONKEY_SPAWN_EGG", "DONKEY");
        registerEgg("DROWNED_SPAWN_EGG", "DROWNED");
        registerEgg("ELDER_GUARDIAN_SPAWN_EGG", "ELDER_GUARDIAN");
        registerEgg("ENDERMAN_SPAWN_EGG", "ENDERMAN");
        registerEgg("ENDERMITE_SPAWN_EGG", "ENDERMITE");
        registerEgg("EVOKER_SPAWN_EGG", "EVOKER");
        registerEgg("FOX_SPAWN_EGG", "FOX");
        registerEgg("GHAST_SPAWN_EGG", "GHAST");
        registerEgg("GUARDIAN_SPAWN_EGG", "GUARDIAN");
        registerEgg("HOGLIN_SPAWN_EGG", "HOGLIN");
        registerEgg("HORSE_SPAWN_EGG", "HORSE");
        registerEgg("HUSK_SPAWN_EGG", "HUSK");
        registerEgg("LLAMA_SPAWN_EGG", "LLAMA");
        registerEgg("MAGMA_CUBE_SPAWN_EGG", "MAGMA_CUBE");
        registerEgg("MOOSHROOM_SPAWN_EGG", "MUSHROOM_COW");
        registerEgg("MULE_SPAWN_EGG", "MULE");
        registerEgg("OCELOT_SPAWN_EGG", "OCELOT");
        registerEgg("PANDA_SPAWN_EGG", "PANDA");
        registerEgg("PARROT_SPAWN_EGG", "PARROT");
        registerEgg("PHANTOM_SPAWN_EGG", "PHANTOM");
        registerEgg("PIG_SPAWN_EGG", "PIG");
        registerEgg("PIGLIN_SPAWN_EGG", "PIGLIN");
        registerEgg("PIGLIN_BRUTE_SPAWN_EGG", "PIGLIN_BRUTE");
        registerEgg("PILLAGER_SPAWN_EGG", "PILLAGER");
        registerEgg("POLAR_BEAR_SPAWN_EGG", "POLAR_BEAR");
        registerEgg("PUFFERFISH_SPAWN_EGG", "PUFFERFISH");
        registerEgg("RABBIT_SPAWN_EGG", "RABBIT");
        registerEgg("RAVAGER_SPAWN_EGG", "RAVAGER");
        registerEgg("SALMON_SPAWN_EGG", "SALMON");
        registerEgg("SHEEP_SPAWN_EGG", "SHEEP");
        registerEgg("SHULKER_SPAWN_EGG", "SHULKER");
        registerEgg("SILVERFISH_SPAWN_EGG", "SILVERFISH");
        registerEgg("SKELETON_SPAWN_EGG", "SKELETON");
        registerEgg("SKELETON_HORSE_SPAWN_EGG", "SKELETON_HORSE");
        registerEgg("SLIME_SPAWN_EGG", "SLIME");
        registerEgg("SPIDER_SPAWN_EGG", "SPIDER");
        registerEgg("SQUID_SPAWN_EGG", "SQUID");
        registerEgg("STRAY_SPAWN_EGG", "STRAY");
        registerEgg("STRIDER_SPAWN_EGG", "STRIDER");
        registerEgg("TRADER_LLAMA_SPAWN_EGG", "TRADER_LLAMA");
        registerEgg("TROPICAL_FISH_SPAWN_EGG", "TROPICAL_FISH");
        registerEgg("TURTLE_SPAWN_EGG", "TURTLE");
        registerEgg("VEX_SPAWN_EGG", "VEX");
        registerEgg("VILLAGER_SPAWN_EGG", "VILLAGER");
        registerEgg("VINDICATOR_SPAWN_EGG", "VINDICATOR");
        registerEgg("WANDERING_TRADER_SPAWN_EGG", "WANDERING_TRADER");
        registerEgg("WITCH_SPAWN_EGG", "WITCH");
        registerEgg("WITHER_SKELETON_SPAWN_EGG", "WITHER_SKELETON");
        registerEgg("WOLF_SPAWN_EGG", "WOLF");
        registerEgg("ZOGLIN_SPAWN_EGG", "ZOGLIN");
        registerEgg("ZOMBIE_SPAWN_EGG", "ZOMBIE");
        registerEgg("ZOMBIE_HORSE_SPAWN_EGG", "ZOMBIE_HORSE");
        registerEgg("ZOMBIE_VILLAGER_SPAWN_EGG", "ZOMBIE_VILLAGER");
        registerEgg("ZOMBIFIED_PIGLIN_SPAWN_EGG", "ZOMBIFIED_PIGLIN");

        // 1.17+ Мобы
        registerEgg("AXOLOTL_SPAWN_EGG", "AXOLOTL");
        registerEgg("GLOW_SQUID_SPAWN_EGG", "GLOW_SQUID");
        registerEgg("GOAT_SPAWN_EGG", "GOAT");

        // 1.19+ Мобы
        registerEgg("ALLAY_SPAWN_EGG", "ALLAY");
        registerEgg("FROG_SPAWN_EGG", "FROG");
        registerEgg("TADPOLE_SPAWN_EGG", "TADPOLE");
        registerEgg("WARDEN_SPAWN_EGG", "WARDEN");

        // 1.20+ Мобы
        registerEgg("CAMEL_SPAWN_EGG", "CAMEL");
        registerEgg("SNIFFER_SPAWN_EGG", "SNIFFER");

        // 1.21 Мобы
        registerEgg("ARMADILLO_SPAWN_EGG", "ARMADILLO");
        registerEgg("BOGGED_SPAWN_EGG", "BOGGED");
        registerEgg("BREEZE_SPAWN_EGG", "BREEZE");
    }

    private void registerEgg(String materialName, String entityName) {
        try {
            this.eggToEntityTypeMap.put(Material.valueOf(materialName), EntityType.valueOf(entityName));
        } catch (Exception ignored) {}
    }

    private void playConfiguredSound(Player player, String path, Sound def) {
        try {
            Sound sound = Sound.valueOf(this.getConfig().getString(path, def.name()).toUpperCase());
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            player.getWorld().playSound(player.getLocation(), def, 1.0f, 1.0f);
        }
    }
}