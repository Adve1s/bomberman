package com.bomberman.bomberman.shared.entity;

/**
 * Read-only view of a Player.
 */
public interface PlayerView extends EntityView {

    int getPlayerId();

    boolean isAlive();
}