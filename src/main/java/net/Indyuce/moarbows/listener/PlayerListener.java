package net.Indyuce.moarbows.listener;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.list.Marked_Bow;
import net.Indyuce.moarbows.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    @EventHandler
    public void a(PlayerJoinEvent event) {
        PlayerData.setup(event.getPlayer());
    }

    @EventHandler
    public void b(PlayerQuitEvent event) {
        cleanup(event.getPlayer());
        PlayerData.get(event.getPlayer()).logOff();
    }

    @EventHandler
    public void changedWorld(PlayerChangedWorldEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler
    public void died(PlayerDeathEvent event) {
        cleanup(event.getEntity());
    }

    private void cleanup(Player player) {
        PlayerData.get(player).invalidateEffects();
        MoarBows.plugin.getArrowManager().removeShooter(player.getUniqueId());
        if (Marked_Bow.isMarked(player)) Marked_Bow.getMark(player).close();
    }
}
