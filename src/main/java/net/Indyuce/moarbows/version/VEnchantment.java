package net.Indyuce.moarbows.version;

import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.util.lib.NotNull;
import net.Indyuce.moarbows.util.lib.Nullable;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public enum VEnchantment {
    POWER("power"),
    FORTUNE("fortune"),
    UNBREAKING("unbreaking"),
    ;

    private final Enchantment wrapped;

    VEnchantment(String... candidates) {
        wrapped = UtilityMethods.resolveField(VEnchantment::fromKey, candidates);
    }

    @Nullable
    private static Enchantment fromKey(@NotNull String key) {
        return io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(key));
    }

    @NotNull
    public Enchantment get() {
        return wrapped;
    }
}
