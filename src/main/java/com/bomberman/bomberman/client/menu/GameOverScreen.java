package com.bomberman.bomberman.client.menu;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class GameOverScreen {
    public static Parent create(String message, Runnable onExitClicked) {
        Label title = new Label("Game Over");
        title.setStyle("-fx-font-size: 28px;");

        Label resultLabel = new Label(message);
        resultLabel.setStyle("-fx-font-size: 18px;");

        Button exitButton = new Button("Back to Menu");
        exitButton.setOnAction(event -> onExitClicked.run());

        VBox layout = new VBox(20, title, resultLabel, exitButton);
        layout.setAlignment(Pos.CENTER);

        return layout;
    }
}
