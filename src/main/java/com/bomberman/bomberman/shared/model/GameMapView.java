package com.bomberman.bomberman.shared.model;

/**
 * Read-only view of the GameMap.
 */
public interface GameMapView {

    Tile getTile(int row, int col);

    int getRows();

    int getCols();
}