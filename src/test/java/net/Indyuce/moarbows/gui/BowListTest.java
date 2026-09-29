package net.Indyuce.moarbows.gui;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.manager.BowManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BowListTest {
    private MockedStatic<Bukkit> bukkit;
    private Player player;
    private PlayerInventory storage;
    private InventoryView view;
    private Inventory top;
    private BowList menu;
    private List<Runnable> tasks;

    @BeforeEach
    void setup() {
        MoarBows.plugin = mock(MoarBows.class);
        BowManager manager = mock(BowManager.class);
        MoarBow bow = mock(MoarBow.class);
        when(bow.getId()).thenReturn("FIRE_BOW");
        when(bow.getItem(1)).thenReturn(mock(ItemStack.class));
        when(manager.getBows()).thenReturn(List.of(bow));
        when(manager.get("FIRE_BOW")).thenReturn(bow);
        when(MoarBows.plugin.getBowManager()).thenReturn(manager);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission("moarbows.gui")).thenReturn(true);
        storage = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(storage);
        view = mock(InventoryView.class);
        when(player.getOpenInventory()).thenReturn(view);
        top = mock(Inventory.class);
        when(top.getSize()).thenReturn(54);
        when(view.getTopInventory()).thenReturn(top);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        tasks = new ArrayList<>();
        doAnswer(call -> { tasks.add(call.getArgument(1)); return null; })
                .when(scheduler).runTask(eq(MoarBows.plugin), any(Runnable.class));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54), any(Component.class))).thenReturn(top);
        menu = new BowList(player);
        when(top.getHolder()).thenReturn(menu);
        menu.open();
    }

    @AfterEach
    void tearDown() {
        menu.close();
        bukkit.close();
        MoarBows.plugin = null;
    }

    private InventoryClickEvent click(ClickType click, int rawSlot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getClick()).thenReturn(click);
        when(event.getRawSlot()).thenReturn(rawSlot);
        return event;
    }

    @Test
    void pageCountIncludesPartialPages() {
        assertEquals(1, BowList.pageCount(0));
        assertEquals(1, BowList.pageCount(28));
        assertEquals(2, BowList.pageCount(29));
        assertEquals(3, BowList.pageCount(57));
        assertSame(top, menu.getInventory());
    }

    @Test
    void shiftDoubleHotbarOffhandDropAndCreativeClicksCannotTransferOrGrant() {
        for (ClickType type : ClickType.values()) {
            if (type == ClickType.LEFT || type == ClickType.RIGHT)
                continue;
            InventoryClickEvent event = click(type, 10);
            assertTrue(menu.whenClicked(event));
            verify(event).setCancelled(true);
        }
        assertTrue(tasks.isEmpty());
        verify(storage, never()).setItem(anyInt(), any());
    }

    @Test
    void bottomInventoryAndOutsideClicksCannotGrant() {
        for (int slot : new int[]{54, 65, -999}) {
            InventoryClickEvent event = click(ClickType.LEFT, slot);
            assertTrue(menu.whenClicked(event));
            verify(event).setCancelled(true);
        }
        assertTrue(tasks.isEmpty());
    }

    @Test
    void dragIsCancelledForWholeSession() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getView()).thenReturn(view);
        menu.onDrag(event);
        verify(event).setCancelled(true);
    }

    @Test
    void actorAndInventoryIdentityAreRequired() {
        InventoryClickEvent wrongPlayer = click(ClickType.LEFT, 10);
        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        when(wrongPlayer.getWhoClicked()).thenReturn(other);
        assertTrue(menu.whenClicked(wrongPlayer));
        InventoryClickEvent wrongInventory = click(ClickType.LEFT, 10);
        InventoryView otherView = mock(InventoryView.class);
        when(otherView.getTopInventory()).thenReturn(mock(Inventory.class));
        when(wrongInventory.getView()).thenReturn(otherView);
        assertFalse(menu.whenClicked(wrongInventory));
        verify(wrongInventory, never()).setCancelled(anyBoolean());
        assertTrue(tasks.isEmpty());
    }

    @Test
    void rapidClickQueuesOnlyOneActionAndNeverTrustsTheClientItem() {
        InventoryClickEvent first = click(ClickType.LEFT, 10);
        menu.whenClicked(first);
        menu.whenClicked(click(ClickType.RIGHT, 10));
        assertEquals(1, tasks.size());
        verify(first, never()).getCurrentItem();
        tasks.getFirst().run();
        verify(storage, times(1)).setItem(eq(0), any(ItemStack.class));
    }

    @Test
    void permissionIsCheckedAgainWhenDeferredActionRuns() {
        menu.whenClicked(click(ClickType.LEFT, 10));
        when(player.hasPermission("moarbows.gui")).thenReturn(false);
        tasks.getFirst().run();
        verify(storage, never()).setItem(anyInt(), any());
    }

    @Test
    void fullInventoryRefusesTheGrant() {
        when(storage.firstEmpty()).thenReturn(-1);
        menu.whenClicked(click(ClickType.LEFT, 10));
        tasks.getFirst().run();
        verify(storage, never()).setItem(anyInt(), any());
        verify(player).sendMessage(contains("Make room"));
    }

    @Test
    void closingOrQuittingInvalidatesPendingActions() {
        menu.whenClicked(click(ClickType.LEFT, 10));
        InventoryCloseEvent close = mock(InventoryCloseEvent.class);
        when(close.getPlayer()).thenReturn(player);
        when(close.getInventory()).thenReturn(top);
        menu.onClose(close);
        menu.close(); // Idempotent even when quit follows close.
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        menu.onQuit(quit);
        tasks.getFirst().run();
        verify(storage, never()).setItem(anyInt(), any());
    }

    @Test
    void deadPlayerCannotReceivePendingGrant() {
        menu.whenClicked(click(ClickType.LEFT, 10));
        when(player.isDead()).thenReturn(true);
        tasks.getFirst().run();
        verify(storage, never()).setItem(anyInt(), any());
    }

    @Test
    void anotherPluginsCancellationIsRespected() {
        InventoryClickEvent event = click(ClickType.LEFT, 10);
        when(event.isCancelled()).thenReturn(true);
        menu.whenClicked(event);
        assertTrue(tasks.isEmpty());
    }
}
