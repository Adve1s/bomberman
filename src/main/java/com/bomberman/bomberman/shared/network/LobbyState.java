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
    private final List<Boolean> readyStates;
    private final int hostPlayerId;


    public LobbyState(List<String> playerNames, List<Boolean> readyStates, int hostPlayerId) {
        this.playerNames = playerNames;
        this.readyStates = readyStates;
        this.hostPlayerId = hostPlayerId;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public List<Boolean> getReadyStates() {
        return readyStates;
    }

    public int getHostPlayerId() {
        return hostPlayerId;
    }
}