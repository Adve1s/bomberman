package com.bomberman.bomberman.shared.entity;

/**
 * Read-only view of an Entity.
 * Used by the Renderer and client-side code that should
 * only read positions and state, never mutate them.
 */
public interface EntityView {

    double getPixelX();

    double getPixelY();

    boolean isActive();
}