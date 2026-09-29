package net.Indyuce.moarbows.manager;

import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.list.*;
import net.Indyuce.moarbows.util.lib.Validate;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BowManager {
    // Preserve the normalized keys used by older Bukkit releases. Names and lore
    // are presentation only and are never used to identify a custom bow.
    public static final NamespacedKey BOW_KEY = new NamespacedKey("moarbows", "moarbow");
    public static final NamespacedKey LEVEL_KEY = new NamespacedKey("moarbows", "moarbowlevel");

    /**
     * Bows are registered in this map using their bow
     * IDs and two bows with the same ID will override
     */
    private final Map<String, MoarBow> map = new HashMap<>();

    /**
     * The plugin must register the bows before the plugin
     * is enabled otherwise it can't generate the required
     * config files
     */
    private boolean registration = true;

    public void register(MoarBow bow) {
        Validate.isTrue(registration, "Bows must be registered before MoarBows enables");
        Validate.isTrue(!map.containsKey(bow.getId()), "A bow with the same ID already exists");

        map.put(bow.getId(), bow);
    }

    public void stopRegistration() {
        Validate.isTrue(registration, "Bow registration is disabled");

        // An explicit catalogue avoids scanning our JAR on the server thread.
        register(new Autobow());
        register(new Blaze_Bow());
        register(new Chicken_Bow());
        register(new Composite_Bow());
        register(new Corona_Bow());
        register(new Corrosive_Bow());
        register(new Cupidons_Bow());
        register(new Earthquake_Bow());
        register(new Explosive_Bow());
        register(new Fire_Bow());
        register(new Gravity_Bow());
        register(new Hunter_Bow());
        register(new Ice_Bow());
        register(new Laser_Bow());
        register(new Lightning_Bowlt());
        register(new Linear_Bow());
        register(new Marked_Bow());
        register(new Meteor_Bow());
        register(new Pulsar_Bow());
        register(new Railgun_Bow());
        register(new Shadow_Bow());
        register(new Shocking_Bow());
        register(new Silver_Bow());
        register(new Snow_Bow());
        register(new Spartan_Bow());
        register(new Trippple_Bow());
        register(new Void_Bow());
        register(new Wither_Bow());

        registration = false;
    }

    public Collection<MoarBow> getBows() {
        return map.values();
    }

    public boolean has(String id) {
        return map.containsKey(id);
    }

    @Nullable
    public MoarBow get(String id) {
        return map.get(id);
    }

    @Nullable
    public MoarBow get(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || item.getAmount() != 1)
            return null;

        var data = item.getPersistentDataContainer();
        if (!data.has(BOW_KEY, PersistentDataType.STRING)
                || (data.has(LEVEL_KEY) && !data.has(LEVEL_KEY, PersistentDataType.INTEGER)))
            return null;
        Integer level = data.get(LEVEL_KEY, PersistentDataType.INTEGER);
        if (level != null && level < 1)
            return null;
        String tag = data.get(BOW_KEY, PersistentDataType.STRING);
        return map.get(tag);
    }

    public int getLevel(ItemStack item) {
        if (get(item) == null)
            return 0;
        return item.getPersistentDataContainer().getOrDefault(LEVEL_KEY, PersistentDataType.INTEGER, 1);
    }

    /** Explicit administrator attestation of an old bow, preserving all other item data. */
    public ItemStack migrate(ItemStack original, MoarBow bow, int level) {
        Validate.isTrue(original != null && original.getType() == Material.BOW && original.getAmount() == 1,
                "Hold exactly one bow to migrate.");
        Validate.isTrue(bow != null && map.get(bow.getId()) == bow && level > 0, "Unknown bow or invalid level.");
        var data = original.getPersistentDataContainer();
        if (data.has(BOW_KEY) || data.has(LEVEL_KEY)) {
            Validate.isTrue(get(original) == bow && getLevel(original) == level,
                    "Existing bow identity or level differs, or its tags are malformed. No changes were made.");
        }
        ItemStack migrated = original.clone();
        migrated.editPersistentDataContainer(pdc -> {
            pdc.set(BOW_KEY, PersistentDataType.STRING, bow.getId());
            pdc.set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        });
        return migrated;
    }
}
