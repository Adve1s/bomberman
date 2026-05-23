package com.bomberman.bomberman.shared.entity;

public class WarningTile extends Entity implements WarningTileView {

    private final double startTime;

    public WarningTile(int row, int col, double startTime) {
        super(row, col);
        this.startTime = startTime;
    }

    @Override
    public void update(double deltaTime) {}

    @Override
    public double getStartTime() {
        return startTime;
    }

    public boolean shouldBecomeWall(double currentTime, double warningDuration) {
        return currentTime - startTime >= warningDuration;
    }
}