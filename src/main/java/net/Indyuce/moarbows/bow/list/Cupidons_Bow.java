package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.Attributes;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Cupidons_Bow extends MoarBow {
	public Cupidons_Bow() {
		super("CUPIDONS_BOW", "&fCupidon's Bow",
				new String[] { "Arrows heal players for &a{heal} &7hearts.", "Also unmarks (&nMarked Bow&7) players." }, 0,
				new ParticleData(VParticle.HEART.get()),
				new String[] { "GLISTERING_MELON_SLICE,GLISTERING_MELON_SLICE,GLISTERING_MELON_SLICE",
						"GLISTERING_MELON_SLICE,BOW,GLISTERING_MELON_SLICE",
						"GLISTERING_MELON_SLICE,GLISTERING_MELON_SLICE,GLISTERING_MELON_SLICE" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(0, 0)), new DoubleModifier("heal", new LinearFormula(4, 3)));
	}

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		return true;
	}

    @Override
    public void modifyHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
        if (target instanceof LivingEntity) event.setDamage(0);
    }

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
		if (!(target instanceof LivingEntity) || !target.isValid() || target.isDead())
			return;

		target.getWorld().spawnParticle(VParticle.HEART.get(), target.getLocation().add(0, target.getHeight(), 0), 16, 1, 1, 1);
		target.getWorld().playSound(target.getLocation(), Sounds.ENTITY_BLAZE_AMBIENT, 2, 2);
		double max = ((LivingEntity) target).getAttribute(Attributes.MAX_HEALTH).getValue();
		((LivingEntity) target).setHealth(Math.min(max, ((LivingEntity) target).getHealth() + data.getDouble("heal")));

		if (Marked_Bow.isMarked(target))
			Marked_Bow.getMark(target).close();
	}

	@Override
	public void whenLand(ArrowMetadata data) {
	}
}
