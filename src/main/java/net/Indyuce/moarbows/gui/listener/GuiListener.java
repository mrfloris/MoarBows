package net.Indyuce.moarbows.gui.listener;

import net.Indyuce.moarbows.gui.PluginInventory;
import net.Indyuce.moarbows.util.UtilityMethods;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    @EventHandler
    public void a(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (event.getClickedInventory() == null) return;

        if (event.getInventory().getHolder() instanceof PluginInventory
                && event.getClickedInventory().equals(event.getInventory())
                && UtilityMethods.isPluginItem(item, false)
                && ((PluginInventory) event.getInventory().getHolder()).whenClicked(event))
            event.setCancelled(true);
    }
}