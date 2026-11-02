package com.narsmp.legend;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LegendCommand implements CommandExecutor, TabCompleter {

    private final LegendWeapons plugin;

    public LegendCommand(LegendWeapons plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("legend.admin")) {
            sender.sendMessage(Component.text("Kamu tidak punya izin.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Pakai: /legend give <senjata> [pemain] | /legend list", NamedTextColor.YELLOW));
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            StringBuilder sb = new StringBuilder();
            for (Weapon w : Weapon.values()) sb.append(w.id()).append(", ");
            sender.sendMessage(Component.text("Senjata: " + sb.substring(0, sb.length() - 2), NamedTextColor.GOLD));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(Component.text("Config LegendWeapons di-reload.", NamedTextColor.GREEN));
            return true;
        }
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("Pakai: /legend give <senjata> [pemain]", NamedTextColor.YELLOW));
                return true;
            }
            Weapon weapon = Weapon.fromId(args[1]);
            if (weapon == null) {
                sender.sendMessage(Component.text("Senjata tidak ditemukan. Ketik /legend list", NamedTextColor.RED));
                return true;
            }
            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(Component.text("Sebutkan nama pemain.", NamedTextColor.RED));
                return true;
            }
            if (target == null) {
                sender.sendMessage(Component.text("Pemain tidak online.", NamedTextColor.RED));
                return true;
            }
            target.getInventory().addItem(weapon.create(plugin)).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
            sender.sendMessage(Component.text("Memberi " + weapon.id() + " ke " + target.getName(), NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("Pakai: /legend give <senjata> [pemain] | /legend list", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("legend.admin")) return out;
        if (args.length == 1) {
            out.add("give");
            out.add("list");
            out.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Weapon w : Weapon.values()) out.add(w.id());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        }
        String prefix = args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        return out;
    }
}
