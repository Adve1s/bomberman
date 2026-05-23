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

    private static final Color COLOR_STUMP_BASE = Color.rgb(140, 85, 45);
    private static final Color COLOR_STUMP_DARK = Color.rgb(95, 55, 30);
    private static final Color COLOR_STUMP_LIGHT = Color.rgb(190, 130, 75);
    private static final Color COLOR_STUMP_RING = Color.rgb(220, 170, 105);

    private static final Color COLOR_BOX   = Color.rgb(180, 130, 70);
    private static final Color COLOR_WARNING_WALL  = Color.rgb(80, 80, 80, 0.6);

    // Entity colors
    private static final Color COLOR_PINECONE_BASE = Color.rgb(125, 85, 45);
    private static final Color COLOR_PINECONE_DARK = Color.rgb(85, 55, 30);
    private static final Color COLOR_PINECONE_LIGHT = Color.rgb(165, 120, 75);
    private static final Color COLOR_PINECONE_STEM = Color.rgb(95, 70, 40);
    private static final Color COLOR_FUSE_SPARK = Color.rgb(255, 170, 40);
    private static final Color COLOR_FUSE_SPARK_LIGHT = Color.rgb(255, 220, 90);

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

    private static final Color COLOR_PLAYER_PUPIL = Color.rgb(30, 30, 30);
    private static final Color COLOR_PLAYER_SHADOW = Color.rgb(0, 0, 0, 0.25);
    private static final Color COLOR_GNOME_SKIN = Color.rgb(244, 214, 182);
    private static final Color COLOR_GNOME_BEARD = Color.rgb(235, 235, 235);
    private static final Color COLOR_GNOME_BOOTS = Color.rgb(90, 60, 35);
    private static final Color COLOR_PLAYER_OUTLINE = Color.rgb(40, 40, 40, 0.35);

    // Entity padding (pixels inset from tile edge)
    private static final int EXPLOSION_PADDING = 4;
    private static final int POWERUP_PADDING   = 10;

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
                    case WALL  -> graphicsContext.setFill(COLOR_ROCK_BASE);
                    case BOX   -> graphicsContext.setFill(COLOR_BOX);
                    case FLOOR -> graphicsContext.setFill(COLOR_FLOOR);
                }

                double x = col * Constants.TILE_SIZE;
                double y = row * Constants.TILE_SIZE;

                graphicsContext.fillRect(x, y, Constants.TILE_SIZE, Constants.TILE_SIZE);
                if (tile == Tile.FLOOR) {
                    drawPixelTextureDetails(x, y, row, col, COLOR_GRASS_DARK, COLOR_GRASS_LIGHT);
                }
                if (tile == Tile.WALL) {
                    if (isOuterBorderTile(map, row, col)) {
                        drawBushWall(x, y, row, col);
                    } else {
                        drawRockWall(x, y, row, col);
                    }
                }
                if (tile == Tile.BOX) {
                    drawStumpBox(x, y, row, col);
                }

                graphicsContext.setStroke(GRID_LINE_COLOR);
                graphicsContext.strokeRect(x, y, Constants.TILE_SIZE, Constants.TILE_SIZE);
            }
        }
    }

    private boolean isOuterBorderTile(GameMapView map, int row, int col) {
        return row == 0 || col == 0 || row == map.getRows() - 1 || col == map.getCols() - 1;
    }

    private void drawPixelTextureDetails(double x, double y, int row, int col, Color darkColor, Color lightColor) {
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
                    graphicsContext.setFill(darkColor);
                    graphicsContext.fillRect(x + gx * cell, y + gy * cell, cell, cell);
                } else if (value < 24) {
                    graphicsContext.setFill(lightColor);
                    graphicsContext.fillRect(x + gx * cell, y + gy * cell, cell, cell);
                }
            }
        }
    }

    private void drawRockWall(double x, double y, int row, int col) {
        double size = Constants.TILE_SIZE;

        graphicsContext.setFill(COLOR_STONE_GROUND);
        graphicsContext.fillRect(x, y, size, size);
        drawPixelTextureDetails(x, y, row, col, COLOR_STONE_GROUND_DARK, COLOR_STONE_GROUND_LIGHT);

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

    private void drawBushWall(double x, double y, int row, int col) {
        double size = Constants.TILE_SIZE;

        graphicsContext.setFill(COLOR_BUSH_DARK);
        graphicsContext.fillRect(x, y, size, size);

        int variant = Math.abs(row * 31 + col * 17) % 3;

        graphicsContext.setFill(COLOR_BUSH_BASE);

        if (variant == 0) {
            graphicsContext.fillOval(x + size * 0.00, y + size * 0.18, size * 0.45, size * 0.45);
            graphicsContext.fillOval(x + size * 0.30, y + size * 0.05, size * 0.50, size * 0.50);
            graphicsContext.fillOval(x + size * 0.55, y + size * 0.25, size * 0.45, size * 0.45);
            graphicsContext.fillOval(x + size * 0.20, y + size * 0.48, size * 0.55, size * 0.40);
        } else if (variant == 1) {
            graphicsContext.fillOval(x + size * 0.10, y + size * 0.05, size * 0.50, size * 0.50);
            graphicsContext.fillOval(x + size * 0.45, y + size * 0.12, size * 0.48, size * 0.48);
            graphicsContext.fillOval(x + size * 0.00, y + size * 0.42, size * 0.48, size * 0.45);
            graphicsContext.fillOval(x + size * 0.35, y + size * 0.45, size * 0.55, size * 0.42);
        } else {
            graphicsContext.fillOval(x + size * 0.02, y + size * 0.08, size * 0.55, size * 0.48);
            graphicsContext.fillOval(x + size * 0.42, y + size * 0.04, size * 0.52, size * 0.50);
            graphicsContext.fillOval(x + size * 0.18, y + size * 0.38, size * 0.60, size * 0.48);
        }

        graphicsContext.setFill(COLOR_BUSH_LIGHT);
        graphicsContext.fillOval(x + size * 0.18, y + size * 0.18, size * 0.16, size * 0.16);
        graphicsContext.fillOval(x + size * 0.55, y + size * 0.20, size * 0.14, size * 0.14);
        graphicsContext.fillOval(x + size * 0.35, y + size * 0.55, size * 0.15, size * 0.15);

        graphicsContext.setFill(COLOR_BUSH_DARK);
        graphicsContext.fillOval(x + size * 0.08, y + size * 0.58, size * 0.18, size * 0.14);
        graphicsContext.fillOval(x + size * 0.68, y + size * 0.48, size * 0.16, size * 0.14);
    }

    private void drawStumpBox(double x, double y, int row, int col) {
        double size = Constants.TILE_SIZE;

        graphicsContext.setFill(COLOR_FLOOR);
        graphicsContext.fillRect(x, y, size, size);
        drawPixelTextureDetails(x, y, row, col, COLOR_GRASS_DARK, COLOR_GRASS_LIGHT);

        int variant = Math.abs(row * 31 + col * 17) % 3;

        double stumpX;
        double stumpY;
        double stumpW;
        double stumpH;

        if (variant == 0) {
            stumpX = x + size * 0.16;
            stumpY = y + size * 0.12;
            stumpW = size * 0.68;
            stumpH = size * 0.76;
        } else if (variant == 1) {
            stumpX = x + size * 0.20;
            stumpY = y + size * 0.10;
            stumpW = size * 0.60;
            stumpH = size * 0.78;
        } else {
            stumpX = x + size * 0.13;
            stumpY = y + size * 0.15;
            stumpW = size * 0.74;
            stumpH = size * 0.70;
        }

        graphicsContext.setFill(COLOR_STUMP_DARK);
        graphicsContext.fillRoundRect(
                stumpX + size * 0.04,
                stumpY + size * 0.05,
                stumpW,
                stumpH,
                14,
                14
        );

        graphicsContext.setFill(COLOR_STUMP_BASE);
        graphicsContext.fillRoundRect(
                stumpX,
                stumpY,
                stumpW,
                stumpH,
                14,
                14
        );

        graphicsContext.setFill(COLOR_STUMP_LIGHT);
        graphicsContext.fillOval(
                stumpX + stumpW * 0.08,
                stumpY + stumpH * 0.06,
                stumpW * 0.84,
                stumpH * 0.32
        );

        graphicsContext.setStroke(COLOR_STUMP_RING);
        graphicsContext.setLineWidth(2);
        graphicsContext.strokeOval(
                stumpX + stumpW * 0.25,
                stumpY + stumpH * 0.13,
                stumpW * 0.50,
                stumpH * 0.16
        );

        graphicsContext.setStroke(COLOR_STUMP_DARK);
        graphicsContext.setLineWidth(2);

        graphicsContext.strokeLine(
                stumpX + stumpW * 0.25,
                stumpY + stumpH * 0.42,
                stumpX + stumpW * 0.22,
                stumpY + stumpH * 0.75
        );

        graphicsContext.strokeLine(
                stumpX + stumpW * 0.52,
                stumpY + stumpH * 0.40,
                stumpX + stumpW * 0.50,
                stumpY + stumpH * 0.78
        );

        graphicsContext.strokeLine(
                stumpX + stumpW * 0.75,
                stumpY + stumpH * 0.45,
                stumpX + stumpW * 0.78,
                stumpY + stumpH * 0.72
        );
    }

    // Waning wall blinking

    private void drawWarningWalls(GameStateView state) {
        double phase = (state.getRoundTime() * Constants.WARNING_BLINK_SPEED) % Constants.WARNING_BLINK_CYCLE;
        boolean blink = phase < Constants.WARNING_BLINK_VISIBLE_DURATION;

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

            double time = System.nanoTime() / 1_000_000_000.0;
            double pulse = (Math.sin(time * 6) + 1) / 2.0;

            drawPineConeBomb(bomb.getPixelX(), bomb.getPixelY(), pulse);
        }
    }

    private void drawPineConeBomb(double x, double y, double pulse) {
        double size = Constants.TILE_SIZE;
        double scale = 1.0 + pulse * 0.05;
        double offset = (size - size * scale) / 2.0;

        x = x + offset;
        y = y + offset;
        size = size * scale;

        graphicsContext.setFill(COLOR_PLAYER_SHADOW);
        graphicsContext.fillOval(
                x + size * 0.30,
                y + size * 0.80,
                size * 0.40,
                size * 0.10
        );

        graphicsContext.setFill(COLOR_PINECONE_STEM);
        graphicsContext.fillRoundRect(
                x + size * 0.46,
                y + size * 0.08,
                size * 0.08,
                size * 0.12,
                4,
                4
        );

        double[] coneX = {
                x + size * 0.50,
                x + size * 0.30,
                x + size * 0.24,
                x + size * 0.32,
                x + size * 0.50,
                x + size * 0.68,
                x + size * 0.76,
                x + size * 0.70
        };

        double[] coneY = {
                y + size * 0.18,
                y + size * 0.32,
                y + size * 0.55,
                y + size * 0.76,
                y + size * 0.84,
                y + size * 0.76,
                y + size * 0.55,
                y + size * 0.32
        };

        graphicsContext.setFill(COLOR_PINECONE_DARK);
        graphicsContext.fillPolygon(coneX, coneY, coneX.length);

        double[] innerConeX = {
                x + size * 0.50,
                x + size * 0.33,
                x + size * 0.28,
                x + size * 0.35,
                x + size * 0.50,
                x + size * 0.65,
                x + size * 0.72,
                x + size * 0.67
        };

        double[] innerConeY = {
                y + size * 0.20,
                y + size * 0.34,
                y + size * 0.55,
                y + size * 0.73,
                y + size * 0.80,
                y + size * 0.73,
                y + size * 0.55,
                y + size * 0.34
        };

        graphicsContext.setFill(COLOR_PINECONE_BASE);
        graphicsContext.fillPolygon(innerConeX, innerConeY, innerConeX.length);

        graphicsContext.setFill(COLOR_PINECONE_DARK);

        graphicsContext.fillOval(x + size * 0.43, y + size * 0.28, size * 0.14, size * 0.08);

        graphicsContext.fillOval(x + size * 0.35, y + size * 0.40, size * 0.14, size * 0.08);
        graphicsContext.fillOval(x + size * 0.52, y + size * 0.40, size * 0.14, size * 0.08);

        graphicsContext.fillOval(x + size * 0.30, y + size * 0.53, size * 0.15, size * 0.08);
        graphicsContext.fillOval(x + size * 0.44, y + size * 0.54, size * 0.15, size * 0.08);
        graphicsContext.fillOval(x + size * 0.58, y + size * 0.53, size * 0.15, size * 0.08);

        graphicsContext.fillOval(x + size * 0.36, y + size * 0.66, size * 0.15, size * 0.08);
        graphicsContext.fillOval(x + size * 0.51, y + size * 0.66, size * 0.15, size * 0.08);

        graphicsContext.setFill(COLOR_PINECONE_LIGHT);
        graphicsContext.fillOval(
                x + size * 0.39,
                y + size * 0.26,
                size * 0.10,
                size * 0.07
        );

        double sparkSize = size * (0.06 + pulse * 0.09);

        graphicsContext.setFill(COLOR_FUSE_SPARK);
        graphicsContext.fillOval(
                x + size * 0.56,
                y + size * 0.03,
                sparkSize,
                sparkSize
        );

        graphicsContext.setFill(COLOR_FUSE_SPARK_LIGHT);
        graphicsContext.fillOval(
                x + size * 0.585,
                y + size * 0.05,
                sparkSize * 0.45,
                sparkSize * 0.45
        );
    }

    private void drawExplosions(GameStateView state) {
        for (ExplosionView explosion : state.getExplosionViews()) {
            if (!explosion.isActive()) continue;

            double alpha = Math.max(0.3, explosion.getLifetime() / Constants.EXPLOSION_DURATION_SECONDS);

            drawForestExplosion(explosion.getPixelX(),explosion.getPixelY(),alpha);
        }
    }

    private void drawForestExplosion(double x, double y, double alpha) {
        double size = Constants.TILE_SIZE;

        Color outerFire = Color.rgb(210, 70, 25, alpha);
        Color middleFire = Color.rgb(255, 130, 35, alpha);
        Color innerFire = Color.rgb(255, 220, 80, alpha);
        Color woodPiece = Color.rgb(75, 40, 20, Math.max(0.75, alpha));
        Color woodPieceLight = Color.rgb(150, 90, 45, Math.max(0.75, alpha));

        graphicsContext.setFill(outerFire);
        graphicsContext.fillOval(
                x + size * 0.08,
                y + size * 0.08,
                size * 0.84,
                size * 0.84
        );

        graphicsContext.setFill(middleFire);
        graphicsContext.fillOval(
                x + size * 0.16,
                y + size * 0.16,
                size * 0.68,
                size * 0.68
        );

        graphicsContext.setFill(innerFire);
        graphicsContext.fillOval(
                x + size * 0.30,
                y + size * 0.30,
                size * 0.40,
                size * 0.40
        );

        graphicsContext.setFill(outerFire);

        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.50,
                        x + size * 0.38,
                        x + size * 0.62
                },
                new double[] {
                        y + size * 0.02,
                        y + size * 0.32,
                        y + size * 0.32
                },
                3
        );

        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.50,
                        x + size * 0.38,
                        x + size * 0.62
                },
                new double[] {
                        y + size * 0.98,
                        y + size * 0.68,
                        y + size * 0.68
                },
                3
        );

        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.02,
                        x + size * 0.32,
                        x + size * 0.32
                },
                new double[] {
                        y + size * 0.50,
                        y + size * 0.38,
                        y + size * 0.62
                },
                3
        );

        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.98,
                        x + size * 0.68,
                        x + size * 0.68
                },
                new double[] {
                        y + size * 0.50,
                        y + size * 0.38,
                        y + size * 0.62
                },
                3
        );

        graphicsContext.setFill(woodPiece);

        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.12,
                        x + size * 0.30,
                        x + size * 0.23
                },
                new double[] {
                        y + size * 0.18,
                        y + size * 0.24,
                        y + size * 0.38
                },
                3
        );

        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.70,
                        x + size * 0.88,
                        x + size * 0.76
                },
                new double[] {
                        y + size * 0.16,
                        y + size * 0.25,
                        y + size * 0.40
                },
                3
        );

        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.14,
                        x + size * 0.32,
                        x + size * 0.24
                },
                new double[] {
                        y + size * 0.78,
                        y + size * 0.70,
                        y + size * 0.58
                },
                3
        );

        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.72,
                        x + size * 0.88,
                        x + size * 0.78
                },
                new double[] {
                        y + size * 0.78,
                        y + size * 0.68,
                        y + size * 0.56
                },
                3
        );

        graphicsContext.setFill(woodPieceLight);

        graphicsContext.fillOval(
                x + size * 0.20,
                y + size * 0.46,
                size * 0.16,
                size * 0.08
        );

        graphicsContext.fillOval(
                x + size * 0.64,
                y + size * 0.46,
                size * 0.16,
                size * 0.08
        );

        graphicsContext.fillOval(
                x + size * 0.43,
                y + size * 0.16,
                size * 0.14,
                size * 0.07
        );

        graphicsContext.fillOval(
                x + size * 0.43,
                y + size * 0.76,
                size * 0.14,
                size * 0.07
        );
    }

    private void drawPlayers(GameStateView state) {
        for (PlayerView player : state.getPlayerViews()) {
            Color clothesColor;

            if (!player.isAlive()) {
                clothesColor = COLOR_PLAYER_DEAD;
            } else {
                clothesColor = PLAYER_COLORS[player.getPlayerId() % PLAYER_COLORS.length];
            }

            drawGnomePlayer(
                    player.getPixelX(),
                    player.getPixelY(),
                    Constants.TILE_SIZE,
                    clothesColor
            );
        }
    }

    private void drawGnomePlayer(double x, double y, double size, Color clothesColor) {
        graphicsContext.setFill(COLOR_PLAYER_SHADOW);
        graphicsContext.fillOval(
                x + size * 0.20,
                y + size * 0.84,
                size * 0.60,
                size * 0.11
        );

        graphicsContext.setFill(COLOR_GNOME_BOOTS);
        graphicsContext.fillRoundRect(
                x + size * 0.26,
                y + size * 0.75,
                size * 0.18,
                size * 0.12,
                4,
                4
        );
        graphicsContext.fillRoundRect(
                x + size * 0.56,
                y + size * 0.75,
                size * 0.18,
                size * 0.12,
                4,
                4
        );

        graphicsContext.setFill(clothesColor);
        graphicsContext.fillRoundRect(
                x + size * 0.20,
                y + size * 0.46,
                size * 0.60,
                size * 0.32,
                12,
                12
        );

        graphicsContext.setStroke(COLOR_PLAYER_OUTLINE);
        graphicsContext.setLineWidth(1.5);
        graphicsContext.strokeRoundRect(
                x + size * 0.20,
                y + size * 0.46,
                size * 0.60,
                size * 0.32,
                12,
                12
        );

        graphicsContext.setFill(COLOR_GNOME_SKIN);
        graphicsContext.fillOval(
                x + size * 0.30,
                y + size * 0.27,
                size * 0.40,
                size * 0.28
        );

        graphicsContext.setFill(COLOR_GNOME_BEARD);
        graphicsContext.fillOval(
                x + size * 0.27,
                y + size * 0.40,
                size * 0.46,
                size * 0.28
        );

        graphicsContext.setFill(Color.rgb(230, 170, 140));
        graphicsContext.fillOval(
                x + size * 0.45,
                y + size * 0.39,
                size * 0.10,
                size * 0.08
        );

        graphicsContext.setFill(COLOR_PLAYER_PUPIL);
        graphicsContext.fillOval(
                x + size * 0.39,
                y + size * 0.35,
                size * 0.045,
                size * 0.055
        );
        graphicsContext.fillOval(
                x + size * 0.56,
                y + size * 0.35,
                size * 0.045,
                size * 0.055
        );

        graphicsContext.setFill(clothesColor.darker());
        graphicsContext.fillRoundRect(
                x + size * 0.24,
                y + size * 0.24,
                size * 0.52,
                size * 0.08,
                7,
                7
        );

        graphicsContext.setFill(clothesColor);
        graphicsContext.fillPolygon(
                new double[] {
                        x + size * 0.50,
                        x + size * 0.22,
                        x + size * 0.78
                },
                new double[] {
                        y + size * 0.03,
                        y + size * 0.28,
                        y + size * 0.28
                },
                3
        );

        graphicsContext.setFill(Color.color(
                Math.min(clothesColor.getRed() + 0.20, 1.0),
                Math.min(clothesColor.getGreen() + 0.20, 1.0),
                Math.min(clothesColor.getBlue() + 0.20, 1.0),
                0.75
        ));
        graphicsContext.fillOval(
                x + size * 0.42,
                y + size * 0.12,
                size * 0.12,
                size * 0.08
        );
    }
}