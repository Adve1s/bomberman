package com.bomberman.bomberman.shared.entity;

import com.bomberman.bomberman.shared.util.Constants;

/**
 * Base class for anything that lives on the game map:
 * players, bombs, explosions, power-ups.
 *
 * Tracks grid position (which tile the entity is on)
 * and an active flag used for deferred removal.
 */
public abstract class Entity implements EntityView {

    /** Position on the tile grid. */
    protected int row;
    protected int col;

    /** Pixel coordinates (where to draw this entity). */
    protected double pixelX;
    protected double pixelY;

    /**
     * Whether this entity is still active.
     * When set to false, the entity will be removed
     * from its list at the end of the current update cycle.
     */
    protected boolean active;

    /**
     * @param row starting row on the map
     * @param col starting column on the map
     */
    protected Entity(int row, int col) {
        this.row = row;
        this.col = col;
        this.pixelX = col * Constants.TILE_SIZE;
        this.pixelY = row * Constants.TILE_SIZE;
        this.active = true;
    }

    /**
     * Called every frame to advance this entity's state.
     * Every subclass must implement this, even if the body is empty.
     *
     * @param deltaTime seconds since last update
     */
    public abstract void update(double deltaTime);

    // EntityView (read-only)

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public double getPixelX() {
        return pixelX;
    }

    @Override
    public double getPixelY() {
        return pixelY;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    // Mutation (server-side only)

    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public void setPixelPosition(double x, double y) {
        this.pixelX = x;
        this.pixelY = y;
    }

    /**
     * Marks this entity for removal at the end of the update cycle.
     */
    public void destroy() {
        this.active = false;
    }
}