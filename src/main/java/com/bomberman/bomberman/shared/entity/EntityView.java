package com.bomberman.bomberman.shared.entity;

/**
 * Read-only view of an Entity.
 */
public interface EntityView {

    double getPixelX();

    double getPixelY();

    boolean isActive();
}