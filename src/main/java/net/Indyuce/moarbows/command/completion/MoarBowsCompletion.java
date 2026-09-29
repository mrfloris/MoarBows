package net.Indyuce.moarbows.command.completion;

import net.Indyuce.moarbows.MoarBows;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MoarBowsCompletion implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 0)
            return options;
        boolean admin = sender.hasPermission("moarbows.admin");
        if (args.length == 1) {
            if (sender.hasPermission("moarbows.gui"))
                options.add("menu");
            if (admin)
                options.addAll(List.of("get", "getall", "list", "reload", "equip", "migrate"));
        } else if (admin) {
            boolean give = args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("give");
            boolean migrate = args[0].equalsIgnoreCase("migrate");
            if (args.length == 2 && (give || migrate))
                MoarBows.plugin.getBowManager().getBows().forEach(bow -> options.add(bow.getLowerCaseId()));
            else if (args.length == 3 && give)
                Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
            else if ((args.length == 4 && give) || (args.length == 3 && migrate))
                options.addAll(List.of("1", "2", "3", "4", "5"));
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
