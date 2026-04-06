package com.ezinnovations.ezkits.command;

import com.ezinnovations.ezkits.EzKitsPlugin;
import com.ezinnovations.ezkits.config.ConfigManager;
import com.ezinnovations.ezkits.gui.GuiService;
import com.ezinnovations.ezkits.kit.KitDefinition;
import com.ezinnovations.ezkits.kit.KitManager;
import com.ezinnovations.ezkits.service.ClaimResult;
import com.ezinnovations.ezkits.service.ClaimService;
import com.ezinnovations.ezkits.service.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EzKitsAdminCommand implements CommandExecutor, TabCompleter {

    private final EzKitsPlugin plugin;
    private final KitManager kitManager;
    private final ClaimService claimService;
    private final GuiService guiService;
    private final MessageService messageService;
    private final ConfigManager configManager;

    public EzKitsAdminCommand(EzKitsPlugin plugin, KitManager kitManager, ClaimService claimService, GuiService guiService, MessageService messageService, ConfigManager configManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.claimService = claimService;
        this.guiService = guiService;
        this.messageService = messageService;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezkits.admin")) {
            messageService.send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            messageService.send(sender, "admin.usage");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("ezkits.reload")) {
                    messageService.send(sender, "general.no-permission");
                    return true;
                }
                plugin.reloadPlugin();
                messageService.send(sender, "admin.reload-success");
            }
            case "give" -> {
                if (!sender.hasPermission("ezkits.give") || args.length < 3) {
                    messageService.send(sender, "admin.give-usage");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    messageService.send(sender, "admin.player-not-found", Map.of("%player%", args[1]));
                    return true;
                }
                KitDefinition kit = kitManager.getKit(args[2]);
                if (kit == null) {
                    messageService.send(sender, "kit.not-found", Map.of("%kit_name%", args[2]));
                    return true;
                }
                ClaimResult result = claimService.claim(target, kit.id(), true);
                if (result == ClaimResult.SUCCESS) {
                    messageService.send(sender, "admin.give-success", Map.of("%player%", target.getName(), "%kit_name%", kit.id()));
                }
            }
            case "open" -> {
                if (!sender.hasPermission("ezkits.open") || args.length < 2) {
                    messageService.send(sender, "admin.open-usage");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    messageService.send(sender, "admin.player-not-found", Map.of("%player%", args[1]));
                    return true;
                }
                guiService.openMain(target);
                messageService.send(sender, "admin.open-success", Map.of("%player%", target.getName()));
            }
            default -> messageService.send(sender, "admin.usage");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "give", "open").stream()
                    .filter(v -> v.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("open"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return kitManager.getKitNames().stream()
                    .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
