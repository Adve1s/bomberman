package com.bomberman.bomberman.shared.network;

import com.bomberman.bomberman.shared.util.Direction;

/**
 * Sent each tick the client wants to move in this direction.
 * Sending stops when the player releases the key — no explicit stop message needed.
 */
public class MoveCommand implements ClientToServerMessage {
    private final Direction direction;

    public MoveCommand(Direction direction) {
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }
}