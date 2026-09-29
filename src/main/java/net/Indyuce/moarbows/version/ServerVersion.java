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
        bukkitVersion = parseVersion(Bukkit.getServer().getBukkitVersion());

        // Modern Paper exposes its version through the supported Bukkit API.
        // Kept for source compatibility with integrations; no CraftBukkit probing.
        revNumber = 0;
        craftBukkitVersion = Bukkit.getBukkitVersion();
        paper = true;

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

    static int[] parseVersion(String value) {
        // Paper 26.x uses e.g. 26.2.build.129-stable; the build is not a game version.
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[.\\-].*)?$").matcher(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Unrecognized Paper version: " + value);
        return matcher.group(3) == null
                ? new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))}
                : new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))};
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
