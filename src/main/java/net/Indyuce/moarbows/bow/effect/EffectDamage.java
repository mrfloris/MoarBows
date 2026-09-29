package net.Indyuce.moarbows.bow.effect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;

/** Main-thread effect operations that preserve cancellation by protection plugins. */
public final class EffectDamage {
    private static final Deque<Attempt> attempts = new ArrayDeque<>();

    private EffectDamage() {}

    public static void afterDamage(LivingEntity target, double amount, LivingEntity shooter, Runnable accepted) {
        Attempt attempt = new Attempt(target, shooter);
        attempts.push(attempt);
        try {
            target.damage(amount, shooter);
        } finally {
            attempts.pop();
        }
        // Read after damage() returns, so later MONITOR listeners have also finished.
        if (attempt.event != null && !attempt.event.isCancelled() && !target.isDead()) accepted.run();
    }

    public static void observe(EntityDamageByEntityEvent event) {
        Attempt current = attempts.peek();
        if (current != null && event.getEntity().equals(current.target) && event.getDamager().equals(current.shooter))
            current.event = event;
    }

    public static void ignite(Entity target, LivingEntity shooter, int ticks) {
        EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(shooter, target, Math.max(0, ticks) / 20F);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) target.setFireTicks(Math.max(0, Math.round(event.getDuration() * 20)));
    }

    public static void push(Entity target, LivingEntity shooter, Vector velocity) {
        if (!(target instanceof LivingEntity living)) return;
        velocity.checkFinite();
        EntityKnockbackByEntityEvent event = new EntityKnockbackByEntityEvent(living, shooter,
                EntityKnockbackEvent.Cause.ENTITY_ATTACK, (float) velocity.length(), velocity.clone());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) living.setVelocity(event.getKnockback());
    }

    private static final class Attempt {
        private final LivingEntity target;
        private final LivingEntity shooter;
        private EntityDamageByEntityEvent event;

        private Attempt(LivingEntity target, LivingEntity shooter) {
            this.target = target;
            this.shooter = shooter;
        }
    }
}
