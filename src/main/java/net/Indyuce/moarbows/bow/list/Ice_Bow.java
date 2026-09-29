package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.util.UtilityMethods;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import net.Indyuce.moarbows.version.VPotionEffectType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;

public class Ice_Bow extends MoarBow {
    public Ice_Bow() {
        super(new String[]{"Shoots ice arrows that cause an ice", "explosion upon landing, temporarily", "slowing every nearby entity."},
                new ParticleData(VParticle.ITEM_SNOWBALL.get()), new String[]{"ICE,ICE,ICE", "ICE,BOW,ICE", "ICE,ICE,ICE"});

        addModifier(new DoubleModifier("cooldown", new LinearFormula(0, 0)), new DoubleModifier("amplifier", new LinearFormula(2, .4)),
                new DoubleModifier("duration", new LinearFormula(5, 1)));
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
        int duration = (int) (data.getDouble("duration") * 20);
        int amplifier = (int) data.getDouble("amplifier");

        data.getArrow().remove();
        data.getArrow().getWorld().spawnParticle(VParticle.LARGE_EXPLOSION.get(), data.getImpactLocation(), 0);
        data.getArrow().getWorld().spawnParticle(VParticle.ITEM_SNOWBALL.get(), data.getImpactLocation(), 48, 0, 0, 0, .2);
        data.getArrow().getWorld().spawnParticle(VParticle.FIREWORK.get(), data.getImpactLocation(), 24, 0, 0, 0, .2);
        data.getArrow().getWorld().playSound(data.getImpactLocation(), Sounds.ENTITY_FIREWORK_ROCKET_BLAST, 3, 1);
        for (Entity ent : data.getNearbyEntities(5, 5, 5))
            if (ent instanceof LivingEntity && UtilityMethods.canTarget(data.getShooter(), null, ent)) {
                ((LivingEntity) ent).removePotionEffect(VPotionEffectType.SLOWNESS.get());
                ((LivingEntity) ent).addPotionEffect(new PotionEffect(VPotionEffectType.SLOWNESS.get(), duration, amplifier));
            }
    }
}
