package com.bomberman.bomberman.client.net;

import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.network.*;
import com.bomberman.bomberman.shared.util.Constants;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Owns the network connection to a GameServer. Sends commands; stores the latest
 * snapshot received. Has no JavaFX dependency — testable on its own.
 * Threading
 *   - KryoNet's Update thread fires received() / disconnected().
 *   - The FX thread polls getLatestSnapshot() / getMyPlayerId() / isGameStarted()
 *     once per frame from the AnimationTimer.
 *   - Callbacks (setOnGameStarted etc.) ALSO fire on the network thread. Callers
 *     that touch UI must wrap in Platform.runLater themselves.
 */
public class GameClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private final Client kryoClient;
    private final String playerName;

    /** Latest snapshot received. Written on net thread, read on FX thread. */
    private volatile GameState latestSnapshot;

    /** Our playerId after JoinAccepted; -1 until then. */
    private volatile int myPlayerId = -1;

    /** True once GameStarted arrives. */
    private volatile boolean gameStarted = false;

    // Callback hooks. volatile because written on the FX thread (during wiring)
    // and read on the network thread (when the message arrives).
    private volatile Runnable onGameStartedCallback;
    private volatile Consumer<String> onJoinRejectedCallback;



    public GameClient(String playerName) {
        this.playerName = playerName;
        this.kryoClient = new Client(Constants.NETWORK_WRITE_BUFFER_SIZE, Constants.NETWORK_OBJECT_BUFFER_SIZE);
        NetworkRegistration.register(kryoClient.getKryo());

        kryoClient.addListener(new Listener() {
            @Override public void received(Connection connection, Object object) {
                if (object instanceof NetworkMessage message) {
                    handleReceived(message);
                }
            }
            @Override public void disconnected(Connection connection) {
                handleDisconnected();
            }
        });
    }

    /**
     * Connects to the server and sends the JoinRequest. BLOCKS for up to
     * CONNECT_TIMEOUT_MS. Caller must run this off the FX thread.
     */
    public void connect(String host) throws IOException {
        kryoClient.start();
        kryoClient.connect(CONNECT_TIMEOUT_MS, host, Constants.NETWORK_PORT);
        kryoClient.sendTCP(new JoinRequest(playerName));
        System.out.println("[client] connected to " + host + ":" + Constants.NETWORK_PORT);
    }

    public void disconnect() {
        kryoClient.stop();
    }

    public void send(ClientToServerMessage message) {
        kryoClient.sendTCP(message);
    }

    // Polled by the FX thread each frame
    public GameState getLatestSnapshot() { return latestSnapshot; }
    public int       getMyPlayerId()     { return myPlayerId; }
    public  boolean  isGameStarted() { return gameStarted; }

    // Callback registration

    /**
     * Sets a callback to run when the server sends GameStarted.
     * Threading: the callback fires on the network thread, not the FX
     * thread. If it touches the UI, wrap the body in {@code Platform.runLater}.
     */
    public void setOnGameStarted(Runnable callback) {
        this.onGameStartedCallback = callback;
    }

    /**
     * Sets a callback to run when the server rejects a join request.
     * The callback receives the rejection reason.
     * Threading: the callback fires on the network thread, not the FX
     * thread. If it touches the UI, wrap the body in {@code Platform.runLater}.
     */
    public void setOnJoinRejected(Consumer<String> callback) { this.onJoinRejectedCallback = callback; }


    // Network thread

    private void handleReceived(NetworkMessage message) {
        switch (message) {
            // Hot path — every server tick.
            case StateSnapshot gameState -> latestSnapshot = gameState.getState();

            case JoinAccepted joinAccepted -> {
                myPlayerId = joinAccepted.getPlayerId();
                System.out.println("[client] joined as player " + myPlayerId);
            }

            case JoinRejected joinRejected -> {
                Consumer<String> cb = this.onJoinRejectedCallback;
                if (cb != null) cb.accept(joinRejected.getReason());
                System.err.println("[client] join rejected: " + joinRejected.getReason());
            }

            case GameStarted gameStarted -> {
                this.gameStarted = true;
                System.out.println("[client] game started");
                Runnable callback = this.onGameStartedCallback;
                if (callback != null) callback.run();
            }

            case GameOver gameOver -> {
                // TODO (teammate A): onGameOver callback for the win/lose screen with exit option.
                if (gameOver.isDraw()) {
                    System.out.println("[client] game over: draw");
                } else {
                    System.out.println("[client] game over: player " + gameOver.getWinnerPlayerId() + " wins");
                }
            }

            case LobbyState lobbyState -> {
                // TODO (teammate A): onLobbyState callback to refresh the lobby UI.
                //
                System.out.println("[client] LobbyState received (TODO teammate A)");
            }

            case PlayerLeft playerLeft ->
            {
                // TODO (teammate B): onPlayerLeft callback for in-game notification —
                // "Alice disconnected" overlay, etc.
                System.out.println("[client] player " + playerLeft.getPlayerId() + " left");
            }

            case Pong pong -> {
                // TODO (teammate B): latency = System.currentTimeMillis() - pong.getClientTimestamp();
                System.out.println("[client] Pong received (TODO teammate B)");
            }

            default -> { /* other message types — ignore */ }
        }
    }

    private void handleDisconnected() {
        // TODO (teammate B): onDisconnected callback to show "lost connection" overlay.
        System.out.println("[client] disconnected from server (TODO teammate B)");
    }
}