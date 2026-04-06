package com.ezinnovations.ezkits.config;

import com.ezinnovations.ezkits.EzKitsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

public class ConfigManager {

    private final EzKitsPlugin plugin;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;

    public ConfigManager(EzKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.reloadConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("gui.yml");
        saveResourceIfMissing("kits/starter.yml");

        this.messagesConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        this.guiConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
    }

    private void saveResourceIfMissing(String path) {
        File outFile = new File(plugin.getDataFolder(), path);
        if (outFile.exists()) {
            return;
        }
        outFile.getParentFile().mkdirs();
        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing embedded resource: " + path);
            }
            Files.copy(input, outFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save default resource " + path, e);
        }
    }

    public File getKitsFolder() {
        File folder = new File(plugin.getDataFolder(), "kits");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getMessagesConfig() {
        return Objects.requireNonNull(messagesConfig, "messages config not loaded");
    }

    public FileConfiguration getGuiConfig() {
        return Objects.requireNonNull(guiConfig, "gui config not loaded");
    }
}
