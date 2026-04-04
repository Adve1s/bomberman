package com.bomberman.bomberman.shared.model;

import com.bomberman.bomberman.shared.util.Constants;

/**
 * The game board: a 2D grid of Tile values.
 */
public class GameMap implements GameMapView {

    private final int cols;
    private final int rows;
    private final Tile[][] grid;

    public GameMap(int rows, int cols) {
        this.cols = cols;
        this.rows = rows;
        this.grid = new Tile[rows][cols];
        generateDefaultMap();
    }

    public GameMap() {
        this(Constants.GRID_ROWS, Constants.GRID_COLS);
    }

    /**
     * Generates the classic Bomberman map layout:
     * - Border of walls around the edge
     * - Walls on every other row+col intersection (the pillar pattern)
     * - Bricks scattered in remaining spaces
     * - Corners near player spawns kept clear
     */
    private void generateDefaultMap() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (isBorder(row, col)) {
                    grid[row][col] = Tile.WALL;
                } else if (isPillar(row, col)) {
                    grid[row][col] = Tile.WALL;
                } else {
                    grid[row][col] = Tile.FLOOR;
                }
            }
        }

        // Scatter bricks on remaining floor tiles (except spawn zones)
        fillBoxes();

        // Ensure spawn corners are clear so players aren't trapped
        clearSpawnZones();
    }

    /**
     * Fills empty floor tiles with bricks at ~40% density.
     */
    private void fillBoxes() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (grid[row][col] == Tile.FLOOR && Math.random() < 0.4) {
                    grid[row][col] = Tile.BOX;
                }
            }
        }
    }

    /**
     * Clears a small L-shaped area in each corner so players can move
     */
    private void clearSpawnZones() {
        // Top-left (player 1 spawn)
        clearTile(1, 1);
        clearTile(1, 2);
        clearTile(2, 1);

        // Top-right
        clearTile(1, cols - 2);
        clearTile(1, cols - 3);
        clearTile(2, cols - 2);

        // Bottom-left
        clearTile(rows - 2, 1);
        clearTile(rows - 2, 2);
        clearTile(rows - 3, 1);

        // Bottom-right
        clearTile(rows - 2, cols - 2);
        clearTile(rows - 2, cols - 3);
        clearTile(rows - 3, cols - 2);
    }

    private void clearTile(int row, int col) {
        if (isInBounds(row, col)) {
            grid[row][col] = Tile.FLOOR;
        }
    }

    private boolean isBorder(int row, int col) {
        return row == 0 || col == 0 || row == rows - 1 || col == cols - 1;
    }

    private boolean isPillar(int row, int col) {
        return row % 2 == 0 && col % 2 == 0;
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    // GameMapView (read-only)

    @Override
    public Tile getTile(int row, int col) {
        if (!isInBounds(row, col)) {
            return Tile.WALL; // out-of-bounds treated as wall
        }
        return grid[row][col];
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getCols() {
        return cols;
    }

    // Mutation (server-side only)

    public void setTile(int row, int col, Tile tile) {
        if (isInBounds(row, col)) {
            grid[row][col] = tile;
        }
    }

    public boolean isWalkable(int row, int col) {
        return getTile(row, col) == Tile.FLOOR;
    }

    public Tile[][] getGrid() {
        return grid;
    }
}