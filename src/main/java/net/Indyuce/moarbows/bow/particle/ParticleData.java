package net.Indyuce.moarbows.bow.particle;

import net.Indyuce.moarbows.util.lib.Validate;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleData {
	private final Particle particle;
	private final Color color;
	private final int amount;

	public ParticleData(ConfigurationSection config) {
		Validate.notNull(config, "Could not read config");

		Validate.isTrue(config.contains("particle"), "Could not read particle");
		particle = parseParticle(config.getString("particle"));

		color = config.contains("color") ? Color.fromRGB(config.getInt("color.red"), config.getInt("color.green"), config.getInt("color.blue"))
				: null;
		amount = Math.max(0, Math.min(64, config.getInt("amount", 2)));
        validatePayload();
	}

	public ParticleData(Particle particle) {
		this(particle, null, 2);
	}

	public ParticleData(Particle particle, Color color, int amount) {
		this.particle = particle;
		this.color = color;
		this.amount = amount;
        validatePayload();
	}

	public ParticleRunnable newRunnable(Player player, boolean offhand) {
		return new ParticleRunnable(player, offhand);
	}

    public static Particle parseParticle(String name) {
        String id = name.toUpperCase(Locale.ROOT);
        id = switch (id) {
            case "REDSTONE" -> "DUST";
            case "SPELL_INSTANT" -> "INSTANT_EFFECT";
            case "SPELL" -> "EFFECT";
            case "SPELL_MOB", "SPELL_MOB_AMBIENT" -> "ENTITY_EFFECT";
            case "SPELL_WITCH" -> "WITCH";
            case "VILLAGER_HAPPY" -> "HAPPY_VILLAGER";
            case "VILLAGER_ANGRY" -> "ANGRY_VILLAGER";
            case "EXPLOSION_NORMAL" -> "POOF";
            case "EXPLOSION_LARGE" -> "EXPLOSION";
            case "EXPLOSION_HUGE" -> "EXPLOSION_EMITTER";
            case "SMOKE_NORMAL" -> "SMOKE";
            case "SMOKE_LARGE" -> "LARGE_SMOKE";
            case "FIREWORKS_SPARK" -> "FIREWORK";
            case "CRIT_MAGIC" -> "ENCHANTED_HIT";
            case "SNOW_SHOVEL" -> "SNOWFLAKE";
            case "SNOWBALL" -> "ITEM_SNOWBALL";
            case "SLIME" -> "ITEM_SLIME";
            case "BLOCK_CRACK", "BLOCK_DUST" -> "BLOCK";
            case "ITEM_CRACK" -> "ITEM";
            case "TOTEM" -> "TOTEM_OF_UNDYING";
            default -> id;
        };
        return Particle.valueOf(id);
    }

    /** Paper 26.2 requires typed payloads even when no custom color was configured. */
    private void validatePayload() {
        Class<?> type = particle.getDataType();
        if (type != Void.class && type != Particle.DustOptions.class && type != Particle.DustTransition.class
                && type != Particle.Spell.class && type != Color.class && type != BlockData.class && type != ItemStack.class
                && type != Float.class && type != Integer.class)
            throw new IllegalArgumentException("Particle " + particle.name() + " requires a destination payload and cannot be used for a bow trail");
    }

    public Object particlePayload() {
        Color tint = color == null ? Color.WHITE : color;
        if (particle.getDataType() == Particle.DustOptions.class) return new Particle.DustOptions(tint, 1);
        if (particle.getDataType() == Particle.DustTransition.class) return new Particle.DustTransition(tint, tint, 1);
        if (particle.getDataType() == Particle.Spell.class) return new Particle.Spell(tint, 1);
        if (particle.getDataType() == Color.class) return tint;
        if (particle.getDataType() == BlockData.class) return Material.STONE.createBlockData();
        if (particle.getDataType() == ItemStack.class) return new ItemStack(Material.SNOWBALL);
        if (particle.getDataType() == Float.class) return 0F;
        if (particle.getDataType() == Integer.class) return 0;
        return null;
    }

    public void displayParticle(Location loc) {
        display(loc, 0, 0);
    }

    private void display(Location loc, int count, double offset) {
        Object payload = particlePayload();
        loc.getWorld().spawnParticle(particle, loc, count, offset, offset, offset, 0, payload);
    }

	public void setup(ConfigurationSection config) {
		config.set("particle", particle.name());
		config.set("amount", amount);
		if (color != null) {
			config.set("color.red", color.getRed());
			config.set("color.green", color.getGreen());
			config.set("color.blue", color.getBlue());
		}
	}

	public class ParticleRunnable extends BukkitRunnable {
		private final Player player;
		private final boolean offhand;

		public ParticleRunnable(Player player, boolean offhand) {
			this.player = player;
			this.offhand = offhand;
		}

		@Override
		public void run() {
            if (!player.isOnline() || player.isDead()) { cancel(); return; }
			Location loc = player.getLocation().clone().add(0, .8, 0);
			loc.setYaw(player.getLocation().getYaw());
			loc.add(loc.getDirection().multiply(.2));
			loc.setPitch(0);
			loc.setYaw(player.getLocation().getYaw() + (offhand ? -90 : 90));
			loc.add(loc.getDirection().multiply(.3));

            display(loc, amount, .1);
		}
	}
}
