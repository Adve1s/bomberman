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
import com.bomberman.bomberman.client.hud.GameHud;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.List;

/**
 * JavaFX entry point. Owns the Stage + a single Scene whose root is swapped
 * to transition between screens.
 * On connect failure, returns to MainMenu with an error label and the user's
 * last input preserved so they can fix a typo and retry.
 *
 * Host vs Join
 *   - Host: starts an in-process GameServer, then connects to localhost.
 *     The server runs in the same JVM; it shuts down when the window closes.
 *   - Join: just connects to whatever the user typed in the host field.
 *
 * Launch modes
 *   - Default: client (shows menu with Host/Join buttons).
 *   - {@code --server}: headless dedicated GameServer, no JavaFX window.
 *
 * CLI args (client mode)
 *   - args[0] → pre-fills the host field (default "")
 *   - args[1] → pre-fills the name field (default "")
 */
public class GameApp extends Application {

    private Stage stage;
    private Scene scene;
    private GameClient client;
    private NetworkedGameRunner runner;
    private GameServer inProcessServer; // non-null only when user chose Host

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(
                MainMenu.create(this::joinGame, this::hostGame, defaultHost(), defaultName(), null),
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
        if (runner != null)          { runner.stop();            runner = null; }
        if (client != null)          { client.disconnect();      client = null; }
        if (inProcessServer != null) { inProcessServer.stop();   inProcessServer = null; }
    }

    /**
     * Host button handler. Starts an in-process server, then connects to it
     * as a regular client at localhost. If the server fails to start (e.g.
     * port already in use), returns to the menu with an error message.
     */
    private void hostGame(String name) {
        try {
            inProcessServer = new GameServer();
            inProcessServer.start();
        } catch (IOException ex) {
            System.err.println("[client] couldn't start in-process server: " + ex.getMessage());
            inProcessServer = null;
            returnToMenu("", name, "Couldn't start server: " + ex.getMessage());
            return;
        }
        joinGame("localhost", name);
    }

    /**
     * Join button handler — and the entry point for the hosted path too
     * (hostGame falls through to this once the server is running).
     *
     * Demonstrates the chain teammates will reuse for lobby actions:
     *   1. Swap to a transitional screen
     *   2. Wire the callback that handles the server's response
     *   3. Kick off the network call on a background thread
     */
    private void joinGame(String host, String name) {
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
        Parent hud = com.bomberman.bomberman.client.hud.GameHud.create(client);
        scene.setRoot(new StackPane(canvas, hud)); // hud stacks above the canvas

        runner = new NetworkedGameRunner(canvas, client);
        runner.start(scene);
    }

    /**
     * Returns to the menu with the user's last input preserved and an optional
     * error message displayed. Tears down anything we started — runner, client,
     * AND the in-process server if Host was the path that failed.
     */
    private void returnToMenu(String lastHost, String lastName, String errorMessage) {
        shutdown();
        stage.setTitle("Bomberman");
        scene.setRoot(MainMenu.create(this::joinGame, this::hostGame, lastHost, lastName, errorMessage));
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