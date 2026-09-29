package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import net.Indyuce.moarbows.bow.effect.EffectTask;
import org.bukkit.util.Vector;

public class Composite_Bow extends MoarBow {
	public Composite_Bow() {
		super(new String[] { "Fires enchanted arrows that", "follow a linear trajectory.", "Deals &c{damage} &7damage." },
				new ParticleData(VParticle.REDSTONE.get(), Color.fromRGB(91, 60, 17), 2),
				new String[] { "AIR,AIR,AIR", "BOW,NETHER_STAR,BOW", "AIR,AIR,AIR" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(2, 0)), new DoubleModifier("damage", new LinearFormula(8, 2)));
	}

    @Override
    public boolean usesVanillaArrow() {
        return false;
    }

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		final double dmg = data.getDouble("damage") * UtilityMethods.getPowerDamageMultiplier(data.getSource());

		data.getShooter().getWorld().playSound(data.getShooter().getLocation(), Sounds.ENTITY_ARROW_SHOOT, 2, 0);
		new EffectTask(data) {
			Location loc = data.getShooter().getEyeLocation();
			double ti = 0;
			double max = 20 * event.getForce();
			Vector v = data.getShooter().getEyeLocation().getDirection().multiply(1.25);

			public void tick() {
				for (double j = 0; j < 3; j++) {
					ti += .5;
					loc.add(v);
                    if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4) || loc.getBlock().getType().isSolid()) {
                        cancel();
                        return;
                    }
					loc.getWorld().spawnParticle(VParticle.CRIT.get(), loc, 8, .1, .1, .1, .1);
					loc.getWorld().playSound(loc, Sounds.BLOCK_NOTE_BLOCK_HAT, 3, 2);
					for (LivingEntity entity : loc.getWorld().getNearbyLivingEntities(loc, 2))
						if (UtilityMethods.canTarget(data.getShooter(), loc, entity) && !entity.equals(data.getShooter())) {
							entity.getWorld().playSound(entity.getLocation(), Sounds.ENTITY_FIREWORK_ROCKET_BLAST, 3, 0);
							loc.getWorld().spawnParticle(VParticle.LARGE_EXPLOSION.get(), loc, 0);
							cancel();
							entity.damage(dmg, data.getShooter());
							return;
						}
				}
				if (ti >= max)
					cancel();
			}
		}.runTaskTimer(MoarBows.plugin, 0, 1);
		return false;
	}

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
		// TODO Auto-generated method stub

	}

	@Override
	public void whenLand(ArrowMetadata data) {
		// TODO Auto-generated method stub

	}
}
