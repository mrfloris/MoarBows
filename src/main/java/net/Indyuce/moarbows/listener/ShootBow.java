package net.Indyuce.moarbows.listener;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.api.event.MoarBowShootEvent;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.list.Railgun_Bow;
import net.Indyuce.moarbows.bow.particle.ArrowParticles;
import net.Indyuce.moarbows.comp.worldguard.CustomFlag;
import net.Indyuce.moarbows.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.IdentityHashMap;
import java.util.Map;

public class ShootBow implements Listener {
    private static final DecimalFormat COOLDOWN_FORMAT = new DecimalFormat("0.#");
    private final Map<EntityShootBowEvent, ArrowMetadata> pending = new IdentityHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void a(EntityShootBowEvent event) {
        if (event.isCancelled() || !(event.getProjectile() instanceof Arrow arrow)) return;
        ItemStack item = event.getBow();
        MoarBow bow = MoarBows.plugin.getBowManager().get(item);
        if (bow == null) return;
        if (event.getEntity() instanceof Player player && !player.hasPermission("moarbows.use." + bow.getLowerCaseId())) {
            player.sendMessage(MoarBows.plugin.getLanguage().formatMessage("not-enough-perms"));
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player player
                ? !MoarBows.plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.MB_BOWS)
                : !MoarBows.plugin.getWorldGuard().isFlagAllowed(event.getEntity().getLocation(), CustomFlag.MB_BOWS)) {
            event.setCancelled(true);
            if (event.getEntity() instanceof Player player)
                player.sendMessage(MoarBows.plugin.getLanguage().formatMessage("disable-bows-flag"));
            return;
        }
        if (event.getForce() < 1 && MoarBows.plugin.getConfig().getBoolean("full-pull-restriction")) return;
        ArrowMetadata data = event.getEntity() instanceof Player player
                ? new ArrowMetadata(bow, PlayerData.setup(player), arrow, item)
                : new ArrowMetadata(bow, event.getEntity(), arrow, item);
        if (data.hasPlayer() && data.getPlayerData().hasCooldown(bow, data.getLevel())) {
            data.getShooter().sendMessage(MoarBows.plugin.getLanguage().formatMessage("on-cooldown", "left",
                    COOLDOWN_FORMAT.format(data.getPlayerData().getRemainingCooldown(bow, data.getLevel()))));
            event.setCancelled(true);
            return;
        }
        // Railgun's vehicle requirement is a preflight condition, before native item consumption.
        if (bow instanceof Railgun_Bow && !bow.canShoot(event, data)) {
            event.setCancelled(true);
            return;
        }
        MoarBowShootEvent custom = new MoarBowShootEvent(data);
        Bukkit.getPluginManager().callEvent(custom);
        if (custom.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        if (!bow.usesVanillaArrow()) {
            // Leave ammo, Infinity and bow durability to Paper. Hold the harmless placeholder
            // until all listeners have had an opportunity to cancel the original shot.
            arrow.setDamage(0);
            arrow.setVelocity(new Vector());
            arrow.setGravity(false);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }
        pending.put(event, data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void finish(EntityShootBowEvent event) {
        ArrowMetadata data = pending.remove(event);
        if (data == null || event.isCancelled() || event.getProjectile() != data.getArrow()) return;
        if (data.hasPlayer()) data.getPlayerData().applyCooldown(data.getBow(), data.getLevel());
        if (data.getBow().usesVanillaArrow()) MoarBows.plugin.getArrowManager().registerArrow(data);
        Bukkit.getScheduler().runTask(MoarBows.plugin, () -> {
            if (event.isCancelled() || !data.canContinue()) {
                MoarBows.plugin.getArrowManager().unregisterArrow(data.getArrow());
                data.getArrow().remove();
                return;
            }
            if (!data.getBow().usesVanillaArrow()) {
                data.getArrow().remove();
                data.getBow().canShoot(event, data);
            } else if (data.getArrow().isValid() && MoarBows.plugin.getConfig().getBoolean("arrow-particles") && data.getBow().hasParticles()) {
                new ArrowParticles(data.getBow(), data.getArrow()).runTaskTimer(MoarBows.plugin, 0, 1);
            }
        });
    }
}
