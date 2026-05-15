package com.bomberman.bomberman.client.menu;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The main menu shown when the app launches. Two buttons:
 *   - Host: start an in-process server, then connect to it (uses localhost,
 *           ignoring the host field).
 *   - Join: connect to the server address in the host field.
 *
 * Both require a name. Join also requires a non-empty host field.
 *
 * Template for future screens — copy this structure for lobby, game over, etc.:
 *   1. Static {@code create(...)} returning a Parent.
 *   2. Callbacks as functional interface parameters — the screen doesn't know
 *      what the actions actually do, just signals them.
 *   3. Optional pre-fill values and error message for retry flows.
 */
public class MainMenu {

    /**
     * Builds the menu UI.
     *
     * @param onJoin        invoked when Join is clicked, with (host, name)
     * @param onHost        invoked when Host is clicked, with (name) — the server
     *                      is always localhost so no host string is passed
     * @param defaultHost   pre-fill for the host field (empty string for blank)
     * @param defaultName   pre-fill for the name field (empty string for blank)
     * @param errorMessage  if non-null, shown above the fields in red. Use for
     *                      "couldn't reach server" on return from a failed connect.
     */
    public static Parent create(BiConsumer<String, String> onJoin,
                                Consumer<String> onHost,
                                String defaultHost,
                                String defaultName,
                                String errorMessage) {
        Label title = new Label("BOMBERMAN");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");

        TextField hostField = new TextField(defaultHost);
        hostField.setPromptText("Server address (for Join)");
        hostField.setMaxWidth(240);

        TextField nameField = new TextField(defaultName);
        nameField.setPromptText("Your name");
        nameField.setMaxWidth(240);

        Button hostButton = new Button("Host");
        hostButton.setStyle("-fx-font-size: 18px; -fx-padding: 6 24;");
        hostButton.setOnAction(event -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return;
            onHost.accept(name);
        });

        Button joinButton = new Button("Join");
        joinButton.setStyle("-fx-font-size: 18px; -fx-padding: 6 24;");
        joinButton.setDefaultButton(true); // Enter triggers Join
        joinButton.setOnAction(event -> {
            String host = hostField.getText().trim();
            String name = nameField.getText().trim();
            if (host.isEmpty() || name.isEmpty()) return;
            onJoin.accept(host, name);
        });

        HBox buttons = new HBox(20, hostButton, joinButton);
        buttons.setAlignment(Pos.CENTER);

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().add(title);
        if (errorMessage != null) {
            Label errorLabel = new Label(errorMessage);
            errorLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 14px;");
            layout.getChildren().add(errorLabel);
        }
        layout.getChildren().addAll(hostField, nameField, buttons);
        return layout;
    }
}