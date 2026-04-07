package com.ezinnovations.ezkits.kit;

import com.ezinnovations.ezkits.EzKitsPlugin;
import com.ezinnovations.ezkits.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class KitManager {

    private final EzKitsPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, KitDefinition> kits = new HashMap<>();
    private final CustomItemResolver customItemResolver;

    public KitManager(EzKitsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.customItemResolver = new CustomItemResolver(plugin);
    }

    public void loadKits() {
        kits.clear();
        File[] files = configManager.getKitsFolder().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            plugin.getLogger().warning("No kit files found.");
            return;
        }

        Set<Integer> usedSlots = new HashSet<>();
        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                String id = yaml.getString("id", file.getName().replace(".yml", "")).trim().toLowerCase(Locale.ROOT);
                if (id.isBlank()) {
                    plugin.getLogger().warning("Skipping kit file " + file.getName() + " because id is blank.");
                    continue;
                }
                if (kits.containsKey(id)) {
                    plugin.getLogger().warning("Duplicate kit id '" + id + "' in " + file.getName() + ". Keeping first definition.");
                    continue;
                }

                int slot = yaml.getInt("slot", 0);
                if (!usedSlots.add(slot)) {
                    plugin.getLogger().warning("Kit '" + id + "' uses duplicate slot " + slot + ". GUI may override another kit.");
                }

                String displayName = yaml.getString("display-name", id);
                String permission = yaml.getString("permission", "ezkits.kit." + id);
                long cooldown = Math.max(0L, yaml.getLong("cooldown-seconds", 0L));
                boolean oneTime = yaml.getBoolean("one-time", false);
                boolean preview = yaml.getBoolean("preview-enabled", true);
                boolean hidden = yaml.getBoolean("hidden", false);
                String category = yaml.getString("category", "default");

                ItemStack icon = resolveIcon(yaml, id);
                List<String> lore = yaml.getStringList("lore");
                List<ItemStack> items = readItems(yaml.getConfigurationSection("items"), id);
                List<String> commands = yaml.getStringList("commands-on-claim");

                Optional<Sound> successSound = parseSound(yaml.getString("sounds.success"), id);
                Optional<Sound> failSound = parseSound(yaml.getString("sounds.fail"), id);

                kits.put(id, new KitDefinition(id, displayName, permission, slot, cooldown, oneTime, preview, hidden, category, icon, lore, items, commands, successSound, failSound));
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load kit file " + file.getName() + ": " + ex.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + kits.size() + " kits.");
    }

    private ItemStack resolveIcon(YamlConfiguration yaml, String kitId) {
        ItemStack icon = yaml.getItemStack("icon");
        if (icon != null && icon.getType() != Material.AIR) {
            return icon;
        }

        String materialName = yaml.getString("icon-material", "CHEST");
        Material material = Material.matchMaterial(materialName == null ? "CHEST" : materialName);
        if (material == null || material == Material.AIR) {
            plugin.getLogger().warning("Invalid icon-material for kit '" + kitId + "': " + materialName + ". Falling back to CHEST.");
            material = Material.CHEST;
        }
        return new ItemStack(material);
    }

    private List<ItemStack> readItems(ConfigurationSection section, String kitId) {
        List<ItemStack> items = new ArrayList<>();
        if (section == null) {
            return items;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection != null && itemSection.contains("provider") && itemSection.contains("id")) {
                String provider = itemSection.getString("provider", "");
                String itemId = itemSection.getString("id", "");
                int amount = Math.max(1, itemSection.getInt("amount", 1));
                var resolved = customItemResolver.resolve(provider, itemId, amount);
                if (resolved.isPresent()) {
                    items.add(resolved.get());
                } else {
                    plugin.getLogger().warning("Skipping custom item key '" + key + "' in kit '" + kitId + "' because provider/id could not be resolved (" + provider + ":" + itemId + ").");
                }
                continue;
            }

            ItemStack item = section.getItemStack(key);
            if (item == null || item.getType() == Material.AIR) {
                plugin.getLogger().warning("Skipping invalid item key '" + key + "' in kit '" + kitId + "'.");
                continue;
            }
            items.add(item);
        }
        return items;
    }

    private Optional<Sound> parseSound(String name, String kitId) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Sound.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sound for kit '" + kitId + "': " + name);
            return Optional.empty();
        }
    }

    public Collection<KitDefinition> getAllKits() {
        return kits.values().stream().sorted(Comparator.comparingInt(KitDefinition::slot).thenComparing(KitDefinition::id)).toList();
    }

    public KitDefinition getKit(String name) {
        return name == null ? null : kits.get(name.toLowerCase(Locale.ROOT));
    }

    public List<String> getKitNames() {
        return new ArrayList<>(kits.keySet());
    }

    public int getKitCount() {
        return kits.size();
    }

    public boolean saveKitItems(String kitId, List<ItemStack> items, Map<Integer, ProviderItemRef> providerRefs, boolean createIfMissing) {
        if (kitId == null || kitId.isBlank()) {
            return false;
        }

        String normalizedId = kitId.toLowerCase(Locale.ROOT);
        File file = new File(configManager.getKitsFolder(), normalizedId + ".yml");
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        if (!file.exists() && !createIfMissing) {
            return false;
        }

        yaml.set("id", normalizedId);
        yaml.set("display-name", yaml.getString("display-name", normalizedId));
        yaml.set("slot", yaml.getInt("slot", Math.max(0, kits.size())));
        yaml.set("permission", yaml.getString("permission", "ezkits.kit." + normalizedId));
        yaml.set("cooldown-seconds", Math.max(0L, yaml.getLong("cooldown-seconds", 0L)));
        yaml.set("one-time", yaml.getBoolean("one-time", false));
        yaml.set("preview-enabled", yaml.getBoolean("preview-enabled", true));
        yaml.set("hidden", yaml.getBoolean("hidden", false));
        yaml.set("category", yaml.getString("category", "default"));
        yaml.set("icon-material", yaml.getString("icon-material", "CHEST"));
        yaml.set("commands-on-claim", yaml.getStringList("commands-on-claim"));

        yaml.set("items", null);
        int index = 1;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack item = items.get(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            String key = "items.item" + index++;
            ProviderItemRef ref = providerRefs.get(slot);
            if (ref != null) {
                yaml.set(key + ".provider", ref.provider());
                yaml.set(key + ".id", ref.id());
                yaml.set(key + ".amount", Math.max(1, ref.amount()));
            } else {
                yaml.set(key, item);
            }
        }

        try {
            yaml.save(file);
            loadKits();
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save kit '" + normalizedId + "': " + exception.getMessage());
            return false;
        }
    }

    public record ProviderItemRef(String provider, String id, int amount) {
    }
}
