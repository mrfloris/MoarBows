package net.Indyuce.moarbows.bow.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class EffectDamageTest {
    @Test
    void protectionCancellationPreventsSecondaryFireOrKnockback() {
        LivingEntity shooter = mock(LivingEntity.class), target = mock(LivingEntity.class);
        Runnable followup = mock(Runnable.class);
        EntityDamageByEntityEvent damage = event(shooter, target);
        doAnswer(invocation -> {
            EffectDamage.observe(damage);
            // Simulate a listener after our MONITOR handler cancelling the damage.
            when(damage.isCancelled()).thenReturn(true);
            return null;
        }).when(target).damage(4, shooter);
        EffectDamage.afterDamage(target, 4, shooter, followup);
        verifyNoInteractions(followup);
    }

    @Test
    void acceptedDamageRunsFollowupOnceAndAbsentDamageFailsClosed() {
        LivingEntity shooter = mock(LivingEntity.class), target = mock(LivingEntity.class);
        Runnable followup = mock(Runnable.class);
        EntityDamageByEntityEvent damage = event(shooter, target);
        doAnswer(invocation -> { EffectDamage.observe(damage); return null; }).when(target).damage(4, shooter);
        EffectDamage.afterDamage(target, 4, shooter, followup);
        EffectDamage.afterDamage(target, 5, shooter, followup);
        verify(followup, times(1)).run();
    }

    @Test
    void unrelatedNestedDamageDoesNotAuthorizeTheOriginalEffect() {
        LivingEntity shooter = mock(LivingEntity.class), target = mock(LivingEntity.class), other = mock(LivingEntity.class);
        Runnable followup = mock(Runnable.class);
        EntityDamageByEntityEvent damage = event(shooter, other);
        doAnswer(invocation -> { EffectDamage.observe(damage); return null; }).when(target).damage(4, shooter);
        EffectDamage.afterDamage(target, 4, shooter, followup);
        verifyNoInteractions(followup);
    }

    private EntityDamageByEntityEvent event(LivingEntity shooter, LivingEntity target) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(target);
        when(event.getDamager()).thenReturn(shooter);
        return event;
    }
}
