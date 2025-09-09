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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Meteor_Bow extends MoarBow {
    public Meteor_Bow() {
        super(new String[]{"Shoots arrows that summon a fire", "comet upon landing, dealing &c{damage} &7damage", "and knockback to entities within &c{radius} &7blocks."},
                new ParticleData(VParticle.LAVA.get()),
                new String[]{"FIRE_CHARGE,FIRE_CHARGE,FIRE_CHARGE", "FIRE_CHARGE,BOW,FIRE_CHARGE", "FIRE_CHARGE,FIRE_CHARGE,FIRE_CHARGE"});

        addModifier(new DoubleModifier("cooldown", new LinearFormula(10, -1, 3, 10)), new DoubleModifier("damage", new LinearFormula(8, 4)),
                new DoubleModifier("knockback", new LinearFormula(1, 1.3)), new DoubleModifier("radius", new LinearFormula(3, 1)));
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
        double damage = data.getDouble("damage");
        double knockback = data.getDouble("knockback");
        double radius = data.getDouble("radius");
        double radiusSquared = radius * radius;

        data.getArrow().getWorld().playSound(data.getArrow().getLocation(), Sounds.ENTITY_ENDERMAN_TELEPORT, 2, 1);
        new BukkitRunnable() {
            final Location loc = data.getArrow().getLocation();
            final Location source = loc.clone().add(5 * Math.cos(random.nextDouble() * 2 * Math.PI), 20,
                    5 * Math.sin(random.nextDouble() * 2 * Math.PI));
            final Vector vec = loc.subtract(source).toVector().multiply((double) 1 / 30);

            int ti = 0;

            public void run() {
                if (ti == 0)
                    loc.setDirection(vec);

                for (int k = 0; k < 2; k++) {
                    ti++;
                    source.add(vec);
                    for (double i = 0; i < Math.PI * 2; i += Math.PI / 6) {
                        Vector vec = UtilityMethods.rotateFunc(new Vector(Math.cos(i), Math.sin(i), 0), loc);
                        source.getWorld().spawnParticle(VParticle.LARGE_SMOKE.get(), source, 0, vec.getX(), vec.getY(), vec.getZ(), .1);
                    }
                }

                if (ti >= 30) {
                    source.getWorld().playSound(source, Sounds.ENTITY_GENERIC_EXPLODE, 3, 1);
                    source.getWorld().spawnParticle(VParticle.FLAME.get(), source, 64, 0, 0, 0, .25);
                    source.getWorld().spawnParticle(VParticle.LAVA.get(), source, 32);
                    for (double j = 0; j < Math.PI * 2; j += Math.PI / 24)
                        source.getWorld().spawnParticle(VParticle.LARGE_SMOKE.get(), source, 0, Math.cos(j), 0, Math.sin(j), .5);

                    for (LivingEntity entity : data.getArrow().getWorld().getEntitiesByClass(LivingEntity.class))
                        if (entity.getLocation().distanceSquared(source) < radiusSquared) {
                            entity.damage(damage, data.getShooter());
                            entity.setVelocity(entity.getLocation().toVector().subtract(source.toVector()).setY(.75).normalize().multiply(knockback));
                        }
                    cancel();
                }
            }
        }.runTaskTimer(MoarBows.plugin, 0, 1);
    }
}
