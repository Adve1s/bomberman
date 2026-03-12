package com.bomberman.bomberman.shared.util;

/**
 * The four cardinal directions on the grid.
 * Each direction provides row/col deltas for moving one tile.
 */
public enum Direction {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1);

    public final int rowDelta;
    public final int colDelta;

    Direction(int rowDelta, int colDelta) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
    }
}
