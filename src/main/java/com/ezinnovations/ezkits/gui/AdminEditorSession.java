package com.ezinnovations.ezkits.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AdminEditorSession {

    private final String kitId;
    private final boolean creating;
    private final Inventory inventory;
    private final Map<Integer, CustomItemRef> customRefs = new HashMap<>();

    public AdminEditorSession(String kitId, boolean creating, Inventory inventory) {
        this.kitId = kitId;
        this.creating = creating;
        this.inventory = inventory;
    }

    public String getKitId() {
        return kitId;
    }

    public boolean isCreating() {
        return creating;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, CustomItemRef> getCustomRefs() {
        return customRefs;
    }

    public boolean isControlSlot(int slot) {
        return slot >= 45 && slot <= 53;
    }

    public boolean isEditableSlot(int slot) {
        return slot >= 0 && slot <= 44;
    }

    public void setCustomRef(int slot, CustomItemRef customItemRef) {
        if (customItemRef == null) {
            customRefs.remove(slot);
            return;
        }
        customRefs.put(slot, customItemRef);
        ItemStack current = inventory.getItem(slot);
        if (current != null) {
            current.setAmount(Math.max(1, customItemRef.amount()));
            inventory.setItem(slot, current);
        }
    }

    public record CustomItemRef(String provider, String id, int amount) {
    }
}
