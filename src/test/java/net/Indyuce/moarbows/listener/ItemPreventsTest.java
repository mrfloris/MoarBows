package net.Indyuce.moarbows.listener;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.manager.BowManager;
import net.Indyuce.moarbows.manager.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemPreventsTest {
    private BowManager manager;
    private Player player;
    private Inventory inventory;
    private ItemPrevents listener;
    private MoarBow bow;
    private ItemStack first, second;

    @BeforeEach
    void setup() {
        MoarBows.plugin = mock(MoarBows.class);
        manager = mock(BowManager.class);
        when(manager.getBows()).thenReturn(List.of());
        when(MoarBows.plugin.getBowManager()).thenReturn(manager);
        ConfigManager config = new ConfigManager(new ConfigManager.Snapshot(new YamlConfiguration(), new YamlConfiguration(), new YamlConfiguration()));
        when(MoarBows.plugin.getLanguage()).thenReturn(config);
        bow = mock(MoarBow.class);
        player = mock(Player.class);
        inventory = mock(Inventory.class);
        first = mock(ItemStack.class);
        second = mock(ItemStack.class);
        when(inventory.getItem(0)).thenReturn(first);
        when(inventory.getItem(1)).thenReturn(second);
        listener = new ItemPrevents();
    }

    @AfterEach
    void cleanup() { MoarBows.plugin = null; }

    @Test
    void customSecondInputCannotBypassAnvilRestriction() {
        when(manager.get(second)).thenReturn(bow);
        assertTrue(listener.denyAnvil(player, inventory, null));
    }

    @Test
    void repairPermissionIsRevalidatedAtResultTake() {
        when(manager.get(first)).thenReturn(bow);
        when(player.hasPermission("moarbows.anvil")).thenReturn(true);
        assertTrue(listener.denyAnvil(player, inventory, null));
    }

    @Test
    void differentBowIdentitiesCannotBeCombinedEvenByAnvilOperators() {
        when(manager.get(first)).thenReturn(mock(MoarBow.class));
        when(manager.get(second)).thenReturn(bow);
        when(player.hasPermission("moarbows.anvil")).thenReturn(true);
        when(player.hasPermission("moarbows.repair")).thenReturn(true);
        assertTrue(listener.denyAnvil(player, inventory, null));
    }

    @Test
    void sameBowWithMatchingLevelCanBeRepairedByAuthorizedPlayer() {
        when(manager.get(first)).thenReturn(bow);
        when(manager.get(second)).thenReturn(bow);
        when(manager.getLevel(first)).thenReturn(3);
        when(manager.getLevel(second)).thenReturn(3);
        when(player.hasPermission("moarbows.anvil")).thenReturn(true);
        when(player.hasPermission("moarbows.repair")).thenReturn(true);
        assertFalse(listener.denyAnvil(player, inventory, null));
    }

    @Test
    void differentLevelsCannotBeCombined() {
        when(manager.get(first)).thenReturn(bow);
        when(manager.get(second)).thenReturn(bow);
        when(manager.getLevel(first)).thenReturn(1);
        when(manager.getLevel(second)).thenReturn(3);
        when(player.hasPermission("moarbows.anvil")).thenReturn(true);
        when(player.hasPermission("moarbows.repair")).thenReturn(true);
        assertTrue(listener.denyAnvil(player, inventory, null));
    }

    @Test
    void normalAnvilOperationsAreUnaffected() {
        assertFalse(listener.denyAnvil(player, inventory, null));
    }

    @Test
    void newAnvilEnchantmentsRequireEnchantPermission() {
        when(manager.get(first)).thenReturn(bow);
        when(player.hasPermission("moarbows.anvil")).thenReturn(true);
        when(player.hasPermission("moarbows.repair")).thenReturn(true);
        ItemStack result = mock(ItemStack.class);
        // The policy compares enchantment levels; a key placeholder keeps this
        // unit test independent of the server's live enchantment registry.
        when(result.getEnchantments()).thenReturn(java.util.Collections.singletonMap(null, 2));
        when(first.getEnchantments()).thenReturn(java.util.Collections.singletonMap(null, 1));
        assertTrue(listener.denyAnvil(player, inventory, result));
        when(player.hasPermission("moarbows.enchant")).thenReturn(true);
        assertFalse(listener.denyAnvil(player, inventory, result));
    }

    @Test
    void existingEnchantmentsArePreservedWithoutNewEnchantPermission() {
        when(manager.get(first)).thenReturn(bow);
        when(player.hasPermission("moarbows.anvil")).thenReturn(true);
        when(player.hasPermission("moarbows.repair")).thenReturn(true);
        ItemStack result = mock(ItemStack.class);
        when(result.getEnchantments()).thenReturn(java.util.Collections.singletonMap(null, 2));
        when(first.getEnchantments()).thenReturn(java.util.Collections.singletonMap(null, 2));
        assertFalse(listener.denyAnvil(player, inventory, result));
    }
}
