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
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

public class Trippple_Bow extends MoarBow {
	public Trippple_Bow() {
		super(new String[] { "Shoots 3 arrows at a time." }, new ParticleData(VParticle.REDSTONE.get(), Color.fromRGB(255, 255, 255), 2),
				new String[] { "AIR,AIR,AIR", "BOW,BOW,BOW", "AIR,AIR,AIR" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(2.5, 0)));
	}

    @Override
    public boolean usesVanillaArrow() {
        return false;
    }

	@Override
	public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
		data.getShooter().getWorld().playSound(data.getShooter().getLocation(), Sounds.ENTITY_ARROW_SHOOT, 2, 1);
		Location loc = data.getShooter().getLocation().add(0, 1.2, 0);
		for (int j = -1; j < 2; j++) {
			if (j > -1 && !UtilityMethods.consumeAmmo(data.getShooter(), new ItemStack(Material.ARROW)))
				return false;

			loc.setYaw(data.getShooter().getLocation().getYaw() + j);
			net.Indyuce.moarbows.bow.effect.ProjectileEffects.arrow(data).setVelocity(loc.getDirection().multiply(event.getForce() * 3.3));
		}
		return true;
	}

	@Override
	public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
	}

	@Override
	public void whenLand(ArrowMetadata data) {
	}
}
