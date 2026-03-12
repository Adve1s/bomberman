package com.bomberman.bomberman.shared.entity;

/**
 * Read-only view of a Player.
 * Exposes only what the Renderer and client-side code need:
 * identity, position, and alive state.
 */
public interface PlayerView extends EntityView {

    int getPlayerId();

    boolean isAlive();
}