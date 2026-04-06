package com.ezinnovations.ezkits.config;

import com.ezinnovations.ezkits.EzKitsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class ConfigManager {

    private final EzKitsPlugin plugin;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;

    public ConfigManager(EzKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder at " + plugin.getDataFolder().getAbsolutePath());
        }

        plugin.reloadConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("gui.yml");
        if (plugin.getConfig().getBoolean("startup.create-example-kit", true)) {
            saveResourceIfMissing("kits/starter.yml");
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        this.guiConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
    }

    private void saveResourceIfMissing(String path) {
        File outFile = new File(plugin.getDataFolder(), path);
        if (outFile.exists()) {
            return;
        }
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed creating directories for " + outFile.getAbsolutePath());
        }
        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing embedded resource: " + path);
            }
            Files.copy(input, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save default resource " + path, e);
        }
    }

    public File getKitsFolder() {
        File folder = new File(plugin.getDataFolder(), "kits");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Could not create kits folder at " + folder.getAbsolutePath());
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
