package net.Indyuce.moarbows.version;

import net.Indyuce.moarbows.util.lib.NotNull;
import org.bukkit.Particle;

public enum VParticle {
    EXPLOSION("POOF", "EXPLOSION_NORMAL"),
    LARGE_EXPLOSION("EXPLOSION", "EXPLOSION_LARGE"), // EXPLOSION_EMITTER is a huge explosion
    LAVA("LAVA"),
    WITCH("WITCH", "SPELL_WITCH"),
    HEART("HEART"),
    LARGE_SMOKE("LARGE_SMOKE", "SMOKE_LARGE"),
    SMOKE("SMOKE", "SMOKE_NORMAL"),
    REDSTONE("DUST", "REDSTONE"),
    FIREWORK("FIREWORK", "FIREWORKS_SPARK"),
    INSTANT_EFFECT("INSTANT_EFFECT", "SPELL_INSTANT"),
    EFFECT("EFFECT", "SPELL"),
    HAPPY_VILLAGER("VILLAGER_HAPPY", "HAPPY_VILLAGER"),
    ANGRY_VILLAGER("VILLAGER_ANGRY", "ANGRY_VILLAGER"),
    /**
     * Requires color
     */
    ENTITY_EFFECT("ENTITY_EFFECT", "SPELL_MOB"),
    ENTITY_EFFECT_AMBIENT("ENTITY_EFFECT", "SPELL_MOB_AMBIENT"),
    TOTEM_OF_UNDYING("TOTEM_OF_UNDYING", "TOTEM"),
    SNOWFLAKE("ITEM_SNOWBALL", "SNOWBALL"),
    BLOCK("BLOCK", "BLOCK_CRACK"),
    CRIT("CRIT"),
    FLAME("FLAME"),
    SLIME("ITEM_SLIME", "SLIME"),
    /**
     * Requires material
     */
    BLOCK_DUST("BLOCK", "BLOCK_DUST"),
    ITEM_SNOWBALL("SNOWFLAKE", "SNOW_SHOVEL"),
    ITEM_SLIME("ITEM_SLIME", "SLIME"),
    ENCHANTED_HIT("ENCHANTED_HIT", "CRIT_MAGIC"),
    ITEM("ITEM", "ITEM_CRACK"),
    ;

    private final Particle wrapped;

    VParticle(String... candidates) {
        wrapped = net.Indyuce.moarbows.util.UtilityMethods.resolveField(Particle::valueOf, candidates);
    }

    @NotNull
    public Particle get() {
        return wrapped;
    }
}