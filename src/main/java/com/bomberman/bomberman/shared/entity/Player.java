package com.bomberman.bomberman.shared.entity;

import com.bomberman.bomberman.shared.util.Constants;

/**
 * A player on the map.
 */
public class Player extends Entity implements PlayerView {

    private final int playerId;

    // Stats (affected by power-ups)
    private double speed;
    private int maxBombs;
    private int explosionRange;

    // Current bomb tracking
    private int bombsPlaced;

    /**
     * @param playerId unique identifier for this player
     * @param row      starting grid row
     * @param col      starting grid column
     */
    public Player(int playerId, int row, int col) {
        super(row, col);
        this.playerId = playerId;
        this.speed = Constants.DEFAULT_PLAYER_SPEED;
        this.maxBombs = Constants.DEFAULT_BOMB_COUNT;
        this.explosionRange = Constants.DEFAULT_EXPLOSION_RANGE;
        this.bombsPlaced = 0;
    }

    @Override
    public void update(double deltaTime) {
        if (!active) return;

        // Sync grid position to whichever tile the player's center is on
        double centerX = pixelX + Constants.TILE_SIZE / 2.0;
        double centerY = pixelY + Constants.TILE_SIZE / 2.0;
        this.col = (int) (centerX / Constants.TILE_SIZE);
        this.row = (int) (centerY / Constants.TILE_SIZE);
    }

    // PlayerView (read-only)

    @Override
    public int getPlayerId() {
        return playerId;
    }

    @Override
    public boolean isAlive() {
        return active;
    }

    // Bomb management (server-side only)

    public boolean canPlaceBomb() {
        return bombsPlaced < maxBombs;
    }

    public void bombPlaced() {
        bombsPlaced++;
    }

    public void bombExploded() {
        if (bombsPlaced > 0) bombsPlaced--;
    }

    // Power-up application (server-side only)

    public void addBombCapacity(int amount) {
        maxBombs += amount;
    }

    public void addExplosionRange(int amount) {
        explosionRange += amount;
    }

    public void addSpeed(double amount) {
        speed += amount;
    }

    // Getters

    public double getSpeed() {
        return speed;
    }

    public int getMaxBombs() {
        return maxBombs;
    }

    public int getExplosionRange() {
        return explosionRange;
    }

    public void kill() {
        destroy();
    }
}