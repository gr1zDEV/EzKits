package com.ezinnovations.ezkits.util;

import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.List;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<String> color(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return input.stream().map(ColorUtil::color).toList();
    }
}
