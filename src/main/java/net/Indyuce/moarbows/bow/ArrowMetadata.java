package net.Indyuce.moarbows.bow;

import net.Indyuce.moarbows.player.PlayerData;
import net.Indyuce.moarbows.util.UtilityMethods;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.comp.worldguard.CustomFlag;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import java.util.Collection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class ArrowMetadata {
	private final MoarBow bow;
	private final PlayerData playerData;
	private final Arrow arrow;

	/**
	 * The original shooter instance is retained only for the lifetime of the shot;
	 * the session token prevents effects resuming after reconnect, death or world change.
	 */
	private final LivingEntity shooter;

	private final ItemStack source;
	private final UUID worldId;
    private final long effectSession;
    private final double arrowDamage;
    private final boolean critical;
    private final int fireTicks;
	private final int level;
	private Location impact;
	private final long date = System.currentTimeMillis();

	/**
	 * When a non-player entity uses the bow
	 */
	public ArrowMetadata(MoarBow bow, LivingEntity shooter, Arrow arrow, ItemStack source) {
		this.bow = bow;
		this.playerData = null;
		this.shooter = shooter;
		this.arrow = arrow;

		this.source = source.clone();
		this.worldId = shooter.getWorld().getUID();
        this.effectSession = playerData == null ? 0 : playerData.getEffectSession();
        this.arrowDamage = arrow.getDamage();
        this.critical = arrow.isCritical();
        this.fireTicks = arrow.getFireTicks();
		this.level = UtilityMethods.getBowLevel(source);
	}

	/**
	 * When a player uses the bow
	 */
	public ArrowMetadata(MoarBow bow, PlayerData playerData, Arrow arrow, ItemStack source) {
		this.bow = bow;
		this.playerData = playerData;
		this.shooter = playerData.getPlayer();
		this.arrow = arrow;

		this.source = source.clone();
		this.worldId = shooter.getWorld().getUID();
        this.effectSession = playerData == null ? 0 : playerData.getEffectSession();
        this.arrowDamage = arrow.getDamage();
        this.critical = arrow.isCritical();
        this.fireTicks = arrow.getFireTicks();
		this.level = UtilityMethods.getBowLevel(source);
	}

	public MoarBow getBow() {
		return bow;
	}

	public ItemStack getSource() {
		return source.clone();
	}

	public PlayerData getPlayerData() {
		return playerData;
	}

	public LivingEntity getShooter() {
		return shooter;
	}

	/**
	 * Lets you know if a player or a monster shot the arrow that way it can
	 * apply cooldowns to players and not do anything for monsters
	 */
	public boolean hasPlayer() {
		return playerData != null;
	}

	public Arrow getArrow() {
		return arrow;
	}

    public void captureImpact() {
        impact = arrow.getLocation().clone();
    }

    public Location getImpactLocation() {
        return impact == null ? arrow.getLocation() : impact.clone();
    }

    public Collection<Entity> getNearbyEntities(double x, double y, double z) {
        Location loc = getImpactLocation();
        return loc.getWorld().getNearbyEntities(loc, x, y, z);
    }

	public int getLevel() {
		return level;
	}

	public double getDouble(String path) {
		return bow.getDouble(path, level);
	}

	/**
	 * The arrow instance must be flushed from the database if it exists for
	 * longer than 10 minutes which is reasonable since landing an arrow should
	 * only take about a few seconds max
	 */
	public boolean canContinue() {
        if (playerData != null && playerData.getEffectSession() != effectSession) return false;
        if (!shooter.isValid() || shooter.isDead() || !shooter.getWorld().getUID().equals(worldId))
            return false;
        if (shooter instanceof Player player && (!player.isOnline() || !player.hasPermission("moarbows.use." + bow.getLowerCaseId())))
            return false;
        return shooter instanceof Player player
                ? MoarBows.plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.MB_BOWS)
                : MoarBows.plugin.getWorldGuard().isFlagAllowed(shooter.getLocation(), CustomFlag.MB_BOWS);
    }

    public void configureExtraArrow(Arrow extra) {
        extra.setWeapon(source.clone());
        extra.setDamage(arrowDamage);
        extra.setCritical(critical);
        extra.setFireTicks(fireTicks);
    }

    public boolean hasTimedOut() {
		return date + 10 * 60 * 1000 < System.currentTimeMillis();
	}
}
