package com.ezinnovations.ezkits.command;

import com.ezinnovations.ezkits.EzKitsPlugin;
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

    public EzKitsAdminCommand(EzKitsPlugin plugin, KitManager kitManager, ClaimService claimService, GuiService guiService, MessageService messageService) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.claimService = claimService;
        this.guiService = guiService;
        this.messageService = messageService;
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
                } else {
                    messageService.send(sender, "admin.give-failed", Map.of("%player%", target.getName(), "%kit_name%", kit.id(), "%reason%", result.name()));
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
            case "edit" -> {
                if (!(sender instanceof Player player)) {
                    messageService.send(sender, "general.players-only");
                    return true;
                }
                if (!sender.hasPermission("ezkits.edit") || args.length < 2) {
                    messageService.send(sender, "admin.edit-usage");
                    return true;
                }
                if (guiService.openAdminEditor(player, args[1], false)) {
                    messageService.send(sender, "admin.edit-opened", Map.of("%kit_name%", args[1]));
                } else {
                    messageService.send(sender, "kit.not-found", Map.of("%kit_name%", args[1]));
                }
            }
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    messageService.send(sender, "general.players-only");
                    return true;
                }
                if (!sender.hasPermission("ezkits.create") || args.length < 2) {
                    messageService.send(sender, "admin.create-usage");
                    return true;
                }
                guiService.openAdminEditor(player, args[1], true);
                messageService.send(sender, "admin.create-opened", Map.of("%kit_name%", args[1]));
            }
            case "save" -> {
                if (!(sender instanceof Player player)) {
                    messageService.send(sender, "general.players-only");
                    return true;
                }
                if (!sender.hasPermission("ezkits.edit")) {
                    messageService.send(sender, "general.no-permission");
                    return true;
                }
                if (guiService.saveAdminSession(player)) {
                    messageService.send(sender, "admin.save-success");
                } else {
                    messageService.send(sender, "admin.no-editor-session");
                }
            }
            case "discard" -> {
                if (!(sender instanceof Player player)) {
                    messageService.send(sender, "general.players-only");
                    return true;
                }
                if (!sender.hasPermission("ezkits.edit")) {
                    messageService.send(sender, "general.no-permission");
                    return true;
                }
                guiService.discardAdminSession(player);
                messageService.send(sender, "admin.discarded");
            }
            case "custom" -> {
                if (!(sender instanceof Player player)) {
                    messageService.send(sender, "general.players-only");
                    return true;
                }
                if (!sender.hasPermission("ezkits.edit") || args.length < 4) {
                    messageService.send(sender, "admin.custom-usage");
                    return true;
                }
                int slot;
                int amount = 1;
                try {
                    slot = Integer.parseInt(args[1]);
                    if (args.length >= 5) {
                        amount = Integer.parseInt(args[4]);
                    }
                } catch (NumberFormatException exception) {
                    messageService.send(sender, "admin.custom-usage");
                    return true;
                }
                if (guiService.setCustomRef(player, slot, args[2], args[3], amount)) {
                    messageService.send(sender, "admin.custom-success", Map.of("%slot%", String.valueOf(slot), "%provider%", args[2], "%id%", args[3]));
                } else {
                    messageService.send(sender, "admin.no-editor-session");
                }
            }
            default -> messageService.send(sender, "admin.usage");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "give", "open", "edit", "create", "save", "discard", "custom").stream()
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
        if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("create"))) {
            return kitManager.getKitNames().stream()
                    .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("custom")) {
            return List.of("nexo", "executableitems").stream()
                    .filter(v -> v.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
