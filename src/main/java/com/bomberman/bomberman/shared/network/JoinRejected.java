package com.bomberman.bomberman.shared.network;

/**
 * Sent in response to JoinRequest when the server refuses the player.
 * Reason is a human-readable string the client can display.
 */
public class JoinRejected implements ServerToClientMessage {
    private final String reason;

    public JoinRejected(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}