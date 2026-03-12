package com.bomberman.bomberman.shared.model;

import com.bomberman.bomberman.shared.entity.*;

import java.util.List;

/**
 * Read-only view of the GameState.
 * Used by the Renderer and client-side code.
 *
 * Returns lists as {@code List<? extends XxxView>} so the
 * Renderer can iterate and read, but cannot cast back to the
 * concrete types or call mutation methods.
 */
public interface GameStateView {

    GameMapView getGameMapView();

    List<? extends PlayerView> getPlayerViews();

    List<? extends BombView> getBombViews();

    List<? extends ExplosionView> getExplosionViews();

    List<? extends PowerUpView> getPowerUpViews();

    boolean isGameOver();
}