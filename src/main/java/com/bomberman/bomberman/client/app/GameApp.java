package com.bomberman.bomberman.client.app;

import com.bomberman.bomberman.client.menu.ConnectingScreen;
import com.bomberman.bomberman.client.menu.MainMenu;
import com.bomberman.bomberman.client.net.GameClient;
import com.bomberman.bomberman.client.runner.NetworkedGameRunner;
import com.bomberman.bomberman.server.net.GameServer;
import com.bomberman.bomberman.shared.util.Constants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * JavaFX entry point. Owns the Stage + a single Scene whose root is swapped
 * to transition between screens.
 * On connect failure, returns to MainMenu with an error label and the user's
 * last input preserved so they can fix a typo and retry.
 * Launch modes
 *   - Default: client (shows menu).
 *   - {@code --server}: headless GameServer, no JavaFX window.
 *
 * CLI args (client mode)
 *   - args[0] → pre-fills the host field (default "localhost")
 *   - args[1] → pre-fills the name field (default "Player")
 */
public class GameApp extends Application {

    private Stage stage;
    private Scene scene; // single Scene, root is swapped per screen
    private GameClient client;
    private NetworkedGameRunner runner;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(
                MainMenu.create(this::launchGame, defaultHost(), defaultName(), null),
                Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT
        );

        stage.setTitle("Bomberman");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(event -> shutdown());
        stage.show();
    }

    @Override
    public void stop() {
        shutdown();
    }

    private void shutdown() {
        if (runner != null) runner.stop();
        if (client != null) client.disconnect();
    }

    /**
     * Called by MainMenu when Play is clicked. Demonstrates the chain teammates
     * will reuse for lobby actions:
     *   1. Swap to a transitional screen
     *   2. Wire the callback that handles the server's response
     *   3. Kick off the network call on a background thread
     */
    private void launchGame(String host, String name) {
        stage.setTitle("Bomberman — " + name + " → " + host);

        // (1) transitional screen
        scene.setRoot(ConnectingScreen.create(host));

        // (2) callback fires on the network thread — wrap UI work in Platform.runLater
        client = new GameClient(name);
        client.setOnGameStarted(() -> Platform.runLater(this::switchToGameScene));

        // (3) connect off the FX thread so the 5s timeout doesn't freeze the window
        Thread connectThread = new Thread(() -> {
            try {
                client.connect(host);
            } catch (IOException ex) {
                System.err.println("[client] connect failed: " + ex.getMessage());
                Platform.runLater(() -> returnToMenu(host, name,
                        "Couldn't reach " + host + " — " + ex.getMessage()));
            }
        }, "client-connect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void switchToGameScene() {
        Canvas canvas = new Canvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        scene.setRoot(new StackPane(canvas));

        runner = new NetworkedGameRunner(canvas, client);
        runner.start(scene);
    }

    /**
     * Returns to the menu with the user's last input preserved and an optional
     * error message displayed. Called on connect failure, and a good hook for
     * future "kicked from lobby" / "disconnected" flows.
     */
    private void returnToMenu(String lastHost, String lastName, String errorMessage) {
        if (runner != null) { runner.stop();       runner = null; }
        if (client != null) { client.disconnect(); client = null; }
        stage.setTitle("Bomberman");
        scene.setRoot(MainMenu.create(this::launchGame, lastHost, lastName, errorMessage));
    }

    // Defaults / CLI args

    private String defaultHost() {
        List<String> args = getParameters().getRaw();
        return args.isEmpty() ? "" : args.getFirst();
    }

    private String defaultName() {
        List<String> args = getParameters().getRaw();
        return args.size() < 2 ? "" : args.get(1);
    }

    // Entry point

    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            if (arg.equals("--server")) {
                GameServer.main(new String[0]);
                return;
            }
        }
        launch(args);
    }
}