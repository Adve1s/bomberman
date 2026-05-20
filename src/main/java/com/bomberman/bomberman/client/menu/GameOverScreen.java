package com.bomberman.bomberman.client.menu;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * End-of-match screen shown when the server sends a {@link com.bomberman.bomberman.shared.network.GameOver}.
 * Displays the match result (win, loss, or draw) and allows the player to
 * return to the main menu.
 */
public class GameOverScreen {

    public static Parent create(String message, Runnable onExitClicked) {
        Label title = new Label("BOMBERMAN");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");

        Label subtitle = new Label("Game Over");
        subtitle.setStyle("-fx-font-size: 24px;");

        Label resultLabel = new Label(message);
        resultLabel.setStyle("-fx-font-size: 18px;");

        Button exitButton = new Button("Back to Menu");
        exitButton.setStyle("-fx-font-size: 18px; -fx-padding: 6 24;");
        exitButton.setOnAction(event -> onExitClicked.run());

        VBox layout = new VBox(20, title, subtitle, resultLabel, exitButton);
        layout.setAlignment(Pos.CENTER);

        return layout;
    }
}
