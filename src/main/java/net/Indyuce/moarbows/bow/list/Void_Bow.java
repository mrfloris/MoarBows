package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.Sounds;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Void_Bow extends MoarBow {
    public Void_Bow() {
        super(new String[]{"Its arrows teleport you", "to where they land."}, new ParticleData(VParticle.REDSTONE.get(), Color.fromRGB(128, 0, 128), 2),
                new String[]{"AIR,ENDER_PEARL,AIR", "ENDER_PEARL,BOW,ENDER_PEARL", "AIR,ENDER_PEARL,AIR"});

        addModifier(new DoubleModifier("cooldown", new LinearFormula(5, -1, 2, 5)));
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
        Location loc = data.getImpactLocation();
        loc.setPitch(data.getShooter().getLocation().getPitch());
        loc.setYaw(data.getShooter().getLocation().getYaw());
        data.getShooter().teleport(loc);
        loc.getWorld().spawnParticle(VParticle.LARGE_EXPLOSION.get(), loc, 0);
        data.getArrow().getWorld().playSound(data.getImpactLocation(), Sounds.ENTITY_ENDERMAN_TELEPORT, 3, 1);
    }
}
