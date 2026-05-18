package com.bomberman.bomberman.shared.model;

public class WarningTile implements WarningTileView{
    private final int row;
    private final int col;
    private final double startTime;

    public WarningTile(int row, int col, double startTime) {
        this.row = row;
        this.col = col;
        this.startTime = startTime;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public double getStartTime() {
        return startTime;
    }

    /**
     * Returns true when the warning duration has expired
     * and this tile should become a wall.
     */
    public boolean shouldBecomeWall(double currentTime, double warningDuration) {
        return currentTime - startTime >= warningDuration;
    }
}
