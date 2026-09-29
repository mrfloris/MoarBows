package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.effect.EffectDamage;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import net.Indyuce.moarbows.bow.effect.EffectTask;
import org.bukkit.util.Vector;

public class Blaze_Bow extends MoarBow {
	public Blaze_Bow() {
		super(new String[] { "Shoots a long ranged firebolt that", "deals &c{damage} &7damage to the first entity it",
				"hits, igniting him for &c{duration} &7seconds." }, new ParticleData(VParticle.FLAME.get()),
				new String[] { "MAGMA_CREAM,MAGMA_CREAM,MAGMA_CREAM", "MAGMA_CREAM,BOW,MAGMA_CREAM", "MAGMA_CREAM,MAGMA_CREAM,MAGMA_CREAM" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(10, -1, 3, 10)), new DoubleModifier("damage", new LinearFormula(8, 2)),
				new DoubleModifier("duration", new LinearFormula(4, .3)));
	}

    @Override
    public boolean usesVanillaArrow() {
        return false;
    }

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		final double dmg = data.getDouble("damage") * UtilityMethods.getPowerDamageMultiplier(data.getSource());
		final double duration = data.getDouble("duration");

		new EffectTask(data) {
			Location loc = data.getShooter().getEyeLocation();
			double ti = 0;
			Vector v = data.getShooter().getEyeLocation().getDirection().multiply(1.25);

			public void tick() {
				for (double j = 0; j < 3; j++) {
					ti += .5;
					loc.add(v);
                    if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4) || loc.getBlock().getType().isSolid()) {
                        cancel();
                        return;
                    }
					loc.getWorld().spawnParticle(VParticle.FLAME.get(), loc, 8, .1, .1, .1, 0);
					loc.getWorld().spawnParticle(VParticle.SMOKE.get(), loc, 0);
					loc.getWorld().playSound(loc, Sounds.BLOCK_NOTE_BLOCK_HAT, 3, 2);
					for (LivingEntity entity : loc.getWorld().getNearbyLivingEntities(loc, 2))
						if (UtilityMethods.canTarget(data.getShooter(), loc, entity) && !entity.equals(data.getShooter())) {
							new EffectTask(data) {
								final Location loc2 = entity.getLocation();
								double y = 0;

								public void tick() {
									for (int item = 0; item < 2; item++) {
										y += .05;
										for (int j = 0; j < 2; j++) {
											double xz = y * Math.PI * .8 + (j * Math.PI);
											loc.getWorld().spawnParticle(VParticle.FLAME.get(), loc2.clone().add(Math.cos(xz) * 1.3, y, Math.sin(xz) * 1.3),
													0);

										}
									}
									if (y >= 2.5)
										cancel();
								}
							}.runTaskTimer(MoarBows.plugin, 0, 1);
							entity.getWorld().playSound(entity.getLocation(), Sounds.ENTITY_FIREWORK_ROCKET_BLAST, 3, 0);
							loc.getWorld().spawnParticle(VParticle.LARGE_EXPLOSION.get(), entity.getLocation().add(0, 1, 0), 0);
							cancel();
							EffectDamage.afterDamage(entity, dmg, data.getShooter(),
                                    () -> EffectDamage.ignite(entity, data.getShooter(), (int) (duration * 20)));
							return;
						}
				}
				if (ti >= 20 * event.getForce())
					cancel();
			}
		}.runTaskTimer(MoarBows.plugin, 0, 1);
		return false;
	}

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
	}

	@Override
	public void whenLand(ArrowMetadata data) {
	}
}
