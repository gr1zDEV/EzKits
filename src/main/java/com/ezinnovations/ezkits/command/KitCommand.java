package com.ezinnovations.ezkits.command;

import com.ezinnovations.ezkits.gui.GuiService;
import com.ezinnovations.ezkits.kit.KitDefinition;
import com.ezinnovations.ezkits.kit.KitManager;
import com.ezinnovations.ezkits.service.ClaimResult;
import com.ezinnovations.ezkits.service.ClaimService;
import com.ezinnovations.ezkits.service.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final KitManager kitManager;
    private final ClaimService claimService;
    private final GuiService guiService;
    private final MessageService messageService;

    public KitCommand(KitManager kitManager, ClaimService claimService, GuiService guiService, MessageService messageService) {
        this.kitManager = kitManager;
        this.claimService = claimService;
        this.guiService = guiService;
        this.messageService = messageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "general.players-only");
            return true;
        }
        if (!player.hasPermission("ezkits.use")) {
            messageService.send(player, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            guiService.openMain(player);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("preview")) {
            if (!player.hasPermission("ezkits.preview")) {
                messageService.send(player, "general.no-permission");
                return true;
            }
            KitDefinition kit = kitManager.getKit(args[1]);
            if (kit == null) {
                messageService.send(player, "kit.not-found", Map.of("%kit_name%", args[1]));
                return true;
            }
            guiService.openPreview(player, kit);
            return true;
        }

        String kitName = args[0];
        ClaimResult result = claimService.claim(player, kitName, true);
        if (result == ClaimResult.KIT_NOT_FOUND) {
            messageService.send(player, "kit.not-found", Map.of("%kit_name%", kitName));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(kitManager.getKitNames());
            options.add("preview");
            return options.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            return kitManager.getKitNames().stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
