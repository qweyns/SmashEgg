package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class EggListenerTest {
    private SmashEgg plugin;
    private Player player;
    private PlayerInventory inventory;
    private Block block;
    private IntSupplier roll;
    private EggListener listener;
    private YamlConfiguration config;
    private final List<Runnable> scheduled = new ArrayList<>();

    @BeforeEach
    void setup() {
        plugin = mock(SmashEgg.class);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        block = mock(Block.class);
        roll = mock(IntSupplier.class);
        when(roll.getAsInt()).thenReturn(99);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.hasPermission("smashegg.use")).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(player.isOnline()).thenReturn(true);
        when(block.getType()).thenReturn(Material.STONE);
        World world = mock(World.class);
        when(block.getWorld()).thenReturn(world);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        doAnswer(call -> {
            scheduled.add(call.getArgument(1, Runnable.class));
            return null;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        config = TestSupport.config();
        applyConfig();
        listener = new EggListener(plugin, roll);
    }

    private void applyConfig() {
        when(plugin.settings()).thenReturn(PluginSettings.load(config, ignored -> {}));
    }

    private PlayerInteractEvent event(EquipmentSlot hand, int amount) {
        // The event stack deliberately differs from the inventory stack, as it can on a server.
        ItemStack eventItem = new ItemStack(Material.ZOMBIE_SPAWN_EGG, amount);
        ItemStack held = mock(ItemStack.class);
        when(held.getAmount()).thenReturn(amount);
        when(held.isSimilar(eventItem)).thenReturn(true);
        when(held.clone()).thenReturn(new ItemStack(Material.ZOMBIE_SPAWN_EGG, amount));
        when(inventory.getItem(hand)).thenReturn(held);
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, eventItem, block, BlockFace.UP, hand);
    }

    private PlayerInteractEvent event() {
        return event(EquipmentSlot.HAND, 2);
    }

    private void nextTick() {
        List<Runnable> tasks = List.copyOf(scheduled);
        scheduled.clear();
        tasks.forEach(Runnable::run);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentSlot.class, names = {"HAND", "OFF_HAND"})
    void consumesExactlyOneEggFromTheCorrectHand(EquipmentSlot hand) {
        PlayerInteractEvent event = event(hand, 2);
        listener.onPlayerUseEgg(event);
        assertTrue(event.isCancelled());
        ArgumentCaptor<ItemStack> stack = ArgumentCaptor.forClass(ItemStack.class);
        verify(inventory).setItem(eq(hand), stack.capture());
        assertEquals(1, stack.getValue().getAmount());
        assertEquals(2, event.getItem().getAmount());
        verify(plugin).message(player, "ground-failure");
        verify(plugin).sound(player, "failure");
    }

    @Test
    void removesLastEggInsteadOfLeavingAZeroStack() {
        listener.onPlayerUseEgg(event(EquipmentSlot.OFF_HAND, 1));
        verify(inventory).setItem(EquipmentSlot.OFF_HAND, null);
    }

    @Test
    void doesNotConsumeTwiceWhenBothHandsFireInTheSameTick() {
        listener.onPlayerUseEgg(event(EquipmentSlot.HAND, 2));
        PlayerInteractEvent offhand = event(EquipmentSlot.OFF_HAND, 2);
        listener.onPlayerUseEgg(offhand);
        assertTrue(offhand.isCancelled());
        verify(inventory, never()).setItem(eq(EquipmentSlot.OFF_HAND), any());
        verify(plugin, times(1)).message(player, "ground-failure");
        verify(roll, times(1)).getAsInt();
        nextTick();
        listener.onPlayerUseEgg(event(EquipmentSlot.OFF_HAND, 2));
        verify(inventory).setItem(eq(EquipmentSlot.OFF_HAND), any());
    }

    @Test
    void blacklistTakesPriorityOverChanceAndDoesNotConsume() {
        when(block.getType()).thenReturn(Material.SPAWNER);
        config.set("settings.black-entities", List.of("zombie"));
        config.set("settings.egg-break-chance", 100);
        applyConfig();
        PlayerInteractEvent event = event();
        listener.onPlayerUseEgg(event);
        assertTrue(event.isCancelled());
        verify(plugin).message(player, "denied");
        verify(plugin).sound(player, "denied");
        verifyNoInteractions(roll);
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    @Test
    void blacklistAlsoAppliesToCreativeWithBypass() {
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        when(player.hasPermission("smashegg.bypass")).thenReturn(true);
        when(block.getType()).thenReturn(Material.SPAWNER);
        config.set("settings.black-entities", List.of("ZOMBIE"));
        applyConfig();
        PlayerInteractEvent event = event();
        listener.onPlayerUseEgg(event);
        assertTrue(event.isCancelled());
        verify(plugin).message(player, "denied");
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    @Test
    void blacklistDoesNotApplyToGroundSpawn() {
        config.set("settings.black-entities", List.of("ZOMBIE"));
        config.set("settings.ground-spawn-chance", 100);
        applyConfig();
        PlayerInteractEvent event = event();
        listener.onPlayerUseEgg(event);
        assertFalse(event.isCancelled());
        verify(plugin, never()).message(player, "denied");
    }

    @Test
    void spawnerBreakUsesSeparateChanceAndFeedback() {
        when(block.getType()).thenReturn(Material.SPAWNER);
        when(roll.getAsInt()).thenReturn(29);
        PlayerInteractEvent event = event();
        listener.onPlayerUseEgg(event);
        assertTrue(event.isCancelled());
        verify(plugin).message(player, "egg-break");
        verify(plugin).sound(player, "egg-break");
    }

    @Test
    void zeroAndHundredPercentHaveExactGroundSemantics() {
        config.set("settings.ground-spawn-chance", 0);
        applyConfig();
        when(roll.getAsInt()).thenReturn(0);
        PlayerInteractEvent failure = event();
        listener.onPlayerUseEgg(failure);
        assertTrue(failure.isCancelled());
        nextTick();
        config.set("settings.ground-spawn-chance", 100);
        applyConfig();
        when(roll.getAsInt()).thenReturn(99);
        PlayerInteractEvent success = event();
        listener.onPlayerUseEgg(success);
        assertFalse(success.isCancelled());
    }

    @Test
    void zeroAndHundredPercentHaveExactSpawnerSemantics() {
        when(block.getType()).thenReturn(Material.SPAWNER);
        config.set("settings.egg-break-chance", 0);
        applyConfig();
        when(roll.getAsInt()).thenReturn(0);
        PlayerInteractEvent success = event();
        listener.onPlayerUseEgg(success);
        assertFalse(success.isCancelled());
        config.set("settings.egg-break-chance", 100);
        applyConfig();
        when(roll.getAsInt()).thenReturn(99);
        PlayerInteractEvent failure = event();
        listener.onPlayerUseEgg(failure);
        assertTrue(failure.isCancelled());
    }

    @Test
    void disablingSpawnerBreakDoesNotDisableGroundBreak() {
        config.set("settings.egg-break-on-spawner", false);
        applyConfig();
        when(block.getType()).thenReturn(Material.SPAWNER);
        PlayerInteractEvent spawner = event();
        listener.onPlayerUseEgg(spawner);
        assertFalse(spawner.isCancelled());
        verifyNoInteractions(roll);
        when(block.getType()).thenReturn(Material.STONE);
        PlayerInteractEvent ground = event();
        listener.onPlayerUseEgg(ground);
        assertTrue(ground.isCancelled());
    }

    @Test
    void skipsCreativeByDefaultButCanOptIn() {
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        PlayerInteractEvent exempt = event();
        listener.onPlayerUseEgg(exempt);
        assertFalse(exempt.isCancelled());
        verifyNoInteractions(roll);
        config.set("settings.affect-creative", true);
        applyConfig();
        PlayerInteractEvent affected = event();
        listener.onPlayerUseEgg(affected);
        assertTrue(affected.isCancelled());
        verify(inventory).setItem(eq(EquipmentSlot.HAND), any());
    }

    @Test
    void bypassSkipsRandomChecks() {
        when(player.hasPermission("smashegg.bypass")).thenReturn(true);
        PlayerInteractEvent event = event();
        listener.onPlayerUseEgg(event);
        assertFalse(event.isCancelled());
        verifyNoInteractions(roll);
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    @Test
    void usePermissionIsRequiredEvenWithBypass() {
        when(player.hasPermission("smashegg.use")).thenReturn(false);
        when(player.hasPermission("smashegg.bypass")).thenReturn(true);
        PlayerInteractEvent event = event();
        listener.onPlayerUseEgg(event);
        assertTrue(event.isCancelled());
        verify(plugin).message(player, "no-permission");
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    @Test
    void ignoresChestOpeningButChecksSneakingUse() {
        when(block.getType()).thenReturn(Material.CHEST);
        PlayerInteractEvent opening = event();
        listener.onPlayerUseEgg(opening);
        assertFalse(opening.isCancelled());
        verifyNoInteractions(roll);
        when(player.isSneaking()).thenReturn(true);
        PlayerInteractEvent spawning = event();
        listener.onPlayerUseEgg(spawning);
        assertTrue(spawning.isCancelled());
    }

    @Test
    void respectsCancelledEventsAndBothIndependentDenials() {
        PlayerInteractEvent cancelled = event();
        cancelled.setCancelled(true);
        listener.onPlayerUseEgg(cancelled);
        PlayerInteractEvent itemDenied = event();
        itemDenied.setUseItemInHand(Event.Result.DENY);
        listener.onPlayerUseEgg(itemDenied);
        PlayerInteractEvent blockDenied = event();
        blockDenied.setUseInteractedBlock(Event.Result.DENY);
        listener.onPlayerUseEgg(blockDenied);
        verifyNoInteractions(roll);
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
        verify(plugin, never()).message(any(), any());
    }

    @Test
    void ignoresSpectatorAirLeftClicksAndNonEggs() {
        when(player.getGameMode()).thenReturn(GameMode.SPECTATOR);
        listener.onPlayerUseEgg(event());
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        listener.onPlayerUseEgg(new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.ZOMBIE_SPAWN_EGG), null, BlockFace.SELF, EquipmentSlot.HAND));
        listener.onPlayerUseEgg(new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK,
                new ItemStack(Material.ZOMBIE_SPAWN_EGG), block, BlockFace.UP, EquipmentSlot.HAND));
        listener.onPlayerUseEgg(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.EGG), block, BlockFace.UP, EquipmentSlot.HAND));
        listener.onPlayerUseEgg(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, block, BlockFace.UP, EquipmentSlot.HAND));
        verifyNoInteractions(roll);
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    private CreatureSpawner prepareSpawner() {
        when(block.getType()).thenReturn(Material.SPAWNER);
        config.set("settings.egg-break-on-spawner", false);
        applyConfig();
        CreatureSpawner spawner = mock(CreatureSpawner.class);
        when(spawner.getSpawnedType()).thenReturn(EntityType.PIG);
        when(block.getState()).thenReturn(spawner);
        return spawner;
    }

    @Test
    void successSoundWaitsForActualSpawnerChange() {
        CreatureSpawner spawner = prepareSpawner();
        listener.onPlayerUseEgg(event());
        verify(plugin, never()).sound(player, "success");
        when(spawner.getSpawnedType()).thenReturn(EntityType.ZOMBIE);
        nextTick();
        verify(plugin).sound(player, "success");
    }

    @Test
    void noSuccessSoundWhenVanillaDoesNotChangeSpawner() {
        prepareSpawner();
        listener.onPlayerUseEgg(event());
        nextTick();
        verify(plugin, never()).sound(player, "success");
    }

    @Test
    void noSuccessSoundIfAnotherPluginCancelsLater() {
        CreatureSpawner spawner = prepareSpawner();
        PlayerInteractEvent event = event();
        listener.onPlayerUseEgg(event);
        event.setCancelled(true);
        when(spawner.getSpawnedType()).thenReturn(EntityType.ZOMBIE);
        nextTick();
        verify(plugin, never()).sound(player, "success");
    }

    @Test
    void successCheckDoesNotLoadAnUnloadedChunk() {
        CreatureSpawner spawner = prepareSpawner();
        listener.onPlayerUseEgg(event());
        when(spawner.getSpawnedType()).thenReturn(EntityType.ZOMBIE);
        when(block.getWorld().isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        nextTick();
        verify(block, times(1)).getState();
        verify(plugin, never()).sound(player, "success");
    }
    @Test
    void emptySpawnerCanBeConfirmedWithoutNullPointerException() {
        CreatureSpawner spawner = prepareSpawner();
        when(spawner.getSpawnedType()).thenReturn(null);
        assertDoesNotThrow(() -> listener.onPlayerUseEgg(event()));
        when(spawner.getSpawnedType()).thenReturn(EntityType.ZOMBIE);
        nextTick();
        verify(plugin).sound(player, "success");
    }

    @Test
    void sameSpawnerTypeDoesNotScheduleSuccessSound() {
        CreatureSpawner spawner = prepareSpawner();
        when(spawner.getSpawnedType()).thenReturn(EntityType.ZOMBIE);
        listener.onPlayerUseEgg(event());
        assertTrue(scheduled.isEmpty());
        verify(plugin, never()).sound(player, "success");
    }

    @Test
    void doesNotRemoveAnItemReplacedByAnotherPlugin() {
        PlayerInteractEvent event = event();
        when(inventory.getItem(EquipmentSlot.HAND).isSimilar(event.getItem())).thenReturn(false);
        listener.onPlayerUseEgg(event);
        assertTrue(event.isCancelled());
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    @Test
    void chanceThresholdsAreNotOffByOne() {
        when(roll.getAsInt()).thenReturn(69);
        PlayerInteractEvent groundSuccess = event();
        listener.onPlayerUseEgg(groundSuccess);
        assertFalse(groundSuccess.isCancelled());
        when(roll.getAsInt()).thenReturn(70);
        PlayerInteractEvent groundFailure = event();
        listener.onPlayerUseEgg(groundFailure);
        assertTrue(groundFailure.isCancelled());
        nextTick();
        when(block.getType()).thenReturn(Material.SPAWNER);
        when(roll.getAsInt()).thenReturn(30);
        PlayerInteractEvent spawnerSuccess = event();
        listener.onPlayerUseEgg(spawnerSuccess);
        assertFalse(spawnerSuccess.isCancelled());
    }

}
