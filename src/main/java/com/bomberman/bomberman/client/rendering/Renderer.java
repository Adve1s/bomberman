package com.bomberman.bomberman.client.rendering;

import com.bomberman.bomberman.shared.entity.*;
import com.bomberman.bomberman.shared.model.GameMapView;
import com.bomberman.bomberman.shared.model.GameStateView;
import com.bomberman.bomberman.shared.model.Tile;
import com.bomberman.bomberman.shared.util.Constants;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws the game state onto a JavaFX Canvas.
 * Draw order (back to front):
 *   1. Tile grid (floor, walls, boxes)
 *   2. Power-ups
 *   3. Bombs
 *   4. Explosions
 *   5. Players
 */
public class Renderer {

    // Tile colors
    private static final Color COLOR_FLOOR = Color.rgb(95, 155, 75);
    private static final Color COLOR_GRASS_DARK = Color.rgb(65, 120, 55);
    private static final Color COLOR_GRASS_LIGHT = Color.rgb(120, 180, 90);

    private static final Color COLOR_ROCK_BASE = Color.rgb(135, 140, 135);
    private static final Color COLOR_ROCK_DARK = Color.rgb(95, 100, 95);
    private static final Color COLOR_ROCK_LIGHT = Color.rgb(185, 190, 180);

    private static final Color COLOR_STONE_GROUND = Color.rgb(150, 155, 150);
    private static final Color COLOR_STONE_GROUND_DARK = Color.rgb(125, 130, 125);
    private static final Color COLOR_STONE_GROUND_LIGHT = Color.rgb(175, 180, 175);

    private static final Color COLOR_BUSH_BASE = Color.rgb(55, 105, 45);
    private static final Color COLOR_BUSH_DARK = Color.rgb(35, 80, 30);
    private static final Color COLOR_BUSH_LIGHT = Color.rgb(75, 135, 60);

    private static final Color COLOR_BOX   = Color.rgb(180, 130, 70);

    // Entity colors
    private static final Color COLOR_BOMB       = Color.rgb(30, 30, 30);
    private static final Color COLOR_EXPLOSION   = Color.rgb(255, 100, 30);
    private static final Color COLOR_POWERUP     = Color.rgb(255, 215, 0);
    private static final Color COLOR_PLAYER_DEAD = Color.rgb(100, 100, 100);

    // Player colors by ID (supports up to 4 players)
    private static final Color[] PLAYER_COLORS = {
            Color.rgb(60, 120, 255),   // blue
            Color.rgb(230, 50, 50),    // red
            Color.rgb(50, 200, 50),    // green
            Color.rgb(230, 200, 50)    // yellow
    };

    // Entity padding (pixels inset from tile edge)
    private static final int PLAYER_PADDING    = 4;
    private static final int BOMB_PADDING      = 6;
    private static final int EXPLOSION_PADDING = 4;
    private static final int POWERUP_PADDING   = 10;
    private static final int PLAYER_CORNER_RADIUS = 8;

    // Grid line styling
    private static final Color GRID_LINE_COLOR = Color.rgb(0, 0, 0, 0.15);

    private final Canvas canvas;
    private final GraphicsContext graphicsContext;

    public Renderer(Canvas canvas) {
        this.canvas = canvas;
        this.graphicsContext = canvas.getGraphicsContext2D();
    }

    /**
     * Clears the canvas and redraws everything from the current game state.
     */
    public void render(GameStateView state) {
        graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawTiles(state.getGameMapView());
        drawPowerUps(state);
        drawBombs(state);
        drawExplosions(state);
        drawPlayers(state);
    }

    // Tile grid

    private void drawTiles(GameMapView map) {
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                Tile tile = map.getTile(row, col);

                switch (tile) {
                    case WALL  -> graphicsContext.setFill(COLOR_ROCK_BASE);
                    case BOX   -> graphicsContext.setFill(COLOR_BOX);
                    case FLOOR -> graphicsContext.setFill(COLOR_FLOOR);
                }

                double x = col * Constants.TILE_SIZE;
                double y = row * Constants.TILE_SIZE;

                graphicsContext.fillRect(x, y, Constants.TILE_SIZE, Constants.TILE_SIZE);
                if (tile == Tile.FLOOR) {
                    drawPixelGrassDetails(x, y, row, col);
                }
                if (tile == Tile.WALL) {
                    if (isOuterBorderTile(map, row, col)) {
                        drawBushWall(x, y, row, col);
                    } else {
                        drawRockWall(x, y, row, col);
                    }
                }

                graphicsContext.setStroke(GRID_LINE_COLOR);
                graphicsContext.strokeRect(x, y, Constants.TILE_SIZE, Constants.TILE_SIZE);
            }
        }
    }

    private boolean isOuterBorderTile(GameMapView map, int row, int col) {
        return row == 0 || col == 0 || row == map.getRows() - 1 || col == map.getCols() - 1;
    }

    private void drawPixelGrassDetails(double x, double y, int row, int col) {
        int gridSize = 8;
        double cell = Constants.TILE_SIZE / (double) gridSize;

        for (int gy = 0; gy < gridSize; gy++) {
            for (int gx = 0; gx < gridSize; gx++) {

                int hash = row * 73856093
                        ^ col * 19349663
                        ^ gx * 83492791
                        ^ gy * 1234567;

                hash = Math.abs(hash);
                int value = hash % 100;

                if (value < 12) {
                    graphicsContext.setFill(COLOR_GRASS_DARK);
                    graphicsContext.fillRect(x + gx * cell, y + gy * cell, cell, cell);
                } else if (value < 24) {
                    graphicsContext.setFill(COLOR_GRASS_LIGHT);
                    graphicsContext.fillRect(x + gx * cell, y + gy * cell, cell, cell);
                }
            }
        }
    }

    private void drawRockWall(double x, double y, int row, int col) {
        double size = Constants.TILE_SIZE;

        graphicsContext.setFill(COLOR_STONE_GROUND);
        graphicsContext.fillRect(x, y, size, size);
        drawStoneGroundDetails(x, y, row, col);

        int variant = Math.abs(row * 31 + col * 17) % 3;

        double rockX;
        double rockY;
        double rockW;
        double rockH;

        if (variant == 0) {
            rockX = x + size * 0.08;
            rockY = y + size * 0.10;
            rockW = size * 0.84;
            rockH = size * 0.78;
        } else if (variant == 1) {
            rockX = x + size * 0.10;
            rockY = y + size * 0.12;
            rockW = size * 0.80;
            rockH = size * 0.76;
        } else {
            rockX = x + size * 0.12;
            rockY = y + size * 0.08;
            rockW = size * 0.78;
            rockH = size * 0.82;
        }

        graphicsContext.setFill(COLOR_ROCK_DARK);
        graphicsContext.fillRoundRect(
                rockX + size * 0.03,
                rockY + size * 0.04,
                rockW,
                rockH,
                14,
                14
        );

        graphicsContext.setFill(COLOR_ROCK_BASE);
        graphicsContext.fillRoundRect(
                rockX,
                rockY,
                rockW,
                rockH,
                14,
                14
        );

        graphicsContext.setFill(COLOR_ROCK_LIGHT);
        graphicsContext.fillOval(
                rockX + rockW * 0.16,
                rockY + rockH * 0.15,
                rockW * 0.22,
                rockH * 0.16
        );

        graphicsContext.setFill(COLOR_ROCK_DARK);
        graphicsContext.fillOval(
                rockX + rockW * 0.58,
                rockY + rockH * 0.52,
                rockW * 0.18,
                rockH * 0.14
        );
    }

    private void drawStoneGroundDetails(double x, double y, int row, int col) {
        int gridSize = 8;
        double cell = Constants.TILE_SIZE / (double) gridSize;

        for (int gy = 0; gy < gridSize; gy++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int hash = row * 73856093
                        ^ col * 19349663
                        ^ gx * 83492791
                        ^ gy * 1234567;

                hash = Math.abs(hash);
                int value = hash % 100;

                if (value < 10) {
                    graphicsContext.setFill(COLOR_STONE_GROUND_DARK);
                    graphicsContext.fillRect(x + gx * cell, y + gy * cell, cell, cell);
                } else if (value < 20) {
                    graphicsContext.setFill(COLOR_STONE_GROUND_LIGHT);
                    graphicsContext.fillRect(x + gx * cell, y + gy * cell, cell, cell);
                }
            }
        }
    }

    private void drawBushWall(double x, double y, int row, int col) {
        double size = Constants.TILE_SIZE;

        graphicsContext.setFill(COLOR_BUSH_BASE);
        graphicsContext.fillRect(x, y, size, size);

        graphicsContext.setFill(COLOR_BUSH_DARK);
        graphicsContext.fillOval(x + size * 0.05, y + size * 0.20, size * 0.45, size * 0.45);
        graphicsContext.fillOval(x + size * 0.35, y + size * 0.10, size * 0.45, size * 0.45);
        graphicsContext.fillOval(x + size * 0.20, y + size * 0.40, size * 0.50, size * 0.40);

        graphicsContext.setFill(COLOR_BUSH_LIGHT);
        graphicsContext.fillOval(x + size * 0.15, y + size * 0.18, size * 0.18, size * 0.18);
        graphicsContext.fillOval(x + size * 0.52, y + size * 0.22, size * 0.16, size * 0.16);
        graphicsContext.fillOval(x + size * 0.35, y + size * 0.50, size * 0.14, size * 0.14);
    }

    // Entities

    private void drawPowerUps(GameStateView state) {
        for (PowerUpView powerUp : state.getPowerUpViews()) {
            if (!powerUp.isActive()) continue;

            graphicsContext.setFill(COLOR_POWERUP);
            graphicsContext.fillOval(
                    powerUp.getPixelX() + POWERUP_PADDING,
                    powerUp.getPixelY() + POWERUP_PADDING,
                    Constants.TILE_SIZE - POWERUP_PADDING * 2,
                    Constants.TILE_SIZE - POWERUP_PADDING * 2
            );
        }
    }

    private void drawBombs(GameStateView state) {
        for (BombView bomb : state.getBombViews()) {
            if (!bomb.isActive()) continue;

            graphicsContext.setFill(COLOR_BOMB);
            graphicsContext.fillOval(
                    bomb.getPixelX() + BOMB_PADDING,
                    bomb.getPixelY() + BOMB_PADDING,
                    Constants.TILE_SIZE - BOMB_PADDING * 2,
                    Constants.TILE_SIZE - BOMB_PADDING * 2
            );
        }
    }

    private void drawExplosions(GameStateView state) {
        for (ExplosionView explosion : state.getExplosionViews()) {
            if (!explosion.isActive()) continue;

            double alpha = Math.max(0.3, explosion.getLifetime() / Constants.EXPLOSION_DURATION_SECONDS);
            graphicsContext.setFill(Color.color(
                    COLOR_EXPLOSION.getRed(),
                    COLOR_EXPLOSION.getGreen(),
                    COLOR_EXPLOSION.getBlue(),
                    alpha
            ));
            graphicsContext.fillRect(
                    explosion.getPixelX() + EXPLOSION_PADDING,
                    explosion.getPixelY() + EXPLOSION_PADDING,
                    Constants.TILE_SIZE - EXPLOSION_PADDING * 2,
                    Constants.TILE_SIZE - EXPLOSION_PADDING * 2
            );
        }
    }

    private void drawPlayers(GameStateView state) {
        for (PlayerView player : state.getPlayerViews()) {
            Color color;

            if (!player.isAlive()) {
                color = COLOR_PLAYER_DEAD;
            } else {
                color = PLAYER_COLORS[player.getPlayerId() % PLAYER_COLORS.length];
            }

            graphicsContext.setFill(color);
            graphicsContext.fillRoundRect(
                    player.getPixelX() + PLAYER_PADDING,
                    player.getPixelY() + PLAYER_PADDING,
                    Constants.TILE_SIZE - PLAYER_PADDING * 2,
                    Constants.TILE_SIZE - PLAYER_PADDING * 2,
                    PLAYER_CORNER_RADIUS, PLAYER_CORNER_RADIUS
            );
        }
    }
}