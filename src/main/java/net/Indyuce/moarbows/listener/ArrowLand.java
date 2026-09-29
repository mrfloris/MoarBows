package net.Indyuce.moarbows.listener;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.comp.worldguard.CustomFlag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ArrowLand implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void prime(org.bukkit.event.entity.ExplosionPrimeEvent event) {
        MoarBows.plugin.getArrowManager().getEffectData(event.getEntity()).ifPresent(data -> {
            if (!data.canContinue() || !MoarBows.plugin.getWorldGuard().isFlagAllowed(event.getEntity().getLocation(), CustomFlag.MB_BOWS))
                event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(org.bukkit.event.entity.EntityExplodeEvent event) {
        MoarBows.plugin.getArrowManager().getEffectData(event.getEntity()).ifPresent(data -> {
            if (!data.canContinue() || !MoarBows.plugin.getWorldGuard().isFlagAllowed(event.getLocation(), CustomFlag.MB_BOWS)) {
                event.setCancelled(true);
                return;
            }
            event.blockList().removeIf(block -> !MoarBows.plugin.getWorldGuard().isFlagAllowed(block.getLocation(), CustomFlag.MB_BOWS));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void a(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow) || event.getHitEntity() != null) return;
        MoarBows.plugin.getArrowManager().takeArrowData(arrow).ifPresent(data -> {
            if (event.isCancelled()) return;
            data.captureImpact();
            Bukkit.getScheduler().runTask(MoarBows.plugin, () -> {
                if (!event.isCancelled() && data.canContinue()
                        && MoarBows.plugin.getWorldGuard().isFlagAllowed(arrow.getLocation(), CustomFlag.MB_BOWS))
                    data.getBow().whenLand(data);
            });
        });
    }
}
