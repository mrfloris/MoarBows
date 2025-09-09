package net.Indyuce.moarbows.version;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.util.lib.NotNull;
import org.bukkit.attribute.Attribute;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * In 1.21.2 the Attribute enum was changed to an abstract class.
 * Minecraft and therefore Spigot/Paper now use a registry to save
 * attributes, they are no longer hardcoded in an enum.
 * <p>
 * The Java compiler does not like relocations of methods like
 * Enum#valueOf() and Enum#values() so this class just fixes that
 * semantic problem.
 * <p>
 * In the future, it might be needed to wrap attributes inside
 * to support both IDs and namespaced keys, just like what's
 * already been done to attribute modifiers.
 */
public class Attributes {

    /**
     * Maps Spigot IDs to attribute objects. In the future, this might need
     * to support namespaced keys which might make it possible to have
     * custom attributes through datapacks.
     */
    private static final Map<String, Attribute> BY_SPIGOT_ID = new HashMap<>();
    private static final Map<Attribute, String> ATTRIBUTE_NAMES = new HashMap<>();

    static {

        // Before 1.21.2
        if (MoarBows.plugin.getVersion().isUnder(1, 21, 2)) {
            try {

                // Look through all enum fields and store them
                for (Field field : Attribute.class.getDeclaredFields())
                    if (field.getType() == Attribute.class) {
                        Attribute attr = (Attribute) field.get(null);
                        BY_SPIGOT_ID.put(field.getName(), attr);
                        ATTRIBUTE_NAMES.put(attr, field.getName());
                    }

            } catch (Exception exception) {
                throw new RuntimeException("Reflection error", exception);
            }
        }

        // 1.21.2+ Attribute is now an abstract class, not interface
        else {

            for (Attribute attribute : Attribute.values()) {
                String name = attribute.getKey().getKey().toUpperCase();
                BY_SPIGOT_ID.put(name, attribute);
                ATTRIBUTE_NAMES.put(attribute, name);
            }
        }
    }

    @NotNull
    public static Attribute fromName(String... candidates) {
        return UtilityMethods.resolveField(getResolver(), candidates);
    }

    @NotNull
    public static String name(@NotNull Attribute attribute) {
        return ATTRIBUTE_NAMES.get(attribute);
    }

    private static Function<String, Attribute> RESOLVER;

    private static Function<String, Attribute> getResolver() {
        if (RESOLVER == null)
            try {
                Method method = Attribute.class.getDeclaredMethod("valueOf", String.class);
                RESOLVER = str -> {
                    try {
                        return (Attribute) method.invoke(null, str);
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                };
            } catch (Exception exception) {
                throw new RuntimeException("Reflection error: " + exception.getMessage());
            }

        return RESOLVER;
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
