package com.ezinnovations.ezkits.service;

import com.ezinnovations.ezkits.config.ConfigManager;
import com.ezinnovations.ezkits.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class MessageService {

    private final ConfigManager configManager;

    public MessageService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        if (configManager.getMessagesConfig().isList(path)) {
            List<String> lines = configManager.getMessagesConfig().getStringList(path);
            if (lines.isEmpty()) {
                sender.sendMessage(ColorUtil.color("&cMissing message: " + path));
                return;
            }
            for (String line : lines) {
                sender.sendMessage(ColorUtil.color(applyPlaceholders(line, placeholders)));
            }
            return;
        }

        String raw = configManager.getMessagesConfig().getString(path, "&cMissing message: " + path);
        sender.sendMessage(ColorUtil.color(applyPlaceholders(raw, placeholders)));
    }

    public String get(String path, Map<String, String> placeholders) {
        String raw = configManager.getMessagesConfig().getString(path, "&cMissing message: " + path);
        return ColorUtil.color(applyPlaceholders(raw, placeholders));
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
