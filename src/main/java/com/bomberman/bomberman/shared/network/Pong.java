package com.bomberman.bomberman.shared.network;

/**
 * Server's echo of a Ping. Client computes latency as
 * {@code System.currentTimeMillis() - clientTimestamp}.
 */
public class Pong implements ServerToClientMessage {
    private final long clientTimestamp;

    public Pong(long clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }

    public long getClientTimestamp() {
        return clientTimestamp;
    }
}