package com.bomberman.bomberman.shared.entity;

import com.bomberman.bomberman.shared.util.Constants;

/**
 * One tile of an explosion.
 */
public class Explosion extends Entity implements ExplosionView {

    private double lifetime;

    public Explosion(int gridRow, int gridCol) {
        super(gridRow, gridCol);
        this.lifetime = Constants.EXPLOSION_DURATION_SECONDS;
    }

    @Override
    public void update(double deltaTime) {
        lifetime -= deltaTime;
        if (lifetime <= 0) {
            destroy();
        }
    }

    // ExplosionView (read-only)

    @Override
    public double getLifetime() {
        return lifetime;
    }
}