package com.ezinnovations.ezkits.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {

    private final GuiService guiService;

    public GuiListener(GuiService guiService) {
        this.guiService = guiService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        guiService.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesTopInventory = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (touchesTopInventory) {
            event.setCancelled(true);
        }
    }
}
