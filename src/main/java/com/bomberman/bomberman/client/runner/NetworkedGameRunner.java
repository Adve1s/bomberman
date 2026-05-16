package com.bomberman.bomberman.client.runner;

import com.bomberman.bomberman.client.input.InputHandler;
import com.bomberman.bomberman.client.net.GameClient;
import com.bomberman.bomberman.client.rendering.Renderer;
import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.network.MoveCommand;
import com.bomberman.bomberman.shared.network.PlaceBombCommand;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

/**
 * Per-frame client loop:
 *   1. Translate input → commands sent via GameClient
 *   2. Render the latest snapshot GameClient has received
 * The client has no game logic. The server is authoritative; we render
 * whatever it last told us and ship inputs upstream.
 */
public class NetworkedGameRunner {

    private final GameClient client;
    private final Renderer renderer;
    private final InputHandler inputHandler;

    private AnimationTimer animationTimer;

    public NetworkedGameRunner(Canvas canvas, GameClient client) {
        this.client = client;
        this.renderer = new Renderer(canvas);
        this.inputHandler = new InputHandler();
    }

    public void start(Scene scene) {
        inputHandler.attachTo(scene);
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long nowNanos) {
                sendInputs();
                renderLatestSnapshot();
            }
        };
        animationTimer.start();
    }

    public void stop() {
        if (animationTimer != null) animationTimer.stop();
    }

    private void sendInputs() {
        // Gate on playerId
        if (client.getMyPlayerId() == -1) return;

        // Movement: collect all currently-held directions into a dx/dy pair.
        // Server normalizes diagonal speed and slides along walls per-axis.
        int dx = 0, dy = 0;
        if (inputHandler.isPressed(KeyCode.LEFT)  || inputHandler.isPressed(KeyCode.A)) dx -= 1;
        if (inputHandler.isPressed(KeyCode.RIGHT) || inputHandler.isPressed(KeyCode.D)) dx += 1;
        if (inputHandler.isPressed(KeyCode.UP)    || inputHandler.isPressed(KeyCode.W)) dy -= 1;
        if (inputHandler.isPressed(KeyCode.DOWN)  || inputHandler.isPressed(KeyCode.S)) dy += 1;

        // Send only when there's actual intent. No keys held = no message = no
        // move (the server reads-and-clears pendingMove each tick, so silence
        // means "stop").
        if (dx != 0 || dy != 0) {
            client.send(new MoveCommand(dx, dy));
        }

        // One-shot: fires exactly once per SPACE press, not per frame held.
        if (inputHandler.consumePress(KeyCode.SPACE)) {
            client.send(new PlaceBombCommand());
        }
    }

    private void renderLatestSnapshot() {
        GameState snapshot = client.getLatestSnapshot();
        if (snapshot != null) {
            renderer.render(snapshot);
        }
    }
}