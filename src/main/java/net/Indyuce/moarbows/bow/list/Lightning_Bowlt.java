package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Lightning_Bowlt extends MoarBow {
	public Lightning_Bowlt() {
		super("LIGHTNING_BOWLT", "&fLightning Bow'lt", new String[] { "Shoots arrows that summon", "lightning upon landing." }, 0,
				new ParticleData(VParticle.FIREWORK.get()), new String[] { "AIR,BEACON,AIR", "AIR,BOW,AIR", "AIR,AIR,AIR" });

		addModifier(new DoubleModifier("cooldown", new LinearFormula(10, -1, 3, 10)));
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
		data.getArrow().remove();
		org.bukkit.entity.LightningStrike lightning = data.getArrow().getWorld().strikeLightning(data.getImpactLocation());
        net.Indyuce.moarbows.MoarBows.plugin.getArrowManager().trackEffect(lightning, data);
        if (data.getShooter() instanceof org.bukkit.entity.Player player) lightning.setCausingPlayer(player);
	}
}
