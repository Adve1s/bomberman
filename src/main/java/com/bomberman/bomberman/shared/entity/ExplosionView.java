package com.bomberman.bomberman.shared.entity;

/**
 * Read-only view of an Explosion.
 * Exposes lifetime so the Renderer can fade out the visual.
 */
public interface ExplosionView extends EntityView {

    double getLifetime();
}