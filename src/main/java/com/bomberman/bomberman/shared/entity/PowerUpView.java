package com.bomberman.bomberman.shared.entity;

import com.bomberman.bomberman.shared.entity.PowerUp.PowerUpType;

/**
 * Read-only view of a PowerUp.
 */
public interface PowerUpView extends EntityView {

    PowerUpType getType();
}