package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.BooleanModifier;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Material;

public class Marked_Bow extends MoarBow implements Listener {
	private static final Map<UUID, Mark> marked = new HashMap<>();

	public Marked_Bow() {
		super(new String[] { "Arrows mark players. Hitting a", "marked player deals &c{extra}% &7additional", "damage. Milk dispels the mark." },
				new ParticleData(VParticle.WITCH.get()), new String[] { "COAL,COAL,COAL", "COAL,BOW,COAL", "COAL,COAL,COAL" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(10, -1, 3, 10)), new DoubleModifier("extra", new LinearFormula(40, 20)),
				new DoubleModifier("duration", new LinearFormula(6, 1)), new BooleanModifier("particles", true));
	}

	public static void clearAll() {
        List.copyOf(marked.values()).forEach(Mark::close);
    }

    public static boolean isMarked(Entity entity) {
		return marked.containsKey(entity.getUniqueId());
	}

	public static Mark getMark(Entity entity) {
		return marked.get(entity.getUniqueId());
	}

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		return true;
	}

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {

		if (!target.isValid() || target.isDead() || isMarked(target))
			return;

		playEffect(target.getLocation());
		new Mark(target, data.getDouble("extra"), data.getDouble("duration"));
		target.getWorld().playSound(target.getLocation(), Sounds.ENTITY_ENDERMAN_HURT, 2, 1.5f);
	}

	@Override
	public void whenLand(ArrowMetadata data) {
		// TODO Auto-generated method stub

	}

	private void playEffect(Location loc) {
		new BukkitRunnable() {
			double y = 0;

			public void run() {
				for (int j1 = 0; j1 < 3; j1++) {
					y += .07;
					for (int j = 0; j < 3; j++)
						loc.getWorld().spawnParticle(VParticle.REDSTONE.get(), loc.clone().add(Math.cos(y * Math.PI + (j * Math.PI * 2 / 3)) * (3 - y) / 2.5,
								y, Math.sin(y * Math.PI + (j * Math.PI * 2 / 3)) * (3 - y) / 2.5), 0, new Particle.DustOptions(Color.BLACK, 1));
				}
				if (y > 3)
					cancel();
			}
		}.runTaskTimer(MoarBows.plugin, 0, 1);
	}

	public class Mark extends BukkitRunnable implements Listener {
		private final Entity entity;
		private final double coef;
        private final BukkitTask expiry;
        private boolean closed;

		public Mark(Entity entity, double extra, double duration) {
			this.entity = entity;
			this.coef = 1 + extra / 100;

			marked.put(entity.getUniqueId(), this);

			Bukkit.getPluginManager().registerEvents(this, MoarBows.plugin);
			expiry = Bukkit.getScheduler().runTaskLater(MoarBows.plugin, this::close, Math.max(1, (long) (duration * 20)));
			runTaskTimer(MoarBows.plugin, 0, 9);
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void a(EntityDamageByEntityEvent event) {
			if (event.getEntity().equals(entity)) {
				event.setDamage(event.getDamage() * coef);
				playEffect(entity.getLocation());
				entity.getWorld().playSound(entity.getLocation(), Sounds.ENTITY_ENDERMAN_DEATH, 2, 2);
			}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void b(PlayerItemConsumeEvent event) {
			if (event.getPlayer().equals(entity) && event.getItem().getType() == Material.MILK_BUCKET) {
				entity.getWorld().playSound(entity.getLocation(), Sounds.ENTITY_BLAZE_AMBIENT, 2, 2);
				close();
			}
		}

		@EventHandler
		public void c(EntityDeathEvent event) {
			if (event.getEntity().equals(entity))
				close();
		}

		public void close() {
            if (closed) return;
            closed = true;
            expiry.cancel();
			marked.remove(entity.getUniqueId());
			HandlerList.unregisterAll(this);
			cancel();
		}

		@Override
		public void run() {
            if (!entity.isValid() || entity.isDead()) { close(); return; }
			for (double j = 0; j < Math.PI * 2; j += Math.PI / 18)
				entity.getWorld().spawnParticle(VParticle.SMOKE.get(), entity.getLocation().clone().add(Math.cos(j) * .7, .1, Math.sin(j) * .7), 0);
		}
	}
}
