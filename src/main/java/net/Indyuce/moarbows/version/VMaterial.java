package net.Indyuce.moarbows.version;

import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.util.lib.NotNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum VMaterial {
    BLAST_FURNACE("BLAST_FURNACE", "FURNACE"),
    CAMPFIRE("CAMPFIRE", "FURNACE"),
    SMOKER("SMOKER", "FURNACE"),
    SMITHING_TABLE("SMITHING_TABLE", "FURNACE"),
    GRASS_BLOCK("GRASS_BLOCK", "GRASS"),
    SPYGLASS("SPYGLASS", "GLASS_BOTTLE"),

    ;

    private final Material wrapped;

    VMaterial(String... candidates) {
        wrapped = UtilityMethods.resolveField(Material::valueOf, candidates);
    }

    @NotNull
    public Material get() {
        return wrapped;
    }

    @NotNull
    public ItemStack toItem() {
        return new ItemStack(wrapped);
    }
}
