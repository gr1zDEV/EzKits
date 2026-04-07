package com.ezinnovations.ezkits.gui;

import com.ezinnovations.ezkits.kit.KitDefinition;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {

    private final MenuType menuType;
    private final KitDefinition previewKit;
    private final String adminKitId;
    private Inventory inventory;

    public GuiHolder(MenuType menuType, KitDefinition previewKit) {
        this(menuType, previewKit, null);
    }

    public GuiHolder(MenuType menuType, KitDefinition previewKit, String adminKitId) {
        this.menuType = menuType;
        this.previewKit = previewKit;
        this.adminKitId = adminKitId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public KitDefinition getPreviewKit() {
        return previewKit;
    }

    public String getAdminKitId() {
        return adminKitId;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
