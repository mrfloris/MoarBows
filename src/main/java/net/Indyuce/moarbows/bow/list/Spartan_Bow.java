package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import net.Indyuce.moarbows.bow.effect.EffectTask;

public class Spartan_Bow extends MoarBow {
	public Spartan_Bow() {
		super(new String[] { "Summons a flurry of arrows from", "the sky when hitting a target." },
				new ParticleData(VParticle.REDSTONE.get(), Color.fromRGB(180, 180, 180), 2),
				new String[] { "BOW,EMERALD,BOW", "EMERALD,BOW,EMERALD", "BOW,EMERALD,BOW" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(25, -3, 10, 25)), new DoubleModifier("duration", new LinearFormula(1.5, .5)));
	}

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		return true;
	}

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
		whenLand(data);
	}

	@Override
	public void whenLand(ArrowMetadata data) {
		data.getArrow().remove();

		final Location loc1 = data.getImpactLocation().clone();
		double randomOffset = Math.PI * 4 * (random.nextDouble() - .5);
		Location sky = data.getImpactLocation().clone().add(Math.cos(randomOffset) * 6, 13, Math.sin(randomOffset) * 6);
		final double duration = data.getDouble("duration");
		new EffectTask(data) {
			double ti = 0;

			public void tick() {
				if ((ti += 3d / 20d) > duration) {
					cancel();
					return;
					}

				sky.getWorld().spawnParticle(VParticle.LARGE_SMOKE.get(), sky, 0);
                Arrow arrow1 = sky.getWorld().spawn(sky, Arrow.class, arrow -> {
                    arrow.setShooter(data.getShooter());
                    arrow.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
                    arrow.setPersistent(false);
                });
                MoarBows.plugin.getArrowManager().trackEffect(arrow1, data);

				arrow1.setVelocity(loc1.clone().add(8 * (random.nextDouble() - .5), 0, 8 * (random.nextDouble() - .5)).toVector()
						.subtract(sky.toVector()).normalize());
			}
		}.runTaskTimer(MoarBows.plugin, 0, 3);
	}
}
