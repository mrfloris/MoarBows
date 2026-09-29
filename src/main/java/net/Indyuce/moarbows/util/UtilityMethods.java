package net.Indyuce.moarbows.util;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.comp.worldguard.CustomFlag;
import net.Indyuce.moarbows.util.lib.NotNull;
import net.Indyuce.moarbows.version.VEnchantment;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class UtilityMethods {
    public static String caseOnWords(String str) {
        StringBuilder builder = new StringBuilder(str);
        boolean isLastSpace = true;
        for (int i = 0; i < builder.length(); i++) {
            char ch = builder.charAt(i);
            if (isLastSpace && ch >= 'a' && ch <= 'z') {
                builder.setCharAt(i, (char) (ch + ('A' - 'a')));
                isLastSpace = false;
            } else if (ch != ' ')
                isLastSpace = false;
            else
                isLastSpace = true;
        }
        return builder.toString();
    }

    @NotNull
    public static <T> T resolveField(@NotNull Function<String, T> resolver, @NotNull String... candidates) {
        return resolveField(resolver, null, candidates);
    }

    @NotNull
    public static <T> T resolveField(@NotNull Function<String, T> resolver, @Nullable Supplier<T> defaultValue, @NotNull String... candidates) {

        // Try all candidates
        for (String candidate : candidates)
            try {
                return Objects.requireNonNull(resolver.apply(candidate), "Null supplied value");
            } catch (Throwable throwable) {
                // Ignore & try next candidate
            }

        // Default value if any
        if (defaultValue != null) return Objects.requireNonNull(defaultValue.get(), "Null supplied default value");

        // Error otherwise
        throw new IllegalArgumentException("Could not find enum field given candidates " + Arrays.asList(candidates));
    }

    public static boolean consumeAmmo(LivingEntity entity, ItemStack ammo) {

        // If sender is not a player, then do not consume any ammo
        if (!(entity instanceof Player))
            return true;

        // Does not consume ammo if the player is in creative mode
        Player player = (Player) entity;
        if (player.getGameMode() == GameMode.SPECTATOR || ammo == null || ammo.getAmount() < 1)
            return false;
        if (player.getGameMode() == GameMode.CREATIVE)
            return true;

        // Check the complete request before changing a slot. Exact similarity
        // preserves custom ammunition and metadata from other plugins.
        var inventory = player.getInventory();
        int available = 0;
        for (ItemStack item : inventory.getStorageContents())
            if (item != null && item.isSimilar(ammo))
                available += item.getAmount();
        ItemStack offhand = inventory.getItemInOffHand();
        if (offhand.isSimilar(ammo))
            available += offhand.getAmount();
        if (available < ammo.getAmount())
            return false;

        int remaining = ammo.getAmount();
        for (int slot = 0; slot < inventory.getStorageContents().length && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.isSimilar(ammo))
                continue;
            int consumed = Math.min(remaining, item.getAmount());
            ItemStack updated = item.clone();
            updated.setAmount(item.getAmount() - consumed);
            inventory.setItem(slot, updated.getAmount() == 0 ? null : updated);
            remaining -= consumed;
        }
        if (remaining > 0) {
            ItemStack updated = offhand.clone();
            updated.setAmount(offhand.getAmount() - remaining);
            inventory.setItemInOffHand(updated.getAmount() == 0 ? null : updated);
        }
        return true;
    }

    public static boolean isPluginItem(ItemStack item, boolean lore) {
        return MoarBows.plugin.getBowManager().get(item) != null;
    }

    public static int getBowLevel(ItemStack item) {
        return MoarBows.plugin.getBowManager().getLevel(item);
    }

    public static double getPowerDamageMultiplier(@NotNull ItemStack item) {
        var powerEnchant = VEnchantment.POWER.get();
        return item == null || item.getType() == Material.AIR || !item.hasItemMeta() || !item.getItemMeta().hasEnchant(powerEnchant) ? 1
                : 1 + .25 * (item.getItemMeta().getEnchantLevel(powerEnchant) + 1);
    }

    public static boolean canTarget(LivingEntity shooter, Location loc, Entity target) {
        if (!shooter.isValid() || shooter.isDead() || !target.isValid() || target.isDead()
                || target.equals(shooter) || !target.getWorld().equals(shooter.getWorld()) || target.hasMetadata("NPC"))
            return false;
        if (shooter instanceof Player player && player.getGameMode() == GameMode.SPECTATOR)
            return false;
        if (target instanceof Player player && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR))
            return false;
        var protection = MoarBows.plugin.getWorldGuard();
        boolean shooterAllowed = shooter instanceof Player player
                ? protection.isFlagAllowed(player, CustomFlag.MB_BOWS)
                : protection.isFlagAllowed(shooter.getLocation(), CustomFlag.MB_BOWS);
        if (!shooterAllowed || !protection.isFlagAllowed(target.getLocation(), CustomFlag.MB_BOWS))
            return false;
        if (target instanceof Player && (!target.getWorld().getPVP()
                || !protection.isPvpAllowed(shooter.getLocation()) || !protection.isPvpAllowed(target.getLocation())))
            return false;
        return loc == null || (loc.getWorld().equals(target.getWorld())
                && target.getBoundingBox().expand(.5, .5, .5).contains(loc.toVector()));
    }

    public static double truncation(double x, int n) {
        double pow = Math.pow(10.0, n);
        return Math.floor(x * pow) / pow;
    }

    public static Vector rotateFunc(Vector v, Location loc) {
        double yaw = loc.getYaw() / 180 * Math.PI;
        double pitch = loc.getPitch() / 180 * Math.PI;
        v = rotAxisX(v, pitch);
        v = rotAxisY(v, -yaw);
        return v;
    }

    private static Vector rotAxisX(Vector v, double a) {
        double y = v.getY() * Math.cos(a) - v.getZ() * Math.sin(a);
        double z = v.getY() * Math.sin(a) + v.getZ() * Math.cos(a);
        return v.setY(y).setZ(z);
    }

    private static Vector rotAxisY(Vector v, double b) {
        double x = v.getX() * Math.cos(b) + v.getZ() * Math.sin(b);
        double z = v.getX() * -Math.sin(b) + v.getZ() * Math.cos(b);
        return v.setX(x).setZ(z);
    }

    /**
     * Method to get all entities surrounding a location. This method does not
     * take every entity in the world but rather takes all the entities from the
     * 9 chunks around the entity, so even if the location is at the border of a
     * chunk (worst case border of 4 chunks), the entity will still be included
     */
    public static void forEachNearbyChunkEntity(Location loc, Consumer<Entity> action) {

        /*
         * Another method to save performance is if an entity bounding box
         * calculation is made twice in the same tick then the method does not
         * need to be called twice, it can utilize the same entity list since
         * the entities have not moved (e.g fireball which does 2+ calculations
         * per tick)
         */
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        for (int x = -1; x < 2; x++)
            for (int z = -1; z < 2; z++)
                if (loc.getWorld().isChunkLoaded(cx + x, cz + z))
                    for (Entity entity : loc.getWorld().getChunkAt(cx + x, cz + z).getEntities())
                        action.accept(entity);
    }

    public static <T> void clean(Iterable<T> collection, Predicate<T> clean) {
        Iterator<T> it = collection.iterator();
        while (it.hasNext()) {
            T next = it.next();
            if (clean.test(next))
                it.remove();
        }
    }
}
