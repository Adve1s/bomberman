package com.bomberman.bomberman.shared.network;

import com.bomberman.bomberman.shared.model.GameState;

/**
 * Server's tick broadcast: the full current game state.
 * Sent at the server tick rate (~30Hz). Clients render the latest one received.
 */
public class StateSnapshot implements NetworkMessage {
    private final GameState state;

    public StateSnapshot(GameState state) {
        this.state = state;
    }

    public GameState getState() {
        return state;
    }
}