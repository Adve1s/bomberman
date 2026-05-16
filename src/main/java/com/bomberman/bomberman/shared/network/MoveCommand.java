package com.bomberman.bomberman.shared.network;

/**
 * Sent each frame the client wants to move. dx and dy together form a 2D intent:
 *   dx = -1 (left), 0 (none), 1 (right)
 *   dy = -1 (up),   0 (none), 1 (down)
 */
public class MoveCommand implements ClientToServerMessage {
    private final int dx;
    private final int dy;

    public MoveCommand(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() { return dx; }
    public int getDy() { return dy; }
}