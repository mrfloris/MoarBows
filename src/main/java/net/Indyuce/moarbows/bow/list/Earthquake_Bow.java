package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Earthquake_Bow extends MoarBow {
	public Earthquake_Bow() {
		super(new String[] { "Summons a shockwave when hitting", "anything, powerfully knocking up", "all enemies within &c{radius} &7blocks.",
				"Knock-up Force: &c{knockup}" }, new ParticleData(VParticle.REDSTONE.get(), Color.fromRGB(128, 0, 0), 2),
				new String[] { "DIRT,DIRT,DIRT", "DIRT,BOW,DIRT", "DIRT,DIRT,DIRT" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(10, -1, 3, 10)), new DoubleModifier("knockup", new LinearFormula(1, .5)),
				new DoubleModifier("radius", new LinearFormula(5, .2)));
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
		double radius = data.getDouble("radius");
		double knockup = data.getDouble("knockup");

		Location loc = data.getArrow().getLocation();
		for (int j = 0; j < 20; j++)
			if (loc.add(0, -1, 0).getBlock().getType().isSolid()) {
				loc.setY(Math.floor(loc.getY()) + 1);
				for (int k = 0; k < 64; k++) {
					double rx = (random.nextDouble() - .5) * 6;
					double rz = (random.nextDouble() - .5) * 6;
					loc.getWorld().spawnParticle(VParticle.BLOCK.get(), loc.clone().add(rx, 1, rz), 0, Material.DIRT.createBlockData());
				}
				break;
			}

		// needs a small delay because of the arrow knockback
		data.getArrow().remove();
		data.getArrow().getWorld().playSound(data.getArrow().getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 2, 0);
		new BukkitRunnable() {
			public void run() {
				for (Entity ent : data.getArrow().getNearbyEntities(radius, radius, radius))
					if (ent instanceof LivingEntity) {
						ent.playEffect(EntityEffect.HURT);
						ent.setVelocity(ent.getVelocity().setY(knockup));
					}
			}
		}.runTaskLater(MoarBows.plugin, 1);
	}
}
