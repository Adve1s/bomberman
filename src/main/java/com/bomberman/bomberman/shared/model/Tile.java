package com.bomberman.bomberman.shared.model;
/**
 * Represents the static tile types on the game map.
 * FLOOR  - empty walkable space
 * WALL   - indestructible border/pillar
 * BOX  - destructible block, may hide a power-up
 */
public enum Tile {
    FLOOR,
    WALL,
    BOX
}
