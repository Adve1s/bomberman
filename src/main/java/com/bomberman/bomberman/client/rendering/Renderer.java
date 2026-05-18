package com.bomberman.bomberman.client.rendering;

import com.bomberman.bomberman.shared.entity.*;
import com.bomberman.bomberman.shared.model.*;
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
    private static final Color COLOR_FLOOR = Color.rgb(140, 180, 100);
    private static final Color COLOR_WALL  = Color.rgb(80, 80, 80);
    private static final Color COLOR_BOX   = Color.rgb(180, 130, 70);
    private static final Color COLOR_WARNING_WALL  = Color.rgb(80, 80, 80, 0.6);

    // Entity colors
    private static final Color COLOR_BOMB       = Color.rgb(30, 30, 30);
    private static final Color COLOR_EXPLOSION   = Color.rgb(255, 100, 30);
    private static final Color COLOR_POWERUP_BOMB  = Color.rgb(255, 140, 90);
    private static final Color COLOR_POWERUP_SPEED = Color.rgb(90, 200, 255);
    private static final Color COLOR_POWERUP_RANGE = Color.rgb(180, 120, 255);
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
        drawWarningWalls(state);
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
                    case WALL -> graphicsContext.setFill(COLOR_WALL);
                    case BOX   -> graphicsContext.setFill(COLOR_BOX);
                    case FLOOR -> graphicsContext.setFill(COLOR_FLOOR);
                }

                double x = col * Constants.TILE_SIZE;
                double y = row * Constants.TILE_SIZE;

                graphicsContext.fillRect(x, y, Constants.TILE_SIZE, Constants.TILE_SIZE);

                graphicsContext.setStroke(GRID_LINE_COLOR);
                graphicsContext.strokeRect(x, y, Constants.TILE_SIZE, Constants.TILE_SIZE);
            }
        }
    }

    // Waning wall blinking

    private void drawWarningWalls(GameStateView state) {
        double phase = (state.getRoundTime() * 4) % 1.3;
        boolean blink = phase < 0.5;

        if (!blink) return;

        graphicsContext.setFill(COLOR_WARNING_WALL);

        for (WarningTileView tile : state.getWarningTiles()) {

            double x = tile.getCol() * Constants.TILE_SIZE;
            double y = tile.getRow() * Constants.TILE_SIZE;

            graphicsContext.fillRect(
                    x,
                    y,
                    Constants.TILE_SIZE,
                    Constants.TILE_SIZE
            );
        }
    }

    // Entities

    private void drawPowerUps(GameStateView state) {
        for (PowerUpView powerUp : state.getPowerUpViews()) {
            if (!powerUp.isActive()) continue;

            switch (powerUp.getType()) {
                case EXTRA_BOMB -> graphicsContext.setFill(COLOR_POWERUP_BOMB);
                case SPEED_BOOST -> graphicsContext.setFill(COLOR_POWERUP_SPEED);
                case EXTRA_RANGE -> graphicsContext.setFill(COLOR_POWERUP_RANGE);
            }

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