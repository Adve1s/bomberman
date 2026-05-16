package com.bomberman.bomberman.shared.network;

/**
 * Broadcast when a player disconnects (during lobby or gameplay).
 * Clients should remove this player from their view.
 */
public class PlayerLeft implements ServerToClientMessage {
    private final int playerId;

    public PlayerLeft(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }
}