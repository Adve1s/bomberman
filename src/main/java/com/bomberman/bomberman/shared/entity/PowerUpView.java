package com.bomberman.bomberman.shared.entity;

import com.bomberman.bomberman.shared.entity.PowerUp.PowerUpType;

/**
 * Read-only view of a PowerUp.
 * Exposes the type so the Renderer can draw different
 * visuals for each power-up kind.
 */
public interface PowerUpView extends EntityView {

    PowerUpType getType();
}