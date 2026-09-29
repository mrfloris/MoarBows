package net.Indyuce.moarbows.gui;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.MoarBow;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A server-owned catalogue. Display items are never transferred to a player. */
public class BowList implements InventoryHolder, Listener {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final Set<BowList> SESSIONS = new HashSet<>();

    private final Player player;
    private final List<String> bows;
    private final Map<Integer, String> displayedBows = new HashMap<>();
    private Inventory inventory;
    private int page = 1;
    private boolean open;
    private boolean closed;
    private boolean pendingAction;

    public BowList(Player player) {
        this.player = player;
        bows = MoarBows.plugin.getBowManager().getBows().stream()
                .map(MoarBow::getId).sorted(Comparator.naturalOrder()).toList();
        render();
    }

    static int pageCount(int size) {
        return Math.max(1, (size + SLOTS.length - 1) / SLOTS.length);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void render() {
        inventory = Bukkit.createInventory(this, 54, Component.text("Available Bows (" + page + "/" + pageCount(bows.size()) + ")"));
        displayedBows.clear();
        for (int j = 0; j < SLOTS.length; j++) {
            int index = (page - 1) * SLOTS.length + j;
            if (index >= bows.size())
                break;
            String id = bows.get(index);
            MoarBow bow = MoarBows.plugin.getBowManager().get(id);
            if (bow != null) {
                displayedBows.put(SLOTS[j], id);
                inventory.setItem(SLOTS[j], bow.getItem(1));
            }
        }
        if (page > 1)
            inventory.setItem(45, navigationItem("Previous Page"));
        if (page < pageCount(bows.size()))
            inventory.setItem(53, navigationItem("Next Page"));
    }

    private static ItemStack navigationItem(String name) {
        ItemStack item = ItemStack.of(Material.ARROW);
        item.editMeta(meta -> meta.displayName(Component.text(name)));
        return item;
    }

    public void open() {
        if (open || closed || !player.hasPermission("moarbows.gui"))
            return;
        open = true;
        SESSIONS.add(this);
        Bukkit.getPluginManager().registerEvents(this, MoarBows.plugin);
        player.openInventory(inventory);
        if (player.getOpenInventory().getTopInventory() != inventory)
            close();
    }

    private boolean ownsView(Inventory top) {
        return top != null && top.getHolder() == this;
    }

    private boolean isActive(Inventory expected) {
        return open && !closed && player.isOnline() && !player.isDead()
                && inventory == expected && ownsView(expected)
                && player.getOpenInventory().getTopInventory() == expected
                && player.hasPermission("moarbows.gui");
    }

    /** Cancel every transfer path, accepting only a simple click on an owned slot. */
    public boolean whenClicked(InventoryClickEvent event) {
        if (!ownsView(event.getView().getTopInventory()))
            return false;
        boolean previouslyCancelled = event.isCancelled();
        event.setCancelled(true);
        if (previouslyCancelled || !event.getWhoClicked().getUniqueId().equals(player.getUniqueId())
                || !isActive(event.getView().getTopInventory()) || pendingAction
                || (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)
                || event.getRawSlot() < 0 || event.getRawSlot() >= inventory.getSize())
            return true;

        int slot = event.getRawSlot();
        if (slot != 45 && slot != 53 && !displayedBows.containsKey(slot))
            return true;
        Inventory expectedInventory = inventory;
        int expectedPage = page;
        String expectedBow = displayedBows.get(slot);
        pendingAction = true;
        Bukkit.getScheduler().runTask(MoarBows.plugin, () -> {
            try {
                if (!isActive(expectedInventory) || page != expectedPage)
                    return;
                if ((slot == 45 && page > 1) || (slot == 53 && page < pageCount(bows.size()))) {
                    page += slot == 45 ? -1 : 1;
                    // Assign the new inventory before opening it, so closing the old
                    // page cannot invalidate this session.
                    render();
                    player.openInventory(inventory);
                    if (player.getOpenInventory().getTopInventory() != inventory)
                        close();
                    return;
                }
                if (expectedBow == null || !expectedBow.equals(displayedBows.get(slot)))
                    return;
                MoarBow bow = MoarBows.plugin.getBowManager().get(expectedBow);
                if (bow == null)
                    return;
                int empty = player.getInventory().firstEmpty();
                if (empty < 0) {
                    player.sendMessage(ChatColor.RED + "Make room in your inventory before taking a bow.");
                    return;
                }
                player.getInventory().setItem(empty, bow.getItem(1));
                player.playSound(player.getLocation(), "minecraft:block.note_block.pling", 1, 2);
            } finally {
                pendingAction = false;
            }
        });
        return true;
    }

    public void close() {
        if (closed)
            return;
        closed = true;
        open = false;
        SESSIONS.remove(this);
        HandlerList.unregisterAll(this);
    }

    /** Called before plugin disable, while the menu listeners still protect their items. */
    public static void closeAll() {
        for (BowList session : new ArrayList<>(SESSIONS)) {
            if (session.ownsView(session.player.getOpenInventory().getTopInventory()))
                session.player.closeInventory();
            session.close();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()) && event.getInventory() == inventory)
            close();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()))
            close();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void registerClicks(InventoryClickEvent event) {
        whenClicked(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (ownsView(event.getView().getTopInventory()))
            event.setCancelled(true);
    }
}
