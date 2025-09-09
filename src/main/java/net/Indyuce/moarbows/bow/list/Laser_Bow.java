package net.Indyuce.moarbows.bow.list;

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
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Laser_Bow extends MoarBow {
	public Laser_Bow() {
		super(new String[] { "Fires instant laser arrows", "that deals &c{damage} &7damage to", "every entity it hits." },
				new ParticleData(VParticle.REDSTONE.get(), Color.fromRGB(255, 0, 0), 2), new String[] { "REDSTONE_BLOCK,REDSTONE_BLOCK,REDSTONE_BLOCK",
						"REDSTONE_BLOCK,BOW,REDSTONE_BLOCK", "REDSTONE_BLOCK,REDSTONE_BLOCK,REDSTONE_BLOCK" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(0, 0)), new DoubleModifier("damage", new LinearFormula(5, 3)));
	}

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		double damage = data.getDouble("damage") * UtilityMethods.getPowerDamageMultiplier(data.getSource());
		if (!UtilityMethods.consumeAmmo(data.getShooter(), new ItemStack(Material.ARROW)))
			return false;

		data.getShooter().getWorld().playSound(data.getShooter().getLocation(), Sounds.ENTITY_ARROW_SHOOT, 2, 0);
		int range = (int) (56 * event.getForce());
		Location loc = data.getShooter().getEyeLocation();
		List<Integer> hit = new ArrayList<>();
		for (int j = 0; j < range; j++) {
			loc.add(data.getShooter().getEyeLocation().getDirection());
			loc.getWorld().spawnParticle(VParticle.REDSTONE.get(), loc, 0, new Particle.DustOptions(Color.RED, 1.2f));
			if (loc.getBlock().getType().isSolid())
				break;

			for (Entity target : data.getShooter().getNearbyEntities(100, 100, 100))
				if (!hit.contains(target.getEntityId()) && UtilityMethods.canTarget(data.getShooter(), loc, target) && target instanceof LivingEntity) {
					hit.add(target.getEntityId());
					((LivingEntity) target).damage(damage, data.getShooter());
				}
		}
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
