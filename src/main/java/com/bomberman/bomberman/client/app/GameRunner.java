package com.bomberman.bomberman.client.app;

import javafx.scene.Scene;

/**
 * Contract for anything that can run the game.
 *
 * GameApp depends on this interface, not on a concrete runner.
 * Implementations:
 *   - LocalGameRunner: runs GameState locally, both "server" and "client" in one process.
 *   - (future) NetworkGameRunner: receives state from a remote server, only renders + sends input.
 */
public interface GameRunner {

    /**
     * Attach input listeners and start the game loop.
     * Called once after the stage is shown.
     */
    void start(Scene scene);

    /**
     * Stop the game loop and clean up.
     */
    void stop();
}