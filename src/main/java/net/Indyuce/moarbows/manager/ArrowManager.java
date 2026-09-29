package net.Indyuce.moarbows.manager;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Main-thread ownership and one-shot dispatch for custom projectiles. */
public class ArrowManager {
    private final Map<UUID, ArrowMetadata> map = new HashMap<>();
    private final Map<UUID, TrackedEntity> effects = new HashMap<>();
    private final Map<UUID, ArrowMetadata> explosions = new HashMap<>();

    public void registerArrow(ArrowMetadata data) {
        map.put(data.getArrow().getUniqueId(), data);
    }

    public void unregisterArrow(Arrow arrow) {
        map.remove(arrow.getUniqueId());
    }

    /** Remove before invoking an effect, including reentrant damage callbacks. */
    public Optional<ArrowMetadata> takeArrowData(Arrow arrow) {
        return Optional.ofNullable(map.remove(arrow.getUniqueId()));
    }

    public Collection<ArrowMetadata> getActive() {
        return List.copyOf(map.values());
    }

    public Optional<ArrowMetadata> getArrowData(Arrow arrow) {
        return Optional.ofNullable(map.get(arrow.getUniqueId()));
    }

    public <T extends Entity> T trackEffect(T entity, ArrowMetadata data) {
        effects.put(entity.getUniqueId(), new TrackedEntity(entity, data, System.currentTimeMillis()));
        return entity;
    }

    public Optional<ArrowMetadata> getEffectData(Entity entity) {
        TrackedEntity tracked = effects.get(entity.getUniqueId());
        return tracked == null ? Optional.ofNullable(explosions.get(entity.getUniqueId())) : Optional.of(tracked.data());
    }

    /** Attribute synchronous explosion callbacks to their originating bow, including block protection. */
    public void createExplosion(ArrowMetadata data, float radius) {
        UUID owner = data.getShooter().getUniqueId();
        ArrowMetadata previous = explosions.put(owner, data);
        try {
            data.getImpactLocation().getWorld().createExplosion(data.getImpactLocation(), radius, false, true, data.getShooter());
        } finally {
            if (previous == null) explosions.remove(owner);
            else explosions.put(owner, previous);
        }
    }

    public void flushArrowData() {
        map.values().removeIf(data -> data.hasTimedOut() || !data.getArrow().isValid() || data.getArrow().isDead());
        effects.values().removeIf(tracked -> {
            if (System.currentTimeMillis() - tracked.created() > 60_000) {
                tracked.entity().remove();
                return true;
            }
            return !tracked.entity().isValid() || tracked.entity().isDead();
        });
    }

    public void removeShooter(UUID shooter) {
        map.values().removeIf(data -> {
            if (!data.getShooter().getUniqueId().equals(shooter)) return false;
            data.getArrow().remove();
            return true;
        });
        effects.values().removeIf(tracked -> {
            if (!tracked.data().getShooter().getUniqueId().equals(shooter)) return false;
            tracked.entity().remove();
            return true;
        });
    }

    public void clear() {
        map.values().forEach(data -> data.getArrow().remove());
        effects.values().forEach(tracked -> tracked.entity().remove());
        map.clear();
        effects.clear();
        explosions.clear();
    }

    private record TrackedEntity(Entity entity, ArrowMetadata data, long created) {}

    @Deprecated
    public ArrowMetadata safeGetArrowData(Arrow arrow) {
        return map.get(arrow.getUniqueId());
    }

    @Deprecated
    public boolean isCustomArrow(Arrow arrow) {
        return map.containsKey(arrow.getUniqueId());
    }
}
