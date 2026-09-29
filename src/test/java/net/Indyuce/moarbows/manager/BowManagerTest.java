package net.Indyuce.moarbows.manager;

import net.Indyuce.moarbows.bow.MoarBow;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BowManagerTest {
    private BowManager manager;
    private MoarBow bow;
    private ItemStack item;
    private PersistentDataContainer data;

    @BeforeEach
    void setup() {
        manager = new BowManager();
        bow = mock(MoarBow.class);
        when(bow.getId()).thenReturn("FIRE_BOW");
        manager.register(bow);
        item = mock(ItemStack.class);
        data = mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(Material.BOW);
        when(item.getAmount()).thenReturn(1);
        when(item.getPersistentDataContainer()).thenReturn(data);
    }

    private void validTag() {
        when(data.has(BowManager.BOW_KEY)).thenReturn(true);
        when(data.has(BowManager.BOW_KEY, PersistentDataType.STRING)).thenReturn(true);
        when(data.get(BowManager.BOW_KEY, PersistentDataType.STRING)).thenReturn("FIRE_BOW");
        when(data.getOrDefault(BowManager.LEVEL_KEY, PersistentDataType.INTEGER, 1)).thenReturn(1);
    }

    @Test
    void cosmeticNameAndLoreCannotCreateABow() {
        when(item.hasItemMeta()).thenReturn(true);
        assertNull(manager.get(item));
        assertEquals(0, manager.getLevel(item));
        verify(item, never()).getItemMeta();
    }

    @Test
    void normalizedHistoricalIdentityWorksWithoutCosmeticMetadataOrLevel() {
        validTag();
        assertSame(bow, manager.get(item));
        assertEquals(1, manager.getLevel(item));
        assertEquals("moarbows:moarbow", BowManager.BOW_KEY.toString());
        assertEquals("moarbows:moarbowlevel", BowManager.LEVEL_KEY.toString());
    }

    @Test
    void unknownMalformedAndInvalidLevelFailClosed() {
        validTag();
        when(data.get(BowManager.BOW_KEY, PersistentDataType.STRING)).thenReturn("UNKNOWN");
        assertNull(manager.get(item));
        when(data.get(BowManager.BOW_KEY, PersistentDataType.STRING)).thenReturn("FIRE_BOW");
        when(data.has(BowManager.LEVEL_KEY)).thenReturn(true);
        assertNull(manager.get(item));
        when(data.has(BowManager.LEVEL_KEY, PersistentDataType.INTEGER)).thenReturn(true);
        when(data.get(BowManager.LEVEL_KEY, PersistentDataType.INTEGER)).thenReturn(-1);
        assertNull(manager.get(item));
    }

    @Test
    void nonBowsAndStackedBowsAreRejected() {
        validTag();
        when(item.getType()).thenReturn(Material.STICK);
        assertNull(manager.get(item));
        when(item.getType()).thenReturn(Material.BOW);
        when(item.getAmount()).thenReturn(2);
        assertNull(manager.get(item));
        assertNull(manager.get((ItemStack) null));
    }

    @Test
    void explicitMigrationClonesAndOnlyEditsOwnedPdcKeys() {
        ItemStack copy = mock(ItemStack.class);
        PersistentDataContainer edited = mock(PersistentDataContainer.class);
        when(item.clone()).thenReturn(copy);
        doAnswer(invocation -> {
            Consumer<PersistentDataContainer> writer = invocation.getArgument(0);
            writer.accept(edited);
            return true;
        }).when(copy).editPersistentDataContainer(any());
        assertSame(copy, manager.migrate(item, bow, 4));
        verify(edited).set(BowManager.BOW_KEY, PersistentDataType.STRING, "FIRE_BOW");
        verify(edited).set(BowManager.LEVEL_KEY, PersistentDataType.INTEGER, 4);
        verifyNoMoreInteractions(edited);
        verify(copy, never()).setItemMeta(any());
        verify(item, never()).editPersistentDataContainer(any());
    }

    @Test
    void migrationCannotOverwriteExistingIdentityOrLevel() {
        validTag();
        assertThrows(IllegalArgumentException.class, () -> manager.migrate(item, bow, 2));
        when(data.has(BowManager.BOW_KEY, PersistentDataType.STRING)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> manager.migrate(item, bow, 1));
        verify(item, never()).clone();
    }

    @Test
    void migrationIsSafeToRepeatForSameIdentityAndLevel() {
        validTag();
        ItemStack copy = mock(ItemStack.class);
        when(item.clone()).thenReturn(copy);
        assertSame(copy, manager.migrate(item, bow, 1));
    }
}
