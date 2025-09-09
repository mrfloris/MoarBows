package net.Indyuce.moarbows.version;

import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.util.lib.NotNull;
import org.bukkit.potion.PotionEffectType;

public enum VPotionEffectType {
    NAUSEA("NAUSEA", "CONFUSION"),
    SLOWNESS("SLOWNESS", "SLOW"),
    JUMP_BOOST("JUMP_BOOST", "JUMP"),
    MINING_FATIGUE("MINING_FATIGUE", "SLOW_DIGGING"),
    HASTE("HASTE", "FAST_DIGGING"),

    ;

    private final PotionEffectType wrapped;

    VPotionEffectType(String... candidates) {
        wrapped = UtilityMethods.resolveField(PotionEffectType::getByName, candidates);
    }

    @NotNull
    public PotionEffectType get() {
        return wrapped;
    }
}