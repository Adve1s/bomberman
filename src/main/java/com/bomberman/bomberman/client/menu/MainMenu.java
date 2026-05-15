package com.bomberman.bomberman.client.menu;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;

/**
 * The main menu shown when the app launches.
 *
 * Deliberately minimal. This file is the example template for every other
 * screen — can copy its structure when adding new screens (lobby, game over,
 * settings, etc.). The pattern is:
 *
 *   1. A {@code create(...)} static method that builds and returns a Parent.
 *   2. Callbacks passed in as functional interfaces — the screen doesn't know
 *      what "play" actually does, it just hands the values to whoever called.
 *   3. Optional initial values + error messages as plain string parameters so
 *      the same builder can be reused for "show with last input + error" on
 *      retry flows.
 *
 * Future expansions teammates can drop in here without changing the signature:
 *   - "Host" vs "Join" buttons (in-process server vs remote)
 *   - Settings (color, key bindings)
 *   - Quit button, visual polish (background image, custom fonts)
 */
public class MainMenu {

    /**
     * Builds the menu UI.
     *
     * @param onPlay        invoked when Play is clicked with (host, name)
     * @param defaultHost   pre-fill for the host field (e.g. CLI arg or last value)
     * @param defaultName   pre-fill for the name field
     * @param errorMessage  if non-null, shown above the fields in red. Use for
     *                      "couldn't reach server" on return from a failed connect.
     */
    public static Parent create(BiConsumer<String, String> onPlay,
                                String defaultHost,
                                String defaultName,
                                String errorMessage) {
        Label title = new Label("BOMBERMAN");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");

        TextField hostField = new TextField(defaultHost);
        hostField.setPromptText("Server address");
        hostField.setMaxWidth(220);

        TextField nameField = new TextField(defaultName);
        nameField.setPromptText("Your name");
        nameField.setMaxWidth(220);

        Button playButton = new Button("Play");
        playButton.setStyle("-fx-font-size: 20px; -fx-padding: 6 28;");
        playButton.setDefaultButton(true); // Enter key triggers it
        playButton.setOnAction(event -> {
            String host = hostField.getText().trim();
            String name = nameField.getText().trim();
            if (host.isEmpty() || name.isEmpty()) return; // ignore invalid input
            onPlay.accept(host, name);
        });

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().add(title);
        if (errorMessage != null) {
            Label errorLabel = new Label(errorMessage);
            errorLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 14px;");
            layout.getChildren().add(errorLabel);
        }
        layout.getChildren().addAll(hostField, nameField, playButton);
        return layout;
    }
}