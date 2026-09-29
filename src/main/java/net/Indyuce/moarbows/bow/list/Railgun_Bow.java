package net.Indyuce.moarbows.bow.list;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.modifier.DoubleModifier;
import net.Indyuce.moarbows.bow.particle.ParticleData;
import net.Indyuce.moarbows.util.LinearFormula;
import net.Indyuce.moarbows.version.VParticle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Railgun_Bow extends MoarBow {
    public Railgun_Bow() {
        super(new String[]{"Only works in minecarts. Arrows ", "explode upon landing, causing", "a powerful explosion."},
                new ParticleData(VParticle.ANGRY_VILLAGER.get()), new String[]{"TNT,RAIL,TNT", "RAIL,BOW,RAIL", "TNT,RAIL,TNT"});

        addModifier(new DoubleModifier("cooldown", new LinearFormula(8, -1, 3, 8)), new DoubleModifier("radius", new LinearFormula(5, 1)));
    }

    @Override
    public boolean canShoot(EntityShootBowEvent event, ArrowMetadata data) {
        return data.getShooter().getVehicle() != null && data.getShooter().getVehicle().getType() == EntityType.MINECART;
    }

    @Override
    public void whenHit(EntityDamageByEntityEvent event, ArrowMetadata data, Entity target) {
        whenLand(data);
    }

    @Override
    public void whenLand(ArrowMetadata data) {
        data.getArrow().remove();
        net.Indyuce.moarbows.MoarBows.plugin.getArrowManager().createExplosion(data, (float) data.getDouble("radius"));
    }
}
