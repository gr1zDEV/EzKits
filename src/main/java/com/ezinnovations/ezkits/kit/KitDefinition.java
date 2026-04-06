package com.ezinnovations.ezkits.kit;

import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record KitDefinition(
        String id,
        String displayName,
        String permission,
        int slot,
        long cooldownSeconds,
        boolean oneTime,
        boolean previewEnabled,
        boolean hidden,
        String category,
        ItemStack icon,
        List<String> lore,
        List<ItemStack> items,
        List<String> commands,
        Optional<Sound> successSound,
        Optional<Sound> failSound
) {
    public List<ItemStack> safeItems() {
        return Collections.unmodifiableList(items);
    }
}
