package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Hunter_Bow extends MoarBow {
	public Hunter_Bow() {
		super(new String[] { "Arrows deal &c{extra}% &7additional", "damage to friendly mobs." },
				new ParticleData(VParticle.REDSTONE.get(), Color.fromRGB(255, 0, 0), 2),
				new String[] { "CHICKEN,BEEF,CHICKEN", "BEEF,BOW,BEEF", "CHICKEN,BEEF,CHICKEN" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(0, 0)), new DoubleModifier("extra", new LinearFormula(75, 25)));
	}

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		return true;
	}

    @Override
    public void modifyHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
        if (target instanceof LivingEntity && !(target instanceof Monster)) event.setDamage(event.getDamage() * (1 + data.getDouble("extra") / 100));
    }

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
		if (target instanceof Monster || !(target instanceof LivingEntity))
			return;

		target.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, target.getLocation(), 12, .3, .3, .3, org.bukkit.Material.REDSTONE_WIRE.createBlockData());
	}

	@Override
	public void whenLand(ArrowMetadata data) {
	}
}
