package com.bomberman.bomberman.shared.network;

/**
 * Sent by the server when the current session is no longer valid and the
 * client should leave the current flow (e.g. disconnected host, closed lobby).
 */
public class SessionClosed implements ServerToClientMessage{
    private String reason;

    public SessionClosed() {
    }

    public SessionClosed(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
