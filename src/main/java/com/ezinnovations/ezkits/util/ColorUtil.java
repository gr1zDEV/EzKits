package com.ezinnovations.ezkits.util;

import org.bukkit.ChatColor;

import java.util.List;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<String> color(List<String> input) {
        return input.stream().map(ColorUtil::color).toList();
    }
}
