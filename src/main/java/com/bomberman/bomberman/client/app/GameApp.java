package com.bomberman.bomberman.client.app;

import com.bomberman.bomberman.client.net.GameClient;
import com.bomberman.bomberman.client.runner.NetworkedGameRunner;
import com.bomberman.bomberman.server.net.GameServer;
import com.bomberman.bomberman.shared.util.Constants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * JavaFX entry point.
 * Launch modes:
 *   - Default: client. Prompts for server address + name, connects, plays.
 *   - {@code --server}: runs the headless GameServer, no JavaFX window.
 * CLI args (client mode):
 *   - args[0] → server host (skips the host dialog)
 *   - args[1] → player name (skips the name dialog)
 *
 * Phase 5 (teammate A): replace the prompts with a real main menu.
 */
public class GameApp extends Application {

    private GameClient client;
    private NetworkedGameRunner runner;

    @Override
    public void start(Stage stage) {
        String serverHost = resolveServerHost();
        if (serverHost == null) { Platform.exit(); return; }

        String playerName = resolvePlayerName();
        if (playerName == null) { Platform.exit(); return; }

        Canvas canvas = new Canvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        stage.setTitle("Bomberman — " + playerName + " - " + serverHost);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        client = new GameClient(playerName);
        runner = new NetworkedGameRunner(canvas, client);
        runner.start(scene);

        // Connect on a background thread so the FX thread isn't frozen during
        // the (up-to-5-second) blocking connect. Without this, a typo host or
        // a down server leaves the user staring at a frozen window.
        Thread connectThread = new Thread(() -> {
            try {
                client.connect(serverHost);
            } catch (IOException e) {
                // TODO (teammate A): replace with an in-window error overlay + retry button.
                System.err.println("[client] failed to connect to " + serverHost + ": " + e.getMessage());
                Platform.runLater(Platform::exit);
            }
        }, "client-connect");
        // Daemon so JVM shutdown isn't blocked if connect is still pending when
        // the user closes the window.
        connectThread.setDaemon(true);
        connectThread.start();

        stage.setOnCloseRequest(event -> shutdown());
    }

    @Override
    public void stop() {
        shutdown();
    }

    private void shutdown() {
        if (runner != null) runner.stop();
        if (client != null) client.disconnect();
    }

    /** Server address from args[0], or prompted via dialog. */
    private String resolveServerHost() {
        List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) return args.getFirst();

        TextInputDialog dialog = new TextInputDialog("localhost");
        dialog.setTitle("Bomberman");
        dialog.setHeaderText("Connect to server");
        dialog.setContentText("Server address:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /** Player name from args[1], or prompted via dialog. */
    private String resolvePlayerName() {
        List<String> args = getParameters().getRaw();
        if (args.size() >= 2) return args.get(1);

        TextInputDialog dialog = new TextInputDialog("Player");
        dialog.setTitle("Bomberman");
        dialog.setHeaderText("Choose a name");
        dialog.setContentText("Your name:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public static void main(String[] args) throws IOException {
        // --server anywhere in args → dedicated server mode, no JavaFX window.
        for (String arg : args) {
            if (arg.equals("--server")) {
                GameServer.main(new String[0]);
                return;
            }
        }
        launch(args);
    }
}