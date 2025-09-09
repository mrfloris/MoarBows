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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Autobow extends MoarBow {
	public Autobow() {
		super(new String[] { "Shoots a flurry of &c{arrows} &7arrows.", "The number depends on the", "bow pull force." },
				new ParticleData(VParticle.CRIT.get()), new String[] { "BOW,BOW,BOW", "BOW,NETHER_STAR,BOW", "BOW,BOW,BOW" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(8, -1, 3, 8)), new DoubleModifier("arrows", new LinearFormula(8, -1, 3, 8)));
	}

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		event.setCancelled(true);
		new BukkitRunnable() {
			int ti = 0;

			public void run() {
				if (ti++ > 20 * event.getForce() || !UtilityMethods.consumeAmmo(data.getShooter(), new ItemStack(Material.ARROW))) {
					cancel();
					return;
				}

				Location loc = data.getShooter().getEyeLocation().clone();
				loc.getWorld().spawnParticle(VParticle.CRIT.get(), loc, 6, .2, .2, .2, 0);
				data.getShooter().getWorld().playSound(data.getShooter().getLocation(), Sounds.ENTITY_ARROW_SHOOT, 1, 1.5f);
				loc.setPitch(loc.getPitch() + random.nextInt(3) - 1);
				loc.setYaw(loc.getYaw() + random.nextInt(3) - 1);
				data.getShooter().launchProjectile(Arrow.class).setVelocity(loc.getDirection().multiply(3.3 * event.getForce()));
			}
		}.runTaskTimer(MoarBows.plugin, 0, 2);
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
