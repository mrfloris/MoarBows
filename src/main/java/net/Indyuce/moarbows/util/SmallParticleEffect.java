package net.Indyuce.moarbows.util;

import net.Indyuce.moarbows.MoarBows;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class SmallParticleEffect extends BukkitRunnable {
	private final Location loc;
	private final Particle particle;
	private final double r;

	private double t;

	public SmallParticleEffect(Entity entity, Particle particle) {
		this(entity, particle, .7);
	}

	public SmallParticleEffect(Entity entity, Particle particle, double r) {
		this.loc = entity.getLocation().add(0, entity.getHeight() / 4, 0);
		this.particle = particle;
		this.r = r;

		runTaskTimer(MoarBows.plugin, 0, 1);
	}

	public void run() {
		if (t > Math.PI * 2 || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            cancel();
            return;
        }

		for (int k = 0; k < 3; k++) {
			t += Math.PI / 10;
			loc.getWorld().spawnParticle(particle, loc.clone().add(r * Math.cos(t), t / Math.PI / 2, r * Math.sin(t)), 0);
		}
	}
}
