package com.bomberman.bomberman.shared.model;

import com.bomberman.bomberman.shared.entity.*;

import java.util.List;

/**
 * Read-only view of the GameState.
 * Returns lists as {@code List<? extends XxxView>}
 */
public interface GameStateView {

    GameMapView getGameMapView();

    List<? extends PlayerView> getPlayerViews();

    List<? extends BombView> getBombViews();

    List<? extends ExplosionView> getExplosionViews();

    List<? extends PowerUpView> getPowerUpViews();

    boolean isGameOver();
}