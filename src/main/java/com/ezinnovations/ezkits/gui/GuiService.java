package com.ezinnovations.ezkits.gui;

import com.ezinnovations.ezkits.EzKitsPlugin;
import com.ezinnovations.ezkits.config.ConfigManager;
import com.ezinnovations.ezkits.kit.KitDefinition;
import com.ezinnovations.ezkits.kit.KitManager;
import com.ezinnovations.ezkits.kit.KitState;
import com.ezinnovations.ezkits.service.ClaimService;
import com.ezinnovations.ezkits.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GuiService {

    private final EzKitsPlugin plugin;
    private final KitManager kitManager;
    private final ClaimService claimService;
    private final ConfigManager configManager;
    private final Map<UUID, AdminEditorSession> adminSessions = new HashMap<>();

    public GuiService(EzKitsPlugin plugin, KitManager kitManager, ClaimService claimService, ConfigManager configManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.claimService = claimService;
        this.configManager = configManager;
    }

    public void openMain(Player player) {
        String title = ColorUtil.color(configManager.getGuiConfig().getString("main.title", "&8Kits"));
        int size = sanitizeSize(configManager.getGuiConfig().getInt("main.size", 54), "main.size");
        GuiHolder holder = new GuiHolder(MenuType.MAIN, null);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        fillIfEnabled(inventory, "filler");

        for (KitDefinition kit : kitManager.getAllKits()) {
            if (kit.hidden() && configManager.getGuiConfig().getBoolean("main.hide-hidden-kits", true)) {
                continue;
            }
            if (kit.slot() < 0 || kit.slot() >= inventory.getSize()) {
                plugin.getLogger().warning("Kit " + kit.id() + " has invalid slot " + kit.slot());
                continue;
            }
            inventory.setItem(kit.slot(), buildKitButton(player, kit));
        }

        player.openInventory(inventory);
    }

    public void openPreview(Player player, KitDefinition kit) {
        String title = ColorUtil.color(configManager.getGuiConfig().getString("preview.title", "&8Preview: %kit_displayname%")
                .replace("%kit_displayname%", kit.displayName()));
        int size = sanitizeSize(configManager.getGuiConfig().getInt("preview.size", 54), "preview.size");
        GuiHolder holder = new GuiHolder(MenuType.PREVIEW, kit);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        Set<Integer> controlSlots = getPreviewControlSlots(size);
        fillIfEnabled(inventory, "filler");

        List<ItemStack> items = kit.safeItems();
        int itemIndex = 0;
        for (int slot = 0; slot < size && itemIndex < items.size(); slot++) {
            if (controlSlots.contains(slot)) {
                continue;
            }
            inventory.setItem(slot, items.get(itemIndex++).clone());
        }

        placeControlButton(inventory, "preview.controls.claim", kit.id());
        placeControlButton(inventory, "preview.controls.back", kit.id());
        placeControlButton(inventory, "preview.controls.close", kit.id());

        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder holder)) {
            return;
        }

        if (holder.getMenuType() == MenuType.ADMIN_EDITOR) {
            handleAdminEditorClick(event, player, holder);
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (holder.getMenuType() == MenuType.MAIN) {
            handleMainClick(player, rawSlot, event.getCurrentItem(), event.getClick());
            return;
        }
        handlePreviewClick(player, holder, rawSlot);
    }

    public void handleDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder holder)) {
            return;
        }
        if (holder.getMenuType() != MenuType.ADMIN_EDITOR) {
            int topSize = event.getView().getTopInventory().getSize();
            boolean touchesTopInventory = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
            if (touchesTopInventory) {
                event.setCancelled(true);
            }
            return;
        }

        AdminEditorSession session = adminSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }

        boolean touchesControl = event.getRawSlots().stream()
                .filter(slot -> slot < event.getView().getTopInventory().getSize())
                .anyMatch(session::isControlSlot);
        if (touchesControl) {
            event.setCancelled(true);
        }
    }

    public boolean openAdminEditor(Player player, String kitId, boolean createIfMissing) {
        String normalized = kitId.toLowerCase(Locale.ROOT);
        KitDefinition kit = kitManager.getKit(normalized);
        if (kit == null && !createIfMissing) {
            return false;
        }

        GuiHolder holder = new GuiHolder(MenuType.ADMIN_EDITOR, null, normalized);
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.color("&8Edit Kit: &f" + normalized));
        holder.setInventory(inventory);

        if (kit != null) {
            List<ItemStack> items = kit.safeItems();
            for (int slot = 0; slot < 45 && slot < items.size(); slot++) {
                inventory.setItem(slot, items.get(slot).clone());
            }
        }

        setEditorControl(inventory, 45, Material.LIME_CONCRETE, "&aSave kit");
        setEditorControl(inventory, 46, Material.BOOK, "&eMark custom (use /ezkits custom <slot> <provider> <id> [amount])");
        setEditorControl(inventory, 49, Material.BARRIER, "&cClose without saving");
        setEditorControl(inventory, 53, Material.REDSTONE_BLOCK, "&cClear items");

        AdminEditorSession session = new AdminEditorSession(normalized, kit == null, inventory);
        adminSessions.put(player.getUniqueId(), session);
        player.openInventory(inventory);
        return true;
    }

    public boolean saveAdminSession(Player player) {
        AdminEditorSession session = adminSessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot <= 44; slot++) {
            ItemStack item = session.getInventory().getItem(slot);
            items.add(item == null ? null : item.clone());
        }
        Map<Integer, KitManager.ProviderItemRef> refs = new HashMap<>();
        session.getCustomRefs().forEach((slot, ref) -> refs.put(slot, new KitManager.ProviderItemRef(ref.provider(), ref.id(), ref.amount())));
        boolean saved = kitManager.saveKitItems(session.getKitId(), items, refs, true);
        if (saved) {
            adminSessions.remove(player.getUniqueId());
        }
        return saved;
    }

    public void discardAdminSession(Player player) {
        adminSessions.remove(player.getUniqueId());
    }

    public boolean setCustomRef(Player player, int slot, String provider, String id, int amount) {
        AdminEditorSession session = adminSessions.get(player.getUniqueId());
        if (session == null || !session.isEditableSlot(slot)) {
            return false;
        }
        session.setCustomRef(slot, new AdminEditorSession.CustomItemRef(provider, id, Math.max(1, amount)));
        return true;
    }

    private void handleAdminEditorClick(InventoryClickEvent event, Player player, GuiHolder holder) {
        AdminEditorSession session = adminSessions.get(player.getUniqueId());
        if (session == null || !session.getKitId().equalsIgnoreCase(holder.getAdminKitId())) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize()) {
            if (session.isControlSlot(rawSlot)) {
                event.setCancelled(true);
                if (rawSlot == 45) {
                    if (saveAdminSession(player)) {
                        player.sendMessage(ColorUtil.color("&aKit saved and reloaded."));
                        player.closeInventory();
                    } else {
                        player.sendMessage(ColorUtil.color("&cCould not save this kit."));
                    }
                } else if (rawSlot == 49) {
                    discardAdminSession(player);
                    player.closeInventory();
                } else if (rawSlot == 53) {
                    for (int slot = 0; slot <= 44; slot++) {
                        session.getInventory().setItem(slot, null);
                    }
                    session.getCustomRefs().clear();
                }
                return;
            }
            event.setCancelled(false);
            return;
        }
        event.setCancelled(false);
    }

    private void setEditorControl(Inventory inventory, int slot, Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void handleMainClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        for (KitDefinition kit : kitManager.getAllKits()) {
            if (kit.slot() != slot) {
                continue;
            }
            if (clickType == ClickType.RIGHT && kit.previewEnabled() && player.hasPermission("ezkits.preview")) {
                openPreview(player, kit);
                return;
            }
            claimService.claim(player, kit.id(), true);
            openMain(player);
            return;
        }
    }

    private void handlePreviewClick(Player player, GuiHolder holder, int slot) {
        KitDefinition kit = holder.getPreviewKit();
        if (kit == null) {
            return;
        }

        int claimSlot = configManager.getGuiConfig().getInt("preview.controls.claim.slot", 48);
        int backSlot = configManager.getGuiConfig().getInt("preview.controls.back.slot", 49);
        int closeSlot = configManager.getGuiConfig().getInt("preview.controls.close.slot", 50);

        if (slot == claimSlot) {
            claimService.claim(player, kit.id(), true);
            openPreview(player, kit);
        } else if (slot == backSlot) {
            openMain(player);
        } else if (slot == closeSlot) {
            player.closeInventory();
        }
    }

    private ItemStack buildKitButton(Player player, KitDefinition kit) {
        ItemStack item = kit.icon().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        KitState state = claimService.resolveState(player, kit);
        ConfigurationSection stateSection = configManager.getGuiConfig().getConfigurationSection("states." + state.name().toLowerCase());

        String statusName = switch (state) {
            case AVAILABLE -> configManager.getGuiConfig().getString("states.available.status-name", "Available");
            case LOCKED -> configManager.getGuiConfig().getString("states.locked.status-name", "Locked");
            case COOLDOWN -> configManager.getGuiConfig().getString("states.cooldown.status-name", "Cooldown");
            case CLAIMED -> configManager.getGuiConfig().getString("states.claimed.status-name", "Claimed");
        };

        meta.setDisplayName(ColorUtil.color(kit.displayName()));
        List<String> dynamicLore = new ArrayList<>();
        List<String> baseLore = kit.lore().isEmpty() ? configManager.getGuiConfig().getStringList("states.default-lore") : kit.lore();
        for (String line : baseLore) {
            dynamicLore.add(applyKitPlaceholders(player, kit, statusName, line));
        }

        if (stateSection != null) {
            for (String line : stateSection.getStringList("lore")) {
                dynamicLore.add(applyKitPlaceholders(player, kit, statusName, line));
            }
            if (stateSection.getBoolean("glow", false)) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }

        meta.setLore(ColorUtil.color(dynamicLore));
        item.setItemMeta(meta);
        return item;
    }

    private String applyKitPlaceholders(Player player, KitDefinition kit, String statusName, String text) {
        return ColorUtil.color(text
                .replace("%kit_name%", kit.id())
                .replace("%kit_displayname%", kit.displayName())
                .replace("%cooldown%", claimService.getCooldownText(player, kit))
                .replace("%status%", statusName));
    }

    private void fillIfEnabled(Inventory inventory, String path) {
        if (!configManager.getGuiConfig().getBoolean(path + ".enabled", true)) {
            return;
        }
        String materialName = configManager.getGuiConfig().getString(path + ".material", "GRAY_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Invalid filler material '" + materialName + "'. Falling back to GRAY_STAINED_GLASS_PANE.");
            material = Material.GRAY_STAINED_GLASS_PANE;
        }
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(configManager.getGuiConfig().getString(path + ".name", " ")));
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private void placeControlButton(Inventory inventory, String path, String kitId) {
        int slot = configManager.getGuiConfig().getInt(path + ".slot", -1);
        if (slot < 0 || slot >= inventory.getSize()) {
            plugin.getLogger().warning("Invalid GUI control slot at " + path + ": " + slot);
            return;
        }
        Material material = Material.matchMaterial(configManager.getGuiConfig().getString(path + ".material", "BARRIER"));
        if (material == null) {
            plugin.getLogger().warning("Invalid GUI control material at " + path + ".material. Falling back to BARRIER.");
            material = Material.BARRIER;
        }
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            String name = configManager.getGuiConfig().getString(path + ".name", "Button");
            meta.setDisplayName(ColorUtil.color(name.replace("%kit_name%", kitId)));
            meta.setLore(ColorUtil.color(configManager.getGuiConfig().getStringList(path + ".lore")));
            button.setItemMeta(meta);
        }
        inventory.setItem(slot, button);
    }

    private int sanitizeSize(int size, String path) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            plugin.getLogger().warning("Invalid GUI size at " + path + " (" + size + "). Using 54.");
            return 54;
        }
        return size;
    }

    private Set<Integer> getPreviewControlSlots(int previewSize) {
        Set<Integer> slots = new HashSet<>();
        int claimSlot = configManager.getGuiConfig().getInt("preview.controls.claim.slot", 48);
        int backSlot = configManager.getGuiConfig().getInt("preview.controls.back.slot", 49);
        int closeSlot = configManager.getGuiConfig().getInt("preview.controls.close.slot", 50);
        if (claimSlot >= 0 && claimSlot < previewSize) {
            slots.add(claimSlot);
        }
        if (backSlot >= 0 && backSlot < previewSize) {
            slots.add(backSlot);
        }
        if (closeSlot >= 0 && closeSlot < previewSize) {
            slots.add(closeSlot);
        }
        return slots;
    }

}
