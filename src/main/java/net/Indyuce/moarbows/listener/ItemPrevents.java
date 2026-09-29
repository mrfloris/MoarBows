package net.Indyuce.moarbows.listener;

import net.Indyuce.moarbows.MoarBows;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemPrevents implements Listener {
    private boolean isBow(ItemStack item) {
        return MoarBows.plugin.getBowManager().get(item) != null;
    }

    boolean denyAnvil(HumanEntity player, Inventory inventory, ItemStack result) {
        // Include the second input: a normal bow must not launder a custom bow's
        // permissions or combine two different custom identities.
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);
        if (!isBow(first) && !isBow(second) && !isBow(result))
            return false;
        if (!player.hasPermission("moarbows.anvil"))
            return true;
        if (MoarBows.plugin.getLanguage().disableRepair && !player.hasPermission("moarbows.repair"))
            return true;
        if (MoarBows.plugin.getLanguage().disableEnchant && !player.hasPermission("moarbows.enchant") && result != null) {
            var originalEnchants = first == null ? java.util.Map.<org.bukkit.enchantments.Enchantment, Integer>of() : first.getEnchantments();
            if (result.getEnchantments().entrySet().stream()
                    .anyMatch(enchant -> enchant.getValue() > originalEnchants.getOrDefault(enchant.getKey(), 0)))
                return true;
        }
        if (isBow(second)) {
            var manager = MoarBows.plugin.getBowManager();
            if (manager.get(first) != manager.get(second) || manager.getLevel(first) != manager.getLevel(second))
                return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void prepareAnvil(PrepareAnvilEvent event) {
        if (denyAnvil(event.getView().getPlayer(), event.getInventory(), event.getResult()))
            event.setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void takeAnvilResult(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getType() == InventoryType.ANVIL && event.getRawSlot() == 2
                && denyAnvil(event.getWhoClicked(), top, event.getCurrentItem()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void enchant(EnchantItemEvent event) {
        if (MoarBows.plugin.getLanguage().disableEnchant && isBow(event.getItem())
                && !event.getEnchanter().hasPermission("moarbows.enchant"))
            event.setCancelled(true);
    }
}
