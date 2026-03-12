package com.bomberman.bomberman.shared.model;

/**
 * Read-only view of the GameMap.
 * Allows the Renderer to query tiles without being able
 * to modify the map (no setTile access).
 */
public interface GameMapView {

    Tile getTile(int row, int col);

    int getRows();

    int getCols();
}