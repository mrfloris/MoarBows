package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.effect.EffectDamage;
import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.util.SmallParticleEffect;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Fire_Bow extends MoarBow {
	public Fire_Bow() {
		super(new String[] { "Shoots burning arrows that cause a", "first burst upon landing, igniting", "any entity within &c{radius} &7blocks.",
				"Ignite duration: &c{ignite} &7seconds" }, new ParticleData(VParticle.FLAME.get()),
				new String[] { "BLAZE_ROD,BLAZE_ROD,BLAZE_ROD", "BLAZE_ROD,BOW,BLAZE_ROD", "BLAZE_ROD,BLAZE_ROD,BLAZE_ROD" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(0, 0)), new DoubleModifier("radius", new LinearFormula(5, 1)),
				new DoubleModifier("ignite", new LinearFormula(4, 2)), new DoubleModifier("max-burning-time", new LinearFormula(8, 2)));
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
		int duration = (int) (data.getDouble("ignite") * 20);
		int maxTicks = (int) (data.getDouble("max-burning-time") * 20);
		double radius = data.getDouble("radius");

		data.getArrow().remove();
		data.getArrow().getWorld().spawnParticle(VParticle.LARGE_EXPLOSION.get(), data.getImpactLocation(), 0);
		data.getArrow().getWorld().spawnParticle(VParticle.LAVA.get(), data.getImpactLocation(), 12, 0, 0, 0);
		data.getArrow().getWorld().spawnParticle(VParticle.FLAME.get(), data.getImpactLocation(), 48, 0, 0, 0, .13);
		data.getArrow().getWorld().playSound(data.getImpactLocation(), Sounds.ENTITY_FIREWORK_ROCKET_BLAST, 3, 1);
		for (Entity entity : data.getNearbyEntities(radius, radius, radius))
			if (entity instanceof LivingEntity && UtilityMethods.canTarget(data.getShooter(), null, entity)) {
				new SmallParticleEffect(entity, VParticle.FLAME.get());
				EffectDamage.ignite(entity, data.getShooter(), Math.min(entity.getFireTicks() + duration, maxTicks));
			}
	}
}
