package com.bomberman.bomberman.local;

import com.bomberman.bomberman.client.app.GameRunner;
import com.bomberman.bomberman.client.input.InputHandler;
import com.bomberman.bomberman.client.rendering.Renderer;
import com.bomberman.bomberman.shared.entity.Player;
import com.bomberman.bomberman.shared.entity.Bomb;
import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.util.Direction;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

/**
 * Runs the game locally — both game logic and rendering in one process.
 * <p>
 * Owns the GameState, Renderer, and InputHandler.
 * Game loop each frame:
 * 1. Read input and tell entities what to do
 * 2. Update game state (entities tick their timers, cooldowns, etc.)
 * 3. Render
 * <p>
 * This class translates key presses into game actions.
 * It does NOT calculate positions or handle physics — that's the entities' job.
 */
public class LocalGameRunner implements GameRunner {

    private final GameState gameState;
    private final Renderer renderer;
    private final InputHandler inputHandler;

    private AnimationTimer gameLoop;
    private long previousNanos;

    public LocalGameRunner(Canvas canvas) {
        this.gameState = new GameState();
        this.renderer = new Renderer(canvas);
        this.inputHandler = new InputHandler();

        setupPlayers();
    }

    @Override
    public void start(Scene scene) {
        inputHandler.attachTo(scene);

        previousNanos = System.nanoTime();

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long nowNanos) {
                double deltaTime = (nowNanos - previousNanos) / 1_000_000_000.0;
                previousNanos = nowNanos;

                // Cap delta so a long pause doesn't cause huge jumps
                if (deltaTime > 0.1) deltaTime = 0.1;

                processInput();
                gameState.update(deltaTime);
                renderer.render(gameState);
            }
        };
        gameLoop.start();
    }

    @Override
    public void stop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    // ── Setup ──

    private void setupPlayers() {
        Player player1 = new Player(0, 1, 1);
        gameState.addPlayer(player1);
    }

    // ── Input processing ──

    /**
     * Translates key presses into game actions.
     * The player's move() handles cooldowns and validation internally,
     * so this just says "player wants to go UP" and the player decides
     * whether that actually happens.
     */
    private void processInput() {
        if (gameState.getPlayers().isEmpty()) return;

        Player player = gameState.getPlayers().get(0);
        if (!player.isAlive()) return;

        if (inputHandler.isPressed(KeyCode.UP) || inputHandler.isPressed(KeyCode.W)) {
            player.move(Direction.UP);
        } else if (inputHandler.isPressed(KeyCode.DOWN) || inputHandler.isPressed(KeyCode.S)) {
            player.move(Direction.DOWN);
        } else if (inputHandler.isPressed(KeyCode.LEFT) || inputHandler.isPressed(KeyCode.A)) {
            player.move(Direction.LEFT);
        } else if (inputHandler.isPressed(KeyCode.RIGHT) || inputHandler.isPressed(KeyCode.D)) {
            player.move(Direction.RIGHT);
        }
        // ── Bomb placement ──
        if (inputHandler.consumePress(KeyCode.SPACE)) {
            placeBomb(player);
        }
    }

    /**
     * Places a bomb at the player's current grid position.
     * Respects the player's bomb limit and prevents stacking bombs on the same tile.
     */
    private void placeBomb(Player player) {
        if (!player.canPlaceBomb()) return;

        int row = player.getRow();
        int col = player.getCol();

        // Check if a bomb already exists on this tile
        boolean tileOccupied = gameState.getBombs().stream()
                .anyMatch(bomb -> bomb.isActive() && bomb.getRow() == row && bomb.getCol() == col);
        if (tileOccupied) return;

        Bomb bomb = new Bomb(
                player.getPlayerId(),
                player.getRow(),
                player.getCol(),
                player.getExplosionRange()
        );
        gameState.queueBomb(bomb);
        player.bombPlaced();
    }
}