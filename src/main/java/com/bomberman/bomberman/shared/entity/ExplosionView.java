package com.bomberman.bomberman.shared.entity;

/**
 * Read-only view of an Explosion.
 */
public interface ExplosionView extends EntityView {

    double getLifetime();
}