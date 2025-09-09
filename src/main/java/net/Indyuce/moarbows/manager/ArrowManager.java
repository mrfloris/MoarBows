package net.Indyuce.moarbows.manager;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.util.UtilityMethods;
import org.bukkit.entity.Arrow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ArrowManager {
    private final Map<Integer, ArrowMetadata> map = new HashMap<>();

    public void registerArrow(ArrowMetadata data) {
        map.put(data.getArrow().getEntityId(), data);
    }

    public void unregisterArrow(Arrow arrow) {
        map.remove(arrow.getEntityId());
    }

    public Collection<ArrowMetadata> getActive() {
        return map.values();
    }

    public Optional<ArrowMetadata> getArrowData(Arrow arrow) {
        return map.containsKey(arrow.getEntityId()) ? Optional.of(map.get(arrow.getEntityId())) : Optional.empty();
    }

    public void flushArrowData() {
        UtilityMethods.clean(map.values(), arrow -> arrow.hasTimedOut());
    }

    //region Deprecated

    @Deprecated
    public ArrowMetadata safeGetArrowData(Arrow arrow) {
        return map.getOrDefault(arrow.getEntityId(), null);
    }

    @Deprecated
    public boolean isCustomArrow(Arrow arrow) {
        return map.containsKey(arrow.getEntityId());
    }

    //endregion
}
