package com.bomberman.bomberman.shared.network;

import java.util.List;

/**
 * Broadcast to all clients in the lobby whenever someone joins, leaves,
 * or toggles ready. Lists currently connected players.
 */
public class LobbyState implements ServerToClientMessage {
    private final List<Integer> playerIds;
    private final List<String> playerNames;
    private final List<Boolean> readyStates;
    private final int hostPlayerId;


    public LobbyState(List<Integer> playerIds, List<String> playerNames, List<Boolean> readyStates, int hostPlayerId) {
        this.playerIds = playerIds;
        this.playerNames = playerNames;
        this.readyStates = readyStates;
        this.hostPlayerId = hostPlayerId;
    }

    public List<Integer> getPlayerIds() { return playerIds; }
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