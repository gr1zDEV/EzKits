package com.ezinnovations.ezkits.storage;

import com.ezinnovations.ezkits.EzKitsPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class SQLitePlayerKitStorage implements PlayerKitStorage {

    private final EzKitsPlugin plugin;
    private Connection connection;

    public SQLitePlayerKitStorage(EzKitsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_kits (" +
                        "uuid TEXT NOT NULL," +
                        "kit_id TEXT NOT NULL," +
                        "cooldown_expiry INTEGER NOT NULL DEFAULT 0," +
                        "one_time_claimed INTEGER NOT NULL DEFAULT 0," +
                        "PRIMARY KEY(uuid, kit_id)" +
                        ")");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize SQLite storage", e);
        }
    }

    @Override
    public long getCooldownExpiry(UUID playerId, String kitId) {
        String sql = "SELECT cooldown_expiry FROM player_kits WHERE uuid = ? AND kit_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, kitId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("cooldown_expiry");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to read cooldown for " + playerId + ": " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean isOneTimeClaimed(UUID playerId, String kitId) {
        String sql = "SELECT one_time_claimed FROM player_kits WHERE uuid = ? AND kit_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, kitId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("one_time_claimed") == 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to read one-time state for " + playerId + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public void markClaimed(UUID playerId, String kitId, long cooldownExpiry, boolean oneTimeClaimed) {
        String sql = "INSERT INTO player_kits(uuid, kit_id, cooldown_expiry, one_time_claimed) VALUES(?, ?, ?, ?) " +
                "ON CONFLICT(uuid, kit_id) DO UPDATE SET cooldown_expiry = excluded.cooldown_expiry, one_time_claimed = excluded.one_time_claimed";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, kitId);
            stmt.setLong(3, cooldownExpiry);
            stmt.setInt(4, oneTimeClaimed ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save claim state for " + playerId + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
