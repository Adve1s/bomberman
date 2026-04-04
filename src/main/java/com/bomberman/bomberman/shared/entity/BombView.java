package com.bomberman.bomberman.shared.entity;

/**
 * Read-only view of a Bomb.
 */
public interface BombView extends EntityView {

    int getOwnerPlayerId();

    double getFuseTimer();
}