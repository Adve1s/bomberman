package com.bomberman.bomberman.shared.network;

/**
 * Sent by a client toggling their ready state in the lobby.
 */
public class ReadyCommand implements ClientToServerMessage {
    private final boolean ready;

    public ReadyCommand(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }
}