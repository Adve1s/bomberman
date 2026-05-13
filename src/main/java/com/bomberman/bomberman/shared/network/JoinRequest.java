package com.bomberman.bomberman.shared.network;

/**
 * Sent by a client right after connecting, announcing its desired name.
 * Server replies with JoinAccepted or JoinRejected.
 */
public class JoinRequest implements ClientToServerMessage {
    private final String playerName;

    public JoinRequest(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}