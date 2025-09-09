package net.Indyuce.moarbows.listener;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Optional;

public class ArrowLand implements Listener {

	@EventHandler()
	public void a(ProjectileHitEvent event) {
		if (event.getEntity().getType() != EntityType.ARROW || event.getHitEntity() != null)
			return;

		Arrow arrow = (Arrow) event.getEntity();
		Optional<ArrowMetadata> opt = MoarBows.plugin.getArrowManager().getArrowData(arrow);
		if (!opt.isPresent())
			return;

		// land effect
		ArrowMetadata arrowMetadata = opt.get();
		arrowMetadata.getBow().whenLand(arrowMetadata);
		MoarBows.plugin.getArrowManager().unregisterArrow(arrow);
	}
}
