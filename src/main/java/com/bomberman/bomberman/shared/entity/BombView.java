package com.bomberman.bomberman.shared.entity;

/**
 * Read-only view of a Bomb.
 * Exposes identity and fuse state for rendering
 * (e.g. visual countdown or pulsing animation).
 */
public interface BombView extends EntityView {

    int getOwnerPlayerId();

    double getFuseTimer();
}