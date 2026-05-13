package com.bomberman.bomberman.shared.network;

/**
 * Sent periodically by a client to measure round-trip latency.
 * Server echoes back a Pong with the same timestamp.
 */
public class Ping implements ClientToServerMessage {
    private final long clientTimestamp;

    public Ping(long clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }

    public long getClientTimestamp() {
        return clientTimestamp;
    }
}