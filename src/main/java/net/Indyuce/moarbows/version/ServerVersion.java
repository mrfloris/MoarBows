package net.Indyuce.moarbows.version;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.util.lib.NotNull;
import net.Indyuce.moarbows.util.lib.Validate;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * 10/09/2025
 * Source: MythicLib
 */
public class ServerVersion {
    private final String craftBukkitVersion;
    private final int revNumber;
    private final int[] bukkitVersion;
    private final boolean paper;

    private static final int MAXIMUM_INDEX = 3;

    @Deprecated
    public ServerVersion(Class<?> ignored) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this();
    }

    public ServerVersion() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        // Version numbers
        final String[] bukkitSplit = Bukkit.getServer().getBukkitVersion().split("\\-")[0].split("\\."); // ["1", "20", "4"]
        bukkitVersion = new int[Math.min(MAXIMUM_INDEX, bukkitSplit.length)];
        for (int i = 0; i < bukkitVersion.length; i++)
            bukkitVersion[i] = Integer.parseInt(bukkitSplit[i]);

        // Compute rev number
        revNumber = findRevisionNumber();
        craftBukkitVersion = craftBukkitVersion(revNumber); // "v1_20_R4"

        // Running Paper?
        boolean isPaper = false;
        try {
            // Any other works, just the shortest I could find.
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            isPaper = true;
        } catch (ClassNotFoundException ignored) {
            // Ignored
        }
        this.paper = isPaper;

        // Validate all mappings
        try {
            //Attributes.getAll(); static code wont run
            VEnchantment.values();
            VMaterial.values();
            VParticle.values();
            VPotionEffectType.values();
            Validate.notNull(Sounds.ENTITY_ENDERMAN_HURT, "Error with sounds");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException("Compatibility error: " + throwable.getMessage());
        }
    }

    private String craftBukkitVersion(int revNumber) {
        return "v" + bukkitVersion[0] + "_" + bukkitVersion[1] + "_R" + revNumber;
    }

    private static final int MAXIMUM_REVISION_NUMBER = 10;
    private static final String CLASS_NAME_USED = "CraftServer";

    private int findRevisionNumber() {

        // Spigot || Paper <1.20.5
        try {
            final Class<?> bukkitServerClass = Bukkit.getServer().getClass();
            final String rev = bukkitServerClass.getPackage().getName().replace(".", ",").split(",")[3]; // "1_20_R4"
            return Integer.parseInt(rev.split("_")[2].replaceAll("[^0-9]", ""));
        } catch (Throwable throwable) {
            // Ignored
        }

        // Spigot 1.20.5+
        for (int revNumber = 1; revNumber < MAXIMUM_REVISION_NUMBER; revNumber++)
            try {
                final String candidate = craftBukkitVersion(revNumber);
                Class.forName("org.bukkit.craftbukkit." + candidate + "." + CLASS_NAME_USED);
                return revNumber;
            } catch (Throwable throwable) {
                // Ignored
            }

        // Assume no need for the revision number (Paper 1.20.5+)
        return 0;
    }

    public boolean isPaper() {
        return paper;
    }

    /**
     * This is the most useful function when dealing with compatibility. Since
     * plugin features are, most of the time, only registered when the server
     * version is found to be above a certain threshold.
     *
     * @param version Provided Minecraft version
     * @return True if server version is either equal to or above provided version.
     */
    public boolean isAbove(int... version) {
        Validate.isTrue(version.length >= 1 && version.length <= MAXIMUM_INDEX, "Provide at least 1 integer and at most " + MAXIMUM_INDEX);

        final int maxLength = Math.min(MAXIMUM_INDEX, Math.max(version.length, bukkitVersion.length));
        for (int i = 0; i < maxLength; i++) {
            final int server = i >= bukkitVersion.length ? 0 : bukkitVersion[i];
            final int provided = i >= version.length ? 0 : version[i];
            if (server != provided) return server > provided;
        }

        return true;
    }

    public boolean isUnder(int... version) {
        return !isAbove(version);
    }

    @NotNull
    public String getCraftBukkitVersion() {
        return craftBukkitVersion;
    }

    public int getRevisionNumber() {
        return revNumber;
    }

    public int[] getBukkitVersion() {
        return bukkitVersion;
    }

    @Override
    public String toString() {
        return "ServerVersion{" +
                "revision='" + craftBukkitVersion + '\'' +
                ", revisionNumber=" + revNumber +
                ", integers=" + Arrays.toString(bukkitVersion) +
                ", paper=" + paper +
                '}';
    }

    //region Static methods

    public static ServerVersion get() {
        return MoarBows.plugin.getVersion();
    }

    //endregion

    //region Deprecated

    @Deprecated
    public String getRevision() {
        return getCraftBukkitVersion();
    }

    @Deprecated
    public int[] toNumbers() {
        return bukkitVersion;
    }

    @Deprecated
    public int[] getIntegers() {
        return getBukkitVersion();
    }

    @Deprecated
    public boolean isStrictlyHigher(int... version) {
        Validate.isTrue(version.length >= 1 && version.length <= MAXIMUM_INDEX, "Provide at least 1 integer and at most " + MAXIMUM_INDEX);

        final int maxLength = Math.min(MAXIMUM_INDEX, Math.max(version.length, bukkitVersion.length));
        for (int i = 0; i < maxLength; i++) {
            final int server = i >= bukkitVersion.length ? 0 : bukkitVersion[i];
            final int provided = i >= version.length ? 0 : version[i];
            if (server != provided) return server > provided;
        }

        return false;
    }

    @Deprecated
    public boolean isBelowOrEqual(int... version) {
        return !isStrictlyHigher(version);
    }

    //endregion
}
