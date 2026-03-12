package com.bomberman.bomberman.shared.entity;

/**
 * A collectible power-up revealed when a brick is destroyed.
 * Sits on the grid until a player walks over it.
 */
public class PowerUp extends Entity implements PowerUpView {

    /**
     * The different power-up effects available.
     */
    public enum PowerUpType {
        EXTRA_BOMB,       // +1 bomb capacity
        EXTRA_RANGE,      // +1 explosion range
        SPEED_BOOST       // +movement speed
    }

    private final PowerUpType powerUpType;

    public PowerUp(PowerUpType powerUpType, int gridRow, int gridCol) {
        super(gridRow, gridCol);
        this.powerUpType = powerUpType;
    }

    @Override
    public void update(double deltaTime) {
        // Power-ups are static, no per-frame behavior.
        // Pickup is handled by collision detection in GameState.
    }

    // PowerUpView (read-only)

    @Override
    public PowerUpType getType() {
        return powerUpType;
    }
}