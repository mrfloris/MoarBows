package net.Indyuce.moarbows.bow.effect;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;

public final class ProjectileEffects {
    private ProjectileEffects() {}

    public static Arrow arrow(ArrowMetadata data) {
        Arrow arrow = data.getShooter().launchProjectile(Arrow.class);
        arrow.setShooter(data.getShooter());
        data.configureExtraArrow(arrow);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setPersistent(false);
        return MoarBows.plugin.getArrowManager().trackEffect(arrow, data);
    }
}
