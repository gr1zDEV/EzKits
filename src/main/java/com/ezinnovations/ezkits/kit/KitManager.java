package com.ezinnovations.ezkits.kit;

import com.ezinnovations.ezkits.EzKitsPlugin;
import com.ezinnovations.ezkits.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class KitManager {

    private final EzKitsPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, KitDefinition> kits = new HashMap<>();

    public KitManager(EzKitsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void loadKits() {
        kits.clear();
        File[] files = configManager.getKitsFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String id = yaml.getString("id", file.getName().replace(".yml", "")).toLowerCase(Locale.ROOT);
            String displayName = yaml.getString("display-name", id);
            String permission = yaml.getString("permission", "ezkits.kit." + id);
            int slot = yaml.getInt("slot", 0);
            long cooldown = yaml.getLong("cooldown-seconds", 0L);
            boolean oneTime = yaml.getBoolean("one-time", false);
            boolean preview = yaml.getBoolean("preview-enabled", true);
            boolean hidden = yaml.getBoolean("hidden", false);
            String category = yaml.getString("category", "default");

            ItemStack icon = yaml.getItemStack("icon");
            if (icon == null) {
                String materialName = yaml.getString("icon-material", "CHEST");
                icon = new ItemStack(Material.matchMaterial(materialName) == null ? Material.CHEST : Material.matchMaterial(materialName));
            }

            List<String> lore = yaml.getStringList("lore");
            List<ItemStack> items = readItems(yaml.getConfigurationSection("items"));
            List<String> commands = yaml.getStringList("commands-on-claim");

            Optional<Sound> successSound = parseSound(yaml.getString("sounds.success"));
            Optional<Sound> failSound = parseSound(yaml.getString("sounds.fail"));

            kits.put(id, new KitDefinition(id, displayName, permission, slot, cooldown, oneTime, preview, hidden, category, icon, lore, items, commands, successSound, failSound));
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kits.");
    }

    private List<ItemStack> readItems(ConfigurationSection section) {
        List<ItemStack> items = new ArrayList<>();
        if (section == null) {
            return items;
        }
        for (String key : section.getKeys(false)) {
            ItemStack item = section.getItemStack(key);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private Optional<Sound> parseSound(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Sound.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sound configured: " + name);
            return Optional.empty();
        }
    }

    public Collection<KitDefinition> getAllKits() {
        return kits.values().stream().sorted(Comparator.comparingInt(KitDefinition::slot)).toList();
    }

    public KitDefinition getKit(String name) {
        return kits.get(name.toLowerCase(Locale.ROOT));
    }

    public List<String> getKitNames() {
        return new ArrayList<>(kits.keySet());
    }
}
