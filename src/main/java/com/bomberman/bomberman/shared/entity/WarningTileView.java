package com.bomberman.bomberman.shared.entity;

public interface WarningTileView extends EntityView {

    double getStartTime();

    int getRow();

    int getCol();

    boolean shouldBecomeWall(double now, double warningWallDuration);
}