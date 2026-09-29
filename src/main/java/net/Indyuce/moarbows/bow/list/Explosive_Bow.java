package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Explosive_Bow extends MoarBow {
	public Explosive_Bow() {
		super(new String[] { "Arrows explode when landing, deal", "&c{damage} &7damage to nearby entities." },
				new ParticleData(VParticle.EXPLOSION.get()), new String[] { "TNT,TNT,TNT", "TNT,BOW,TNT", "TNT,TNT,TNT" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(0, 0)), new DoubleModifier("damage", new LinearFormula(8, 4)));
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
		double dmg = data.getDouble("damage");
		data.getArrow().remove();
		data.getArrow().getWorld().spawnParticle(VParticle.LARGE_EXPLOSION.get(), data.getImpactLocation(), 16, 1.5, 1.5, 1.5);
		data.getArrow().getWorld().spawnParticle(VParticle.EXPLOSION.get(), data.getImpactLocation(), 48, 0, 0, 0, .4);
		data.getArrow().getWorld().playSound(data.getImpactLocation(), Sounds.ENTITY_GENERIC_EXPLODE, 3, 1);
		for (Entity ent : data.getNearbyEntities(5, 5, 5))
			if (ent instanceof LivingEntity && UtilityMethods.canTarget(data.getShooter(), null, ent))
				((LivingEntity) ent).damage(dmg, data.getShooter());
	}
}
