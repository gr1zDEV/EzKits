package com.ezinnovations.ezkits.storage;

import java.util.UUID;

public interface PlayerKitStorage {
    void initialize();

    long getCooldownExpiry(UUID playerId, String kitId);

    boolean isOneTimeClaimed(UUID playerId, String kitId);

    void markClaimed(UUID playerId, String kitId, long cooldownExpiry, boolean oneTimeClaimed);

    void close();
}
