package net.Indyuce.moarbows.player;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.particle.ParticleData.ParticleRunnable;
import net.Indyuce.moarbows.util.lib.NotNull;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;

    private Player player;
    private long effectSession;

    /*
     * used to check twice a second if the player changed the item he's been
     * holding.
     */
    private ItemStack mainhand, offhand;
    private ParticleRunnable mainparticles, offparticles;

    /**
     * Where cooldowns from bows are all stored.
     */
    private final Map<String, Long> cooldowns = new HashMap<>();

    private static final Map<UUID, PlayerData> playerDatas = new HashMap<>();

    /**
     * Private constructor since it is only used when setting
     * up playerDatas, this way there is no possible confusion
     */
    private PlayerData(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
    }

    public UUID getUniqueId() {
        return uuid;
    }

    @NotNull
    public Player getPlayer() {
        return Objects.requireNonNull(player, "Player is offline");
    }

    public void updateItems() {
        if (player == null || !player.isOnline()) return;
        if (mainhand == null || !mainhand.isSimilar(player.getInventory().getItemInMainHand())) {
            mainhand = player.getInventory().getItemInMainHand().clone();
            if (mainparticles != null) mainparticles.cancel();
            mainparticles = null;
            MoarBow mainbow = MoarBows.plugin.getBowManager().get(mainhand);
            if (mainbow != null && mainbow.hasParticles())
                (mainparticles = mainbow.getParticles().newRunnable(player, false)).runTaskTimer(MoarBows.plugin, 0, 4);
        }

        if (offhand == null || !offhand.isSimilar(player.getInventory().getItemInOffHand())) {
            offhand = player.getInventory().getItemInOffHand().clone();
            if (offparticles != null) offparticles.cancel();
            offparticles = null;
            MoarBow offbow = MoarBows.plugin.getBowManager().get(offhand);
            if (offbow != null && offbow.hasParticles())
                (offparticles = offbow.getParticles().newRunnable(player, true)).runTaskTimer(MoarBows.plugin, 0, 4);
        }
    }

    private void stopParticles() {
        mainhand = null;
        offhand = null;
        if (mainparticles != null) mainparticles.cancel();
        if (offparticles != null) offparticles.cancel();
        mainparticles = null;
        offparticles = null;
    }

    public void invalidateEffects() {
        effectSession++;
    }

    public long getEffectSession() {
        return effectSession;
    }

    public void logOff() {
        stopParticles();
        invalidateEffects();
        player = null;
    }

    public static void resetParticles() {
        playerDatas.values().forEach(PlayerData::stopParticles);
    }

    public static void clearAll() {
        playerDatas.values().forEach(PlayerData::logOff);
        playerDatas.clear();
    }

    /** Keep UUID-only cooldown records across reconnects, then release expired offline rows. */
    public static void pruneOffline() {
        long now = System.currentTimeMillis();
        playerDatas.values().removeIf(data -> {
            data.cooldowns.values().removeIf(expiry -> expiry <= now);
            return data.player == null && data.cooldowns.isEmpty();
        });
    }

    public boolean hasCooldown(MoarBow bow, int level) {
        return cooldowns.getOrDefault(bow.getId(), 0L) > System.currentTimeMillis();
    }

    public double getRemainingCooldown(MoarBow bow, int level) {
        return Math.max(0, cooldowns.getOrDefault(bow.getId(), 0L) - System.currentTimeMillis()) / 1000.;
    }

    public void applyCooldown(MoarBow bow) {
        applyCooldown(bow, 1);
    }

    public void applyCooldown(MoarBow bow, int level) {
        double seconds = bow.getDouble("cooldown", level);
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid bow cooldown");
        cooldowns.put(bow.getId(), System.currentTimeMillis() + (long) (seconds * 1000));
    }

    @NotNull
    public static PlayerData get(OfflinePlayer player) {
        return Objects.requireNonNull(playerDatas.get(player.getUniqueId()), "Player data not loaded");
    }

    public static PlayerData setup(Player player) {
        pruneOffline();
        PlayerData found = playerDatas.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(player));
        found.player = player;
        return found;
    }
}
