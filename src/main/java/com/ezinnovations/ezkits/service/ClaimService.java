package com.ezinnovations.ezkits.service;

import com.ezinnovations.ezkits.EzKitsPlugin;
import com.ezinnovations.ezkits.config.ConfigManager;
import com.ezinnovations.ezkits.kit.KitDefinition;
import com.ezinnovations.ezkits.kit.KitManager;
import com.ezinnovations.ezkits.kit.KitState;
import com.ezinnovations.ezkits.storage.PlayerKitStorage;
import com.ezinnovations.ezkits.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimService {

    private final EzKitsPlugin plugin;
    private final KitManager kitManager;
    private final PlayerKitStorage storage;
    private final MessageService messageService;
    private final ConfigManager configManager;

    public ClaimService(EzKitsPlugin plugin, KitManager kitManager, PlayerKitStorage storage, MessageService messageService, ConfigManager configManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.storage = storage;
        this.messageService = messageService;
        this.configManager = configManager;
    }

    public ClaimResult claim(Player player, String kitName, boolean notify) {
        KitDefinition kit = kitManager.getKit(kitName);
        if (kit == null) {
            return ClaimResult.KIT_NOT_FOUND;
        }

        KitState state = resolveState(player, kit);
        if (state != KitState.AVAILABLE) {
            if (notify) {
                sendFailureFeedback(player, kit, state);
            }
            return switch (state) {
                case LOCKED -> ClaimResult.LOCKED;
                case COOLDOWN -> ClaimResult.COOLDOWN;
                case CLAIMED -> ClaimResult.ONE_TIME_ALREADY_CLAIMED;
                default -> ClaimResult.KIT_NOT_FOUND;
            };
        }

        List<ItemStack> safeItems = kit.safeItems().stream().map(ItemStack::clone).toList();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(safeItems.toArray(new ItemStack[0]));

        if (!leftovers.isEmpty() && configManager.getMainConfig().getBoolean("general.drop-leftovers", true)) {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }

        for (String command : kit.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    command.replace("%player%", player.getName()).replace("%kit%", kit.id()));
        }

        long expiry = kit.cooldownSeconds() <= 0 ? 0 : (System.currentTimeMillis() / 1000L) + kit.cooldownSeconds();
        storage.markClaimed(player.getUniqueId(), kit.id(), expiry, kit.oneTime());

        if (notify) {
            Map<String, String> placeholders = basePlaceholders(kit);
            messageService.send(player, "kit.claim-success", placeholders);
            playConfiguredSound(player, kit.successSound().orElse(Sound.ENTITY_PLAYER_LEVELUP));
            if (configManager.getMainConfig().getBoolean("feedback.actionbar-enabled", true)) {
                player.sendActionBar(messageService.get("kit.claim-success-actionbar", placeholders));
            }
        }

        return ClaimResult.SUCCESS;
    }

    public KitState resolveState(Player player, KitDefinition kit) {
        if (!player.hasPermission(kit.permission())) {
            return KitState.LOCKED;
        }
        if (kit.oneTime() && storage.isOneTimeClaimed(player.getUniqueId(), kit.id())) {
            return KitState.CLAIMED;
        }
        long expiry = storage.getCooldownExpiry(player.getUniqueId(), kit.id());
        long now = System.currentTimeMillis() / 1000L;
        if (expiry > now) {
            return KitState.COOLDOWN;
        }
        return KitState.AVAILABLE;
    }

    public String getCooldownText(Player player, KitDefinition kit) {
        long expiry = storage.getCooldownExpiry(player.getUniqueId(), kit.id());
        long remaining = Math.max(0, expiry - (System.currentTimeMillis() / 1000L));
        return TimeUtil.formatDuration(remaining);
    }

    private void sendFailureFeedback(Player player, KitDefinition kit, KitState state) {
        Map<String, String> placeholders = basePlaceholders(kit);
        placeholders.put("%cooldown%", getCooldownText(player, kit));

        switch (state) {
            case LOCKED -> messageService.send(player, "kit.locked", placeholders);
            case COOLDOWN -> messageService.send(player, "kit.cooldown", placeholders);
            case CLAIMED -> messageService.send(player, "kit.one-time-claimed", placeholders);
            default -> {
            }
        }
        playConfiguredSound(player, kit.failSound().orElse(Sound.BLOCK_NOTE_BLOCK_BASS));
    }

    private Map<String, String> basePlaceholders(KitDefinition kit) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%kit_name%", kit.id());
        placeholders.put("%kit_displayname%", kit.displayName());
        return placeholders;
    }

    private void playConfiguredSound(Player player, Sound sound) {
        if (configManager.getMainConfig().getBoolean("feedback.sounds-enabled", true)) {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
    }
}
