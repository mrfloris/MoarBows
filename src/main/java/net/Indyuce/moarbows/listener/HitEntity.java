package net.Indyuce.moarbows.listener;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.util.UtilityMethods;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class HitEntity implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void modify(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        MoarBows.plugin.getArrowManager().getEffectData(event.getDamager()).ifPresent(data -> {
            if (!data.canContinue() || !UtilityMethods.canTarget(data.getShooter(), null, event.getEntity())) event.setCancelled(true);
        });
        if (event.isCancelled() || !(event.getDamager() instanceof Arrow arrow)) return;
        MoarBows.plugin.getArrowManager().getArrowData(arrow).ifPresent(data -> {
            if (data.canContinue() && UtilityMethods.canTarget(data.getShooter(), null, event.getEntity()))
                data.getBow().modifyHit(event, data, event.getEntity());
            else event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void a(EntityDamageByEntityEvent event) {
        net.Indyuce.moarbows.bow.effect.EffectDamage.observe(event);
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        MoarBows.plugin.getArrowManager().takeArrowData(arrow).ifPresent(data -> {
            if (event.isCancelled() || !UtilityMethods.canTarget(data.getShooter(), null, event.getEntity())) return;
            data.captureImpact();
            Bukkit.getScheduler().runTask(MoarBows.plugin, () -> {
                if (!event.isCancelled() && data.canContinue()
                        && MoarBows.plugin.getWorldGuard().isFlagAllowed(data.getImpactLocation(), net.Indyuce.moarbows.comp.worldguard.CustomFlag.MB_BOWS)
                        && (event.getEntity().isDead() || UtilityMethods.canTarget(data.getShooter(), null, event.getEntity())))
                    data.getBow().whenHit(event, data, event.getEntity());
            });
        });
    }
}
