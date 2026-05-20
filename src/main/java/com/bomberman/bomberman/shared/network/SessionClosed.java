package com.bomberman.bomberman.shared.network;

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
