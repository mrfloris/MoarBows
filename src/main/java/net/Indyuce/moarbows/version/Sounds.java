package net.Indyuce.moarbows.version;

import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.util.lib.NotNull;
import org.bukkit.Sound;

import java.lang.reflect.Method;
import java.util.function.Function;

public class Sounds {
    public static final Sound
            ENTITY_ENDERMAN_HURT = fromName("ENTITY_ENDERMAN_HURT", "ENTITY_ENDERMEN_HURT"),
            ENTITY_ENDERMAN_DEATH = fromName("ENTITY_ENDERMAN_DEATH", "ENTITY_ENDERMEN_DEATH"),
            ENTITY_ENDERMAN_TELEPORT = fromName("ENTITY_ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT"),
            ENTITY_FIREWORK_ROCKET_LARGE_BLAST = fromName("ENTITY_FIREWORK_ROCKET_LARGE_BLAST", "ENTITY_FIREWORK_LARGE_BLAST"),
            ENTITY_FIREWORK_ROCKET_TWINKLE = fromName("ENTITY_FIREWORK_ROCKET_TWINKLE", "ENTITY_FIREWORK_TWINKLE"),
            ENTITY_FIREWORK_ROCKET_BLAST = fromName("ENTITY_FIREWORK_ROCKET_BLAST", "ENTITY_FIREWORK_BLAST"),
            ENTITY_ZOMBIE_PIGMAN_ANGRY = fromName("ENTITY_ZOMBIE_PIGMAN_ANGRY", "ENTITY_ZOMBIFIED_PIGLIN_ANGRY", "ENTITY_ZOMBIE_PIG_ANGRY"),
            BLOCK_NOTE_BLOCK_HAT = fromName("BLOCK_NOTE_BLOCK_HAT", "BLOCK_NOTE_HAT"),
            BLOCK_NOTE_BLOCK_PLING = fromName("BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_PLING"),
            BLOCK_NOTE_BLOCK_BELL = fromName("BLOCK_NOTE_BLOCK_BELL", "BLOCK_NOTE_BELL"),
            ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR = fromName("ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR", "ENTITY_ZOMBIE_ATTACK_DOOR_WOOD"),
            ENTITY_ENDER_DRAGON_GROWL = fromName("ENTITY_ENDER_DRAGON_GROWL", "ENTITY_ENDERDRAGON_GROWL"),
            ENTITY_ENDER_DRAGON_FLAP = fromName("ENTITY_ENDER_DRAGON_FLAP", "ENTITY_ENDERDRAGON_FLAP"),
            ENTITY_ZOMBIE_ATTACK_IRON_DOOR = fromName("ENTITY_ZOMBIE_ATTACK_IRON_DOOR"),
            ENTITY_PLAYER_ATTACK_CRIT = fromName("ENTITY_PLAYER_ATTACK_CRIT"),
            BLOCK_END_PORTAL_FRAME_FILL = fromName("BLOCK_END_PORTAL_FRAME_FILL"),
            ENTITY_SNOWBALL_THROW = fromName("ENTITY_SNOWBALL_THROW"),
            BLOCK_BREWING_STAND_BREW = fromName("BLOCK_BREWING_STAND_BREW"),
            BLOCK_GLASS_BREAK = fromName("BLOCK_GLASS_BREAK"),
            ENTITY_GENERIC_EXPLODE = fromName("ENTITY_GENERIC_EXPLODE"),
            ENTITY_EXPERIENCE_ORB_PICKUP = fromName("ENTITY_EXPERIENCE_ORB_PICKUP"),
            ENTITY_WITCH_DRINK = fromName("ENTITY_WITCH_DRINK"),
            BLOCK_FIRE_AMBIENT = fromName("BLOCK_FIRE_AMBIENT"),
            ENTITY_CHICKEN_EGG = fromName("ENTITY_CHICKEN_EGG"),
            ENTITY_PLAYER_ATTACK_SWEEP = fromName("ENTITY_PLAYER_ATTACK_SWEEP"),
            ENTITY_BLAZE_HURT = fromName("ENTITY_BLAZE_HURT"),
            BLOCK_FIRE_EXTINGUISH = fromName("BLOCK_FIRE_EXTINGUISH"),
            BLOCK_SNOW_BREAK = fromName("BLOCK_SNOW_BREAK"),
            ENTITY_PLAYER_LEVELUP = fromName("ENTITY_PLAYER_LEVELUP"),
            BLOCK_GRAVEL_BREAK = fromName("BLOCK_GRAVEL_BREAK"),
            ENTITY_ZOMBIE_HURT = fromName("ENTITY_ZOMBIE_HURT"),
            ENTITY_COW_HURT = fromName("ENTITY_COW_HURT"),
            ENTITY_PLAYER_ATTACK_KNOCKBACK = fromName("ENTITY_PLAYER_ATTACK_KNOCKBACK"),
            ENTITY_SHEEP_DEATH = fromName("ENTITY_SHEEP_DEATH"),
            ENTITY_BLAZE_AMBIENT = fromName("ENTITY_BLAZE_AMBIENT"),
            ENTITY_LLAMA_ANGRY = fromName("ENTITY_LLAMA_ANGRY"),
            ENTITY_WITHER_SHOOT = fromName("ENTITY_WITHER_SHOOT"),
            BLOCK_ANVIL_LAND = fromName("BLOCK_ANVIL_LAND"),
            ENTITY_CHICKEN_HURT = fromName("ENTITY_CHICKEN_HURT"),
            ENTITY_VILLAGER_NO = fromName("ENTITY_VILLAGER_NO"),
            ENTITY_ITEM_BREAK = fromName("ENTITY_ITEM_BREAK"),
            UI_BUTTON_CLICK = fromName("UI_BUTTON_CLICK"),
            ENTITY_GENERIC_EAT = fromName("ENTITY_GENERIC_EAT"),
            ENTITY_ARROW_SHOOT = fromName("ENTITY_ARROW_SHOOT"),
            ENTITY_EGG_THROW = fromName("ENTITY_EGG_THROW"),
            BLOCK_IRON_DOOR_OPEN = fromName("BLOCK_IRON_DOOR_OPEN");

    @NotNull
    public static Sound fromName(String... candidates) {
        return UtilityMethods.resolveField(getResolver(), candidates);
    }

    private static Function<String, Sound> RESOLVER;

    private static Function<String, Sound> getResolver() {
        if (RESOLVER == null)
            try {
                Method method = Sound.class.getDeclaredMethod("valueOf", String.class);
                RESOLVER = str -> {
                    try {
                        return (Sound) method.invoke(null, str);
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                };
            } catch (Exception exception) {
                throw new RuntimeException("Reflection error: " + exception.getMessage());
            }

        return RESOLVER;
    }
}
