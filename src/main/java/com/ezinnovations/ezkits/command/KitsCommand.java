package com.ezinnovations.ezkits.command;

import com.ezinnovations.ezkits.gui.GuiService;
import com.ezinnovations.ezkits.service.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitsCommand implements CommandExecutor {

    private final GuiService guiService;
    private final MessageService messageService;

    public KitsCommand(GuiService guiService, MessageService messageService) {
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
        guiService.openMain(player);
        return true;
    }
}
