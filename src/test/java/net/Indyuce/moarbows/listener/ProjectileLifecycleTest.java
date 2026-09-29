package net.Indyuce.moarbows.listener;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.api.event.MoarBowShootEvent;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.comp.worldguard.WGPlugin;
import net.Indyuce.moarbows.comp.worldguard.CustomFlag;
import net.Indyuce.moarbows.manager.ArrowManager;
import net.Indyuce.moarbows.manager.BowManager;
import net.Indyuce.moarbows.manager.ConfigManager;
import net.Indyuce.moarbows.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProjectileLifecycleTest {
    private MockedStatic<Bukkit> bukkit;
    private MoarBows plugin;
    private BowManager bows;
    private ArrowManager arrows;
    private PluginManager plugins;
    private World world;
    private WGPlugin protection;
    private MoarBow bow;
    private Player player;
    private ItemStack item;
    private final List<Runnable> queued = new ArrayList<>();

    @BeforeEach void setup() {
        plugin = mock(MoarBows.class);
        MoarBows.plugin = plugin;
        bows = mock(BowManager.class);
        arrows = new ArrowManager();
        plugins = mock(PluginManager.class);
        protection = mock(WGPlugin.class);
        world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getPVP()).thenReturn(true);
        when(plugin.getBowManager()).thenReturn(bows);
        when(plugin.getArrowManager()).thenReturn(arrows);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getLanguage()).thenReturn(mock(ConfigManager.class));
        when(plugin.getWorldGuard()).thenReturn(protection);
        when(protection.isFlagAllowed(any(Location.class), any())).thenReturn(true);
        when(protection.isFlagAllowed(any(Player.class), any())).thenReturn(true);
        when(protection.isPvpAllowed(any())).thenReturn(true);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(player.isValid()).thenReturn(true);
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission(anyString())).thenReturn(true);
        bow = mock(MoarBow.class);
        when(bow.getId()).thenReturn("AUTOBOW");
        when(bow.getLowerCaseId()).thenReturn("autobow");
        when(bow.getDouble("cooldown", 1)).thenReturn(8.0);
        item = mock(ItemStack.class);
        when(item.clone()).thenReturn(item);
        when(bows.get(item)).thenReturn(bow);
        when(bows.getLevel(item)).thenReturn(1);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        doAnswer(invocation -> { queued.add(invocation.getArgument(1)); return null; })
                .when(scheduler).runTask(eq(plugin), any(Runnable.class));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
    }

    @AfterEach void cleanup() {
        bukkit.close();
        PlayerData.clearAll();
        MoarBows.plugin = null;
    }

    private Arrow arrow() {
        Arrow arrow = mock(Arrow.class);
        when(arrow.getUniqueId()).thenReturn(UUID.randomUUID());
        when(arrow.getWorld()).thenReturn(world);
        when(arrow.getLocation()).thenReturn(new Location(world, 1, 64, 0));
        when(arrow.isValid()).thenReturn(true);
        return arrow;
    }

    private EntityShootBowEvent shot() {
        return new EntityShootBowEvent(player, item, null, arrow(), EquipmentSlot.HAND, 1F, true);
    }

    @Test void alreadyCancelledShotsAreIgnored() {
        EntityShootBowEvent event = shot();
        event.setCancelled(true);
        ShootBow listener = new ShootBow();
        listener.a(event);
        listener.finish(event);
        verify(bows, never()).get(any(ItemStack.class));
        assertTrue(queued.isEmpty());
    }

    @Test void cancelledCustomEventCancelsNativeShotWithoutCooldownOrAbility() {
        doAnswer(invocation -> {
            ((MoarBowShootEvent) invocation.getArgument(0)).setCancelled(true);
            return null;
        }).when(plugins).callEvent(any(MoarBowShootEvent.class));
        EntityShootBowEvent event = shot();
        ShootBow listener = new ShootBow();
        listener.a(event);
        listener.finish(event);
        assertTrue(event.isCancelled());
        assertFalse(PlayerData.get(player).hasCooldown(bow, 1));
        verify(bow, never()).canShoot(any(), any());
        assertTrue(queued.isEmpty());
    }

    @Test void protectionCancellationAfterValidationPreventsAbilityAndCooldown() {
        EntityShootBowEvent event = shot();
        ShootBow listener = new ShootBow();
        listener.a(event);
        event.setCancelled(true);
        listener.finish(event);
        assertFalse(PlayerData.get(player).hasCooldown(bow, 1));
        assertTrue(queued.isEmpty());
        verify(bow, never()).canShoot(any(), any());
    }

    @Test void replacementShotKeepsNativeConsumptionAndReservesCooldownAgainstRapidFire() {
        EntityShootBowEvent first = shot();
        ShootBow listener = new ShootBow();
        listener.a(first);
        listener.finish(first);
        assertFalse(first.isCancelled());
        assertTrue(first.shouldConsumeItem());
        verify(bow, never()).canShoot(any(), any());
        EntityShootBowEvent rapid = shot();
        listener.a(rapid);
        assertTrue(rapid.isCancelled());
        queued.getFirst().run();
        verify(bow).canShoot(eq(first), any());
        verify(first.getProjectile()).remove();
    }

    @Test void lethalAcceptedHitStillTriggersOneImpactEffectAtTheCapturedLocation() {
        LivingEntity target = mock(LivingEntity.class);
        when(target.isValid()).thenReturn(true);
        when(target.getWorld()).thenReturn(world);
        when(target.getLocation()).thenReturn(new Location(world, 1, 64, 0));
        Arrow arrow = arrow();
        ArrowMetadata data = mock(ArrowMetadata.class);
        when(data.getArrow()).thenReturn(arrow);
        when(data.getShooter()).thenReturn(player);
        when(data.getBow()).thenReturn(bow);
        when(data.canContinue()).thenReturn(true);
        when(data.getImpactLocation()).thenReturn(new Location(world, 1, 64, 0));
        arrows.registerArrow(data);
        EntityDamageByEntityEvent damage = mock(EntityDamageByEntityEvent.class);
        when(damage.getDamager()).thenReturn(arrow);
        when(damage.getEntity()).thenReturn(target);
        HitEntity listener = new HitEntity();
        listener.a(damage);
        listener.a(damage);
        when(target.isDead()).thenReturn(true);
        when(target.isValid()).thenReturn(false);
        assertEquals(1, queued.size());
        queued.getFirst().run();
        verify(data).captureImpact();
        verify(bow).whenHit(damage, data, target);
        assertTrue(arrows.getActive().isEmpty());
    }

    @Test void cancelledLandingConsumesMetadataWithoutSchedulingAnEffect() {
        Arrow arrow = arrow();
        ArrowMetadata data = mock(ArrowMetadata.class);
        when(data.getArrow()).thenReturn(arrow);
        arrows.registerArrow(data);
        ProjectileHitEvent event = mock(ProjectileHitEvent.class);
        when(event.getEntity()).thenReturn(arrow);
        when(event.isCancelled()).thenReturn(true);
        new ArrowLand().a(event);
        assertTrue(arrows.getActive().isEmpty());
        assertTrue(queued.isEmpty());
        verify(data, never()).getBow();
    }
}
