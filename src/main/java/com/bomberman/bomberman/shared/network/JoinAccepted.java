package com.bomberman.bomberman.shared.network;

/**
 * Sent in response to JoinRequest when the server accepts the player.
 * Tells the client which playerId it has been assigned.
 */
public class JoinAccepted implements ServerToClientMessage {
    private final int playerId;

    public JoinAccepted(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }
}