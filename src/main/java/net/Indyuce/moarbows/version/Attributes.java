package net.Indyuce.moarbows.version;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.util.lib.NotNull;
import org.bukkit.attribute.Attribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Supported registry lookup with aliases retained for the upstream helper API. */
public class Attributes {

    /**
     * Maps Spigot IDs to attribute objects. In the future, this might need
     * to support namespaced keys which might make it possible to have
     * custom attributes through datapacks.
     */
    private static final Map<String, Attribute> BY_SPIGOT_ID = new HashMap<>();
    private static final Map<Attribute, String> ATTRIBUTE_NAMES = new HashMap<>();

    static {
        for (Attribute attribute : org.bukkit.Registry.ATTRIBUTE) {
            String name = attribute.getKey().getKey().toUpperCase(java.util.Locale.ROOT);
            BY_SPIGOT_ID.put(name, attribute);
            ATTRIBUTE_NAMES.put(attribute, name);
        }
    }

    @NotNull
    public static Attribute fromName(String... candidates) {
        return UtilityMethods.resolveField(candidate -> BY_SPIGOT_ID.get(
                candidate.replaceFirst("^(GENERIC_|PLAYER_|ZOMBIE_|HORSE_)", "")), candidates);
    }

    @NotNull
    public static String name(@NotNull Attribute attribute) {
        return ATTRIBUTE_NAMES.get(attribute);
    }

    /**
     * Util method to easily find some attribute given its ID, whatever
     * the server version. This tries both legacy and modern Spigot IDs.
     *
     * @param id* Attribute ID like MAX_HEALTH
     * @return Corresponding attribute
     */
    @NotNull
    public static Attribute adapt(@NotNull String id) {
        return fromName(id, "GENERIC_" + id, "PLAYER_" + id);
    }

    // After static block { .. }
    public static final Attribute
            ARMOR = fromName("ARMOR", "GENERIC_ARMOR"),
            ARMOR_TOUGHNESS = fromName("ARMOR_TOUGHNESS", "GENERIC_ARMOR_TOUGHNESS"),
            ATTACK_DAMAGE = fromName("ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE"),
            ATTACK_SPEED = fromName("ATTACK_SPEED", "GENERIC_ATTACK_SPEED"),
            KNOCKBACK_RESISTANCE = fromName("KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE"),
            LUCK = fromName("LUCK", "GENERIC_LUCK"),
            MAX_HEALTH = fromName("MAX_HEALTH", "GENERIC_MAX_HEALTH"),
            MOVEMENT_SPEED = fromName("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED"),
            FOLLOW_RANGE = fromName("FOLLOW_RANGE", "GENERIC_FOLLOW_RANGE"),
            ENTITY_INTERACTION_RANGE = fromName("ENTITY_INTERACTION_RANGE", "GENERIC_ARMOR"),
            BLOCK_INTERACTION_RANGE = fromName("BLOCK_INTERACTION_RANGE", "GENERIC_ARMOR");

    @NotNull
    public static Collection<Attribute> getAll() {
        return BY_SPIGOT_ID.values();
    }
}
