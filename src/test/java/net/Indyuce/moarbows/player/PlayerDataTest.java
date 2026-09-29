package net.Indyuce.moarbows.player;

import net.Indyuce.moarbows.bow.MoarBow;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerDataTest {
    @AfterEach void clear() { PlayerData.clearAll(); }

    @Test
    void cooldownSurvivesReconnectAndOriginalEffectsRemainInvalidated() {
        UUID uuid = UUID.randomUUID();
        Player first = player(uuid), reconnect = player(uuid);
        MoarBow bow = mock(MoarBow.class);
        when(bow.getId()).thenReturn("FIRE_BOW");
        when(bow.getDouble("cooldown", 2)).thenReturn(30.0);
        PlayerData data = PlayerData.setup(first);
        long session = data.getEffectSession();
        data.applyCooldown(bow, 2);
        assertTrue(data.hasCooldown(bow, 2));
        data.logOff();
        PlayerData restored = PlayerData.setup(reconnect);
        assertSame(data, restored);
        assertSame(reconnect, restored.getPlayer());
        assertTrue(restored.hasCooldown(bow, 1));
        assertNotEquals(session, restored.getEffectSession());
    }

    @Test
    void offlinePlayersWithoutCooldownsAreReleased() {
        Player player = player(UUID.randomUUID());
        PlayerData old = PlayerData.setup(player);
        old.logOff();
        PlayerData.pruneOffline();
        assertThrows(NullPointerException.class, () -> PlayerData.get(player));
        assertNotSame(old, PlayerData.setup(player));
    }

    private Player player(UUID id) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }
}
