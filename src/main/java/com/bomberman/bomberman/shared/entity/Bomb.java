package com.bomberman.bomberman.shared.entity;

import com.bomberman.bomberman.shared.util.Constants;

/**
 * A bomb sitting on the grid.
 */
public class Bomb extends Entity implements BombView {

    private final int ownerPlayerId;
    private final int range;
    private double fuseTimer;

    public Bomb(int ownerPlayerId, int gridRow, int gridCol, int range) {
        super(gridRow, gridCol);
        this.ownerPlayerId = ownerPlayerId;
        this.range = range;
        this.fuseTimer = Constants.BOMB_FUSE_SECONDS;
    }

    @Override
    public void update(double deltaTime) {
        if (!active) return;

        fuseTimer -= deltaTime;
        if (fuseTimer <= 0) {
            explode();
        }
    }

    /**
     * Trigger this bomb to explode immediately (fuse ran out or chain reaction).
     */
    public void explode() {
        if (active) {
            destroy();
        }
    }

    // BombView (read-only)

    @Override
    public int getOwnerPlayerId() {
        return ownerPlayerId;
    }

    @Override
    public double getFuseTimer() {
        return fuseTimer;
    }

    // Getters

    public int getRange() {
        return range;
    }

}