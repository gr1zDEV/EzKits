package com.ezinnovations.ezkits.gui;

import com.ezinnovations.ezkits.kit.KitDefinition;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {

    private final MenuType menuType;
    private final KitDefinition previewKit;

    public GuiHolder(MenuType menuType, KitDefinition previewKit) {
        this.menuType = menuType;
        this.previewKit = previewKit;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public KitDefinition getPreviewKit() {
        return previewKit;
    }
}
