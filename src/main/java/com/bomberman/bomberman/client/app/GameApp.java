package com.bomberman.bomberman.client.app;

import com.bomberman.bomberman.local.LocalGameRunner;
import com.bomberman.bomberman.shared.util.Constants;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX entry point. Creates the window and canvas.
 */
public class GameApp extends Application {

    private LocalGameRunner runner;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        stage.setTitle("Bomberman");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        runner = new LocalGameRunner(canvas);

        runner.start(scene);

        stage.setOnCloseRequest(event -> runner.stop());
    }

    @Override
    public void stop() {
        if (runner != null) {
            runner.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}