package com.bomberman.bomberman.local;

import com.bomberman.bomberman.client.input.InputHandler;
import com.bomberman.bomberman.client.rendering.Renderer;
import com.bomberman.bomberman.server.logic.GameManager;
import com.bomberman.bomberman.shared.entity.Player;
import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.util.Direction;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

/**
 * Runs the game locally — both game logic and rendering in one process.
 *
 * This class has exactly three responsibilities:
 *   1. Run the timing loop (AnimationTimer, delta time)
 *   2. Translate JavaFX key codes into game commands
 *   3. Wire each frame: input → GameManager → Renderer
 */
public class LocalGameRunner {

    private final GameState gameState;
    private final GameManager gameManager;
    private final Renderer renderer;
    private final InputHandler inputHandler;

    private AnimationTimer gameLoop;
    private long previousNanos;

    public LocalGameRunner(Canvas canvas) {
        this.gameState = new GameState();
        this.gameManager = new GameManager();
        this.renderer = new Renderer(canvas);
        this.inputHandler = new InputHandler();

        gameManager.initializeGame(gameState);
    }

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
                gameManager.update(gameState, deltaTime);
                renderer.render(gameState);
            }
        };
        gameLoop.start();
    }

    public void stop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    // ── Input translation ──

    /**
     * Translates JavaFX key presses into game commands.
     */
    private void processInput() {
        if (gameState.getPlayers().isEmpty()) return;

        Player player = gameState.getPlayers().get(0);

        // ── Movement ──
        if (inputHandler.isPressed(KeyCode.UP) || inputHandler.isPressed(KeyCode.W)) {
            gameManager.movePlayer(gameState, player, Direction.UP);
        } else if (inputHandler.isPressed(KeyCode.DOWN) || inputHandler.isPressed(KeyCode.S)) {
            gameManager.movePlayer(gameState, player, Direction.DOWN);
        } else if (inputHandler.isPressed(KeyCode.LEFT) || inputHandler.isPressed(KeyCode.A)) {
            gameManager.movePlayer(gameState, player, Direction.LEFT);
        } else if (inputHandler.isPressed(KeyCode.RIGHT) || inputHandler.isPressed(KeyCode.D)) {
            gameManager.movePlayer(gameState, player, Direction.RIGHT);
        }

        // ── Bomb placement ──
        if (inputHandler.consumePress(KeyCode.SPACE)) {
            gameManager.placeBomb(gameState, player);
        }
    }
}