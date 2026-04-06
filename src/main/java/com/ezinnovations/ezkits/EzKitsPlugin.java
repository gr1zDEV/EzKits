package com.ezinnovations.ezkits;

import com.ezinnovations.ezkits.command.EzKitsAdminCommand;
import com.ezinnovations.ezkits.command.KitCommand;
import com.ezinnovations.ezkits.command.KitsCommand;
import com.ezinnovations.ezkits.config.ConfigManager;
import com.ezinnovations.ezkits.gui.GuiListener;
import com.ezinnovations.ezkits.gui.GuiService;
import com.ezinnovations.ezkits.kit.KitManager;
import com.ezinnovations.ezkits.service.ClaimService;
import com.ezinnovations.ezkits.service.MessageService;
import com.ezinnovations.ezkits.storage.PlayerKitStorage;
import com.ezinnovations.ezkits.storage.SQLitePlayerKitStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class EzKitsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private KitManager kitManager;
    private PlayerKitStorage storage;
    private MessageService messageService;
    private ClaimService claimService;
    private GuiService guiService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            this.configManager = new ConfigManager(this);
            this.configManager.loadAll();

            this.kitManager = new KitManager(this, configManager);
            this.kitManager.loadKits();

            this.storage = new SQLitePlayerKitStorage(this);
            this.storage.initialize();

            this.messageService = new MessageService(configManager);
            this.claimService = new ClaimService(this, kitManager, storage, messageService, configManager);
            this.guiService = new GuiService(this, kitManager, claimService, configManager);

            registerCommands();
            Bukkit.getPluginManager().registerEvents(new GuiListener(guiService), this);

            getLogger().info("EzKits enabled successfully. Loaded " + kitManager.getKitCount() + " kits.");
        } catch (Exception ex) {
            getLogger().severe("Failed to enable EzKits: " + ex.getMessage());
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }
    }

    private void registerCommands() {
        registerCommand("kits", new KitsCommand(guiService, messageService));
        registerCommand("kit", new KitCommand(kitManager, claimService, guiService, messageService));
        registerCommand("ezkits", new EzKitsAdminCommand(this, kitManager, claimService, guiService, messageService));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public void reloadPlugin() {
        configManager.loadAll();
        kitManager.loadKits();
        getLogger().info("EzKits reloaded. Loaded " + kitManager.getKitCount() + " kits.");
    }
}
