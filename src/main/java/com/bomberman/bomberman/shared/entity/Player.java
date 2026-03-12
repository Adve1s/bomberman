package com.bomberman.bomberman.shared.entity;

import com.bomberman.bomberman.shared.util.Constants;
import com.bomberman.bomberman.shared.util.Direction;

/**
 * A player on the map.
 *
 * Movement is grid-based with smooth interpolation:
 *   - move(direction) sets a target tile (if not already moving)
 *   - update(deltaTime) slides pixelX/pixelY toward the target each frame
 *   - On arrival, row/col update to the target and the player can move again
 *   - If the key is still held, the runner calls move() again immediately,
 *     so movement feels continuous with no gap between tiles
 */
public class Player extends Entity implements PlayerView {

    private final int playerId;

    // Stats (affected by power-ups)
    private double speed;
    private int maxBombs;
    private int explosionRange;

    // Current bomb tracking
    private int bombsPlaced;

    // Smooth movement state

    /** Whether the player is currently sliding between tiles. */
    private boolean moving;

    /** The tile we're sliding toward. */
    private int targetRow;
    private int targetCol;

    /** Pixel position of the target tile (cached to avoid recalculating each frame). */
    private double targetPixelX;
    private double targetPixelY;

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
        this.moving = false;
    }

    @Override
    public void update(double deltaTime) {
        if (!active) return;
        if (!moving) return;

        double pixelsPerSecond = speed * Constants.TILE_SIZE;
        double maxStep = pixelsPerSecond * deltaTime;

        // Distance remaining to target
        double dx = targetPixelX - pixelX;
        double dy = targetPixelY - pixelY;
        double distanceRemaining = Math.sqrt(dx * dx + dy * dy);

        if (distanceRemaining <= maxStep) {
            // Arrived — snap to exact target position
            pixelX = targetPixelX;
            pixelY = targetPixelY;
            row = targetRow;
            col = targetCol;
            moving = false;
        } else {
            // Slide toward target
            pixelX += (dx / distanceRemaining) * maxStep;
            pixelY += (dy / distanceRemaining) * maxStep;
        }
    }

    // Movement

    /**
     * Requests a move one tile in the given direction.
     * Ignored if the player is already sliding to a target tile.
     */
    public void move(Direction direction) {
        if (!active) return;
        if (moving) return;

        int newRow = row + direction.rowDelta;
        int newCol = col + direction.colDelta;

        // Basic bounds check — wall/box collision will be added by GameManager later
        if (newRow < 0 || newRow >= Constants.GRID_ROWS) return;
        if (newCol < 0 || newCol >= Constants.GRID_COLS) return;

        targetRow = newRow;
        targetCol = newCol;
        targetPixelX = newCol * Constants.TILE_SIZE;
        targetPixelY = newRow * Constants.TILE_SIZE;
        moving = true;
    }

    /**
     * Whether the player is currently between tiles.
     * Useful for GameManager later — e.g. prevent placing a bomb while moving.
     */
    public boolean isMoving() {
        return moving;
    }

    // Bomb management

    public boolean canPlaceBomb() {
        return bombsPlaced < maxBombs;
    }

    public void bombPlaced() {
        bombsPlaced++;
    }

    public void bombExploded() {
        if (bombsPlaced > 0) bombsPlaced--;
    }

    // Power-up application

    public void addBombCapacity(int amount) {
        maxBombs += amount;
    }

    public void addExplosionRange(int amount) {
        explosionRange += amount;
    }

    public void addSpeed(double amount) {
        speed += amount;
    }

    // ── PlayerView (read-only) ──

    @Override
    public int getPlayerId() {
        return playerId;
    }

    @Override
    public boolean isAlive() {
        return active;
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