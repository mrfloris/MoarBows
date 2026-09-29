package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.effect.EffectDamage;
import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import net.Indyuce.moarbows.bow.effect.EffectTask;
import org.bukkit.util.Vector;

public class Gravity_Bow extends MoarBow {
	public Gravity_Bow() {
		super(new String[] { "Shoots arrows that attract", "your target to yourself." }, new ParticleData(VParticle.INSTANT_EFFECT.get()),
				new String[] { "AIR,FISHING_ROD,AIR", "AIR,BOW,AIR", "AIR,AIR,AIR" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(0, 0)), new DoubleModifier("force", new LinearFormula(2.5, .5)),
				new DoubleModifier("y-static", new LinearFormula(.3, .05)));
	}

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		return true;
	}

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
		double force = data.getDouble("force");
		double ystatic = data.getDouble("y-static");
		new EffectTask(data) {
			public void tick() {
				if (!UtilityMethods.canTarget(data.getShooter(), null, target)) return;
                Vector v = data.getShooter().getLocation().toVector().subtract(target.getLocation().toVector());
                if (v.lengthSquared() < 0.0001) return;
                v.normalize();
				v.setX(v.getX() * force);
				v.setY(ystatic);
				v.setZ(v.getZ() * force);

				EffectDamage.push(target, data.getShooter(), v);
			}
		}.runTaskLater(MoarBows.plugin, 1);
	}

	@Override
	public void whenLand(ArrowMetadata data) {
	}
}
