package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Silver_Bow extends MoarBow {
	public Silver_Bow() {
		super(new String[] { "Arrows deal &c{extra}% &7additional damage." }, new ParticleData(VParticle.CRIT.get()),
				new String[] { "IRON_INGOT,IRON_INGOT,IRON_INGOT", "IRON_INGOT,BOW,IRON_INGOT", "IRON_INGOT,IRON_INGOT,IRON_INGOT" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(0, 0)), new DoubleModifier("extra", new LinearFormula(40, 30)),
				new DoubleModifier("block-effect-id", new LinearFormula(12, 0)));
	}

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		return true;
	}

    @Override
    public void modifyHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
        if (target instanceof LivingEntity) event.setDamage(event.getDamage() * (1. + data.getDouble("extra") / 100.));
    }

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
		if (!(target instanceof LivingEntity))
			return;

		// The legacy numeric block-effect-id is retained in config; modern Paper requires BlockData.
        org.bukkit.block.data.BlockData block = org.bukkit.Material.SAND.createBlockData();
		target.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, target.getLocation(), 12, .3, .3, .3, block);
	}

	@Override
	public void whenLand(ArrowMetadata data) {
	}
}
