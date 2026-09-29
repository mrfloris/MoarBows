package net.Indyuce.moarbows.manager;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArrowManagerTest {
    private ArrowMetadata shot(int reusedNumericId) {
        ArrowMetadata data = mock(ArrowMetadata.class);
        Arrow arrow = mock(Arrow.class);
        LivingEntity shooter = mock(LivingEntity.class);
        when(arrow.getEntityId()).thenReturn(reusedNumericId);
        when(arrow.getUniqueId()).thenReturn(UUID.randomUUID());
        when(arrow.isValid()).thenReturn(true);
        when(shooter.getUniqueId()).thenReturn(UUID.randomUUID());
        when(data.getArrow()).thenReturn(arrow);
        when(data.getShooter()).thenReturn(shooter);
        return data;
    }

    @Test
    void reusedEntityNumberDoesNotOverwriteAnotherShotAndImpactIsConsumedOnce() {
        ArrowManager manager = new ArrowManager();
        ArrowMetadata first = shot(7), second = shot(7);
        manager.registerArrow(first);
        manager.registerArrow(second);
        assertEquals(2, manager.getActive().size());
        assertSame(first, manager.takeArrowData(first.getArrow()).orElseThrow());
        assertTrue(manager.takeArrowData(first.getArrow()).isEmpty());
        assertSame(second, manager.getArrowData(second.getArrow()).orElseThrow());
    }

    @Test
    void deadAndExpiredArrowsAreFlushedAndCollectionCannotMutateTracking() {
        ArrowManager manager = new ArrowManager();
        ArrowMetadata dead = shot(1), expired = shot(2);
        when(dead.getArrow().isValid()).thenReturn(false);
        when(expired.hasTimedOut()).thenReturn(true);
        manager.registerArrow(dead);
        manager.registerArrow(expired);
        assertThrows(UnsupportedOperationException.class, () -> manager.getActive().clear());
        manager.flushArrowData();
        assertTrue(manager.getActive().isEmpty());
    }

    @Test
    void quitCleanupRemovesOnlyTheOwnersNativeAndEffectProjectiles() {
        ArrowManager manager = new ArrowManager();
        ArrowMetadata owner = shot(1), other = shot(2);
        Arrow effect = mock(Arrow.class);
        when(effect.getUniqueId()).thenReturn(UUID.randomUUID());
        manager.registerArrow(owner);
        manager.registerArrow(other);
        manager.trackEffect(effect, owner);
        manager.removeShooter(owner.getShooter().getUniqueId());
        verify(owner.getArrow()).remove();
        verify(effect).remove();
        verify(other.getArrow(), never()).remove();
        assertEquals(1, manager.getActive().size());
        manager.clear();
        verify(other.getArrow()).remove();
        assertTrue(manager.getActive().isEmpty());
    }
}
