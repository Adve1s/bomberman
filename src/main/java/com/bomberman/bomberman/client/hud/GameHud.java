package com.bomberman.bomberman.client.hud;

import com.bomberman.bomberman.client.net.GameClient;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;


public class GameHud {

    public static Parent create(GameClient client) {
        // Ping label: top-right
        Label pingLabel = new Label("Ping: -- ms");
        pingLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; -fx-font-family: monospace; -fx-padding: 4 8; -fx-font-size: 14px;");

        // Toast container: top-left
        VBox toastBox = new VBox(6);
        toastBox.setMouseTransparent(true); // clicks pass through to the canvas
        toastBox.setPickOnBounds(false);

        // Root overlay that will be stacked above the game canvas.
        StackPane root = new StackPane(pingLabel, toastBox);
        StackPane.setAlignment(pingLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(pingLabel, new Insets(8));
        StackPane.setAlignment(toastBox, Pos.TOP_LEFT);
        StackPane.setMargin(toastBox, new Insets(8));

        // Timeline that updates the ping label periodically (not every frame).
        Timeline pingUpdater = new Timeline(new KeyFrame(Duration.millis(500), ev -> {
            long lat = client.getLatencyMs();
            String text = (lat < 0) ? "Ping: -- ms" : ("Ping: " + lat + " ms");
            pingLabel.setText(text);
        }));
        pingUpdater.setCycleCount(Timeline.INDEFINITE);
        pingUpdater.play();

        // Register callbacks on the client. GameClient invokes these on the network thread,
        // so wrap UI updates in Platform.runLater.
        client.setOnPlayerLeft(id -> Platform.runLater(() -> {
            Label toast = new Label("Player " + id + " disconnected");
            toast.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; -fx-padding: 4 8;");
            toastBox.getChildren().add(toast);

            // Auto-remove after ~3s
            PauseTransition pt = new PauseTransition(Duration.seconds(3));
            pt.setOnFinished(a -> toastBox.getChildren().remove(toast));
            pt.play();
        }));

        client.setOnDisconnected(() -> Platform.runLater(() -> {
            Label toast = new Label("Disconnected from server");
            toast.setStyle("-fx-background-color: rgba(128,0,0,0.85); -fx-text-fill: white; -fx-padding: 6 10;");
            toastBox.getChildren().add(toast);
            // Keep this persistent or remove after a longer delay; here we leave it persistent.
        }));

        return root;
    }
}