package net.Indyuce.moarbows.util;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AmmoTest {
    @Test
    void insufficientAmmunitionDoesNotPartiallyConsumeOrTouchCustomItems() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        ItemStack request = mock(ItemStack.class);
        when(request.getAmount()).thenReturn(2);
        ItemStack plain = mock(ItemStack.class);
        when(plain.isSimilar(request)).thenReturn(true);
        when(plain.getAmount()).thenReturn(1);
        ItemStack custom = mock(ItemStack.class);
        when(custom.isSimilar(request)).thenReturn(false);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{plain, custom});
        when(inventory.getItemInOffHand()).thenReturn(mock(ItemStack.class));
        assertFalse(UtilityMethods.consumeAmmo(player, request));
        verify(inventory, never()).setItem(anyInt(), any());
        verify(inventory, never()).setItemInOffHand(any());
    }

    @Test
    void offhandAmmunitionIsConsumedWithoutReplacingUnrelatedSlots() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        ItemStack request = mock(ItemStack.class);
        when(request.getAmount()).thenReturn(1);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
        ItemStack offhand = mock(ItemStack.class), remainder = mock(ItemStack.class);
        when(offhand.isSimilar(request)).thenReturn(true);
        when(offhand.getAmount()).thenReturn(2);
        when(offhand.clone()).thenReturn(remainder);
        when(remainder.getAmount()).thenReturn(1);
        when(inventory.getItemInOffHand()).thenReturn(offhand);
        assertTrue(UtilityMethods.consumeAmmo(player, request));
        verify(remainder).setAmount(1);
        verify(inventory).setItemInOffHand(remainder);
        verify(inventory, never()).setItem(anyInt(), any());
    }

    @Test
    void spectatorsCannotSupplyAmmunitionButCreativeDoesNotConsume() {
        Player player = mock(Player.class);
        ItemStack request = mock(ItemStack.class);
        when(request.getAmount()).thenReturn(1);
        when(player.getGameMode()).thenReturn(GameMode.SPECTATOR);
        assertFalse(UtilityMethods.consumeAmmo(player, request));
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        assertTrue(UtilityMethods.consumeAmmo(player, request));
        verify(player, never()).getInventory();
    }
}
