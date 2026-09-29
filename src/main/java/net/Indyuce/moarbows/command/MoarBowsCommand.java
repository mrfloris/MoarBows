package net.Indyuce.moarbows.command;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.gui.BowList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.Indyuce.moarbows.util.lib.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Locale;

public class MoarBowsCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length < 1) {
			if (!sender.hasPermission("moarbows.admin")) {
				sender.sendMessage(MoarBows.plugin.getLanguage().formatMessage("not-enough-perms"));
				return true;
			}

			sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------[" + ChatColor.LIGHT_PURPLE + " MoarBows Help "
					+ ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "]-----------------");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "<>" + ChatColor.GRAY + " = required");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "()" + ChatColor.GRAY + " = optional");
			sender.sendMessage("");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/mb " + ChatColor.WHITE + "shows the help page.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/mb get <bow> (player) (level) " + ChatColor.WHITE + "gives a bow to a player.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/mb getall " + ChatColor.WHITE + "gives you all the available bows.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/mb menu " + ChatColor.WHITE + "shows all available bows (GUI).");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/mb list " + ChatColor.WHITE + "shows all available bows.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/mb reload " + ChatColor.WHITE + "reloads the config file.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/mb migrate <bow> (level) " + ChatColor.WHITE + "tags the held legacy bow after admin verification.");
			return true;
		}

		if (args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command is for players only.");
				return true;
			}

			if (!sender.hasPermission("moarbows.gui")) {
				sender.sendMessage(MoarBows.plugin.getLanguage().formatMessage("not-enough-perms"));
				return true;
			}

			new BowList((Player) sender).open();
			return true;
		}

		// perm for op commands
		if (!sender.hasPermission("moarbows.admin")) {
			sender.sendMessage(MoarBows.plugin.getLanguage().formatMessage("not-enough-perms"));
			return true;
		}

		if (args[0].equalsIgnoreCase("reload")) {
			MoarBows.plugin.reloadPlugin();
			sender.sendMessage(ChatColor.YELLOW + "Configuration reload requested; check console for completion.");
		}

		if (args[0].equalsIgnoreCase("list")) {
			sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "------------------------------------------------");
			sender.sendMessage(ChatColor.GREEN + "List of available bows:");
			if (!(sender instanceof Player)) {
				for (MoarBow bow : MoarBows.plugin.getBowManager().getBows())
					sender.sendMessage("* " + ChatColor.GREEN + " " + bow.getName());
				return true;
			}

			for (MoarBow bow : MoarBows.plugin.getBowManager().getBows())
				sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(bow.getName())
						.clickEvent(ClickEvent.runCommand("/mb get " + bow.getId()))
						.hoverEvent(HoverEvent.showText(Component.text("Click to get this bow")))
						.append(Component.text(", use /mb get " + bow.getLowerCaseId(), NamedTextColor.WHITE)));
		}

		if (args[0].equalsIgnoreCase("equip")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command is for players only.");
				return true;
			}

			Player player = (Player) sender;
			if (player.getEquipment().getItemInMainHand() == null || player.getEquipment().getItemInMainHand().getType() == Material.AIR) {
				sender.sendMessage(ChatColor.RED + "Hold something in your hands first.");
				return true;
			}

			Optional<Entity> found = player.getNearbyEntities(10, 10, 10).stream()
					.filter(entity -> entity instanceof LivingEntity living && !(entity instanceof Player)
							&& living.isValid() && !living.isDead() && living.getEquipment() != null).findFirst();
			if (!found.isPresent()) {
				sender.sendMessage(ChatColor.RED + "Couldn't find an entity to equip.");
				return true;
			}

			LivingEntity target = (LivingEntity) found.get();
			ItemStack hand = target.getEquipment().getItemInMainHand().clone();
			target.getEquipment().setItemInMainHand(player.getEquipment().getItemInMainHand().clone());
			player.getEquipment().setItemInMainHand(hand);
		}

		if (args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("give")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Usage: /mb get <bow> (player) (level)");
				return true;
			}

			if (args.length < 3 && !(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "Please specify a player.");
				return true;
			}

			// bow
			String bowFormat = args[1].toUpperCase(Locale.ROOT).replace("-", "_");
			if (!MoarBows.plugin.getBowManager().has(bowFormat)) {
				sender.sendMessage(ChatColor.RED + "Couldn't find the bow called " + bowFormat + ".");
				return true;
			}

			// player
			MoarBow bow = MoarBows.plugin.getBowManager().get(bowFormat);
			Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : ((Player) sender);
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "Couldn't find the player called " + args[2] + ".");
				return true;
			}

			// level
			int level = 0;
			if (args.length > 3)
				try {
					level = Integer.parseInt(args[3]);
					Validate.isTrue(level > 0, "Level must be positive.");
				} catch (IllegalArgumentException exception) {
					sender.sendMessage(ChatColor.RED + args[3] + " is not a valid number.");
					return true;
				}

			// Refuse a full inventory before creating or delivering the item.
			int emptySlot = target.getInventory().firstEmpty();
			if (emptySlot < 0) {
				sender.sendMessage(ChatColor.RED + "The player's inventory is full. No bow was given.");
				return true;
			}
			ItemStack item = bow.getItem(level);
			target.getInventory().setItem(emptySlot, item);
			sender.sendMessage(ChatColor.YELLOW + target.getName() + " was given " + ChatColor.WHITE + bow.getName() + ChatColor.YELLOW + ".");

			// message
			String message = MoarBows.plugin.getLanguage().formatMessage("receive-bow", "bow", bow.getName());
			if (!message.equals("") && !sender.equals(target))
				target.sendMessage(ChatColor.YELLOW + message);

		}
		if (args[0].equalsIgnoreCase("getall")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command is for players only.");
				return true;
			}

			Player player = (Player) sender;
			var bows = MoarBows.plugin.getBowManager().getBows();
			long emptySlots = java.util.Arrays.stream(player.getInventory().getStorageContents())
					.filter(item -> item == null || item.isEmpty()).count();
			if (emptySlots < bows.size()) {
				sender.sendMessage(ChatColor.RED + "Make room for " + bows.size() + " bows. No bows were given.");
				return true;
			}
			var items = bows.stream().map(bow -> bow.getItem(1)).toList();
			for (ItemStack item : items)
				player.getInventory().setItem(player.getInventory().firstEmpty(), item);
		}

		if (args[0].equalsIgnoreCase("migrate")) {
			if (!(sender instanceof Player player)) {
				sender.sendMessage(ChatColor.RED + "Hold the verified legacy bow in-game to migrate it.");
				return true;
			}
			if (args.length < 2 || args.length > 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /mb migrate <bow> (level). Verify the held bow's provenance first.");
				return true;
			}
			try {
				MoarBow bow = MoarBows.plugin.getBowManager().get(args[1].toUpperCase(Locale.ROOT).replace('-', '_'));
				int level = args.length == 3 ? Integer.parseInt(args[2]) : 1;
				ItemStack migrated = MoarBows.plugin.getBowManager().migrate(player.getInventory().getItemInMainHand(), bow, level);
				player.getInventory().setItemInMainHand(migrated);
				sender.sendMessage(ChatColor.YELLOW + "Held bow tagged as " + bow.getId() + " at level " + level + ". Other item data was preserved.");
			} catch (IllegalArgumentException exception) {
				sender.sendMessage(ChatColor.RED + "Migration refused: " + exception.getMessage());
			}
		}

		return true;
	}
}
