package com.bomberman.bomberman.shared.network;

import java.util.List;

/**
 * Broadcast to all clients in the lobby whenever someone joins, leaves,
 * or toggles ready. Lists currently connected players.
 *
 * Teammate A: feel free to extend this with ready states or other fields
 * as your UI requires.
 */
public class LobbyState implements ServerToClientMessage {
    private final List<String> playerNames;
    private final int hostPlayerId;

    public LobbyState(List<String> playerNames, int hostPlayerId) {
        this.playerNames = playerNames;
        this.hostPlayerId = hostPlayerId;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public int getHostPlayerId() {
        return hostPlayerId;
    }
}