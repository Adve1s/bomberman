package com.bomberman.bomberman.client.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which keyboard keys are currently held down,
 * and detects single key presses for one-shot actions.
 *
 * Two query modes:
 *   - isPressed(key):     true every frame the key is held (for movement)
 *   - consumePress(key):  true once per key press, then false until
 *                         the key is released and pressed again (for bombs)
 *
 * Usage:
 *   inputHandler.attachTo(scene);
 *   // each frame in the game loop:
 *   if (inputHandler.isPressed(KeyCode.W))          { player.move(UP); }
 *   if (inputHandler.consumePress(KeyCode.SPACE))   { placeBomb(); }
 */
public class InputHandler {

    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private final Set<KeyCode> justPressedKeys = new HashSet<>();

    /**
     * Hooks key listeners onto the given scene.
     * Call this once after the scene is created.
     */
    public void attachTo(Scene scene) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (pressedKeys.add(code)) {
                // First frame this key is down — mark as just pressed
                justPressedKeys.add(code);
            }
        });

        scene.setOnKeyReleased(event -> {
            pressedKeys.remove(event.getCode());
        });
    }

    /**
     * Returns true if the given key is currently held down.
     * True every frame the key is held — use for continuous actions like movement.
     */
    public boolean isPressed(KeyCode key) {
        return pressedKeys.contains(key);
    }

    /**
     * Returns true once per key press, then false until the key is
     * released and pressed again. Use for one-shot actions like placing bombs.
     */
    public boolean consumePress(KeyCode key) {
        return justPressedKeys.remove(key);
    }
}