package com.bomberman.bomberman.shared.util;

public final class Constants {

    private Constants() {} // no instantiation

    // Grid dimensions (classic Bomberman is 15x13)
    public static final int GRID_COLS = 15;
    public static final int GRID_ROWS = 13;

    // Tile rendering size in pixels
    public static final int TILE_SIZE = 48;

    // Derived window dimensions
    public static final int WINDOW_WIDTH = GRID_COLS * TILE_SIZE;
    public static final int WINDOW_HEIGHT = GRID_ROWS * TILE_SIZE;

    // Player defaults
    public static final double DEFAULT_PLAYER_SPEED = 3.0;  // tiles per second
    public static final int DEFAULT_BOMB_COUNT = 1;
    public static final int DEFAULT_EXPLOSION_RANGE = 1;

    // Player hitbox — slightly smaller than a tile so the player can
    public static final int PLAYER_HITBOX_SIZE = 40;
    public static final int PLAYER_HITBOX_OFFSET = (TILE_SIZE - PLAYER_HITBOX_SIZE) / 2;

    // Bomb timing
    public static final double BOMB_FUSE_SECONDS = 2.5;

    // Explosion lifetime
    public static final double EXPLOSION_DURATION_SECONDS = 0.5;

    // Networking
    public static final int NETWORK_PORT = 8765;
    public static final int NETWORK_WRITE_BUFFER_SIZE = 32 * 1024;
    public static final int NETWORK_OBJECT_BUFFER_SIZE = 32 * 1024;
}