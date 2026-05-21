package com.bomberman.bomberman.client.net;

import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.network.*;
import com.bomberman.bomberman.shared.util.Constants;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;

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

    private volatile long latencyMs = -1;
    private java.util.concurrent.ScheduledExecutorService pingExecutor;
    private volatile java.util.function.Consumer<Integer> onPlayerLeftCallback;
    private volatile Runnable onDisconnectedCallback;

    // Callback hooks. volatile because written on the FX thread (during wiring)
    // and read on the network thread (when the message arrives).
    private volatile Runnable onGameStartedCallback;

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
        pingExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ping-sender");
            t.setDaemon(true);
            return t;
        });
        pingExecutor.scheduleAtFixedRate(() -> {
            try {
                if (kryoClient.isConnected()) {
                    kryoClient.sendTCP(new Ping(System.currentTimeMillis()));
                } else {
                    var ex = pingExecutor;
                    if (ex != null) ex.shutdown();
                }
            } catch (Exception ignored) {}
        }, 1000, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        System.out.println("[client] connected to " + host + ":" + Constants.NETWORK_PORT);
    }

    public void disconnect() {
        kryoClient.stop();
        var ex = this.pingExecutor;
        if (ex != null) {
            ex.shutdownNow();
            this.pingExecutor = null;
        }
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

    // Network thread
    /** * Returns the latest measured round-trip latency in milliseconds. * Volatile long is written on the network thread and read on the FX thread. */
    public long getLatencyMs() {
        return latencyMs;
    }

    /** * Register a callback that runs when another player leaves. * The callback is invoked on the network thread and receives the playerId. * Callers that touch JavaFX must wrap their UI work in Platform.runLater. */
    public void setOnPlayerLeft(java.util.function.Consumer<Integer> cb) {
        this.onPlayerLeftCallback = cb;
    }

    /** * Register a callback that runs when this client disconnects from the server. * The callback is invoked on the network thread. UI work should be wrapped * with Platform.runLater by the caller. */
    public void setOnDisconnected(Runnable cb) {
        this.onDisconnectedCallback = cb;
    }

    private void handleReceived(NetworkMessage message) {
        switch (message) {
            // Hot path — every server tick.
            case StateSnapshot gameState -> latestSnapshot = gameState.getState();

            case JoinAccepted joinAccepted -> {
                myPlayerId = joinAccepted.getPlayerId();
                System.out.println("[client] joined as player " + myPlayerId);
            }

            case JoinRejected joinRejected -> {
                // TODO (teammate A): fire an onJoinRejected callback. Pattern:
                // declare:
                //   private volatile Consumer<String> onJoinRejectedCallback;
                // Setter:
                //   public void setOnJoinRejected(Consumer<String> cb) { this.onJoinRejectedCallback = cb; }
                // And here:
                //     Consumer<String> cb = this.onJoinRejectedCallback;
                //     if (cb != null) cb.accept(joinRejected.getReason());
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
                System.out.println("[client] LobbyState received (TODO teammate A)");
            }

            case PlayerLeft playerLeft ->
            {
                System.out.println("[client] player " + playerLeft.getPlayerId() + " left");
                java.util.function.Consumer<Integer> cb = this.onPlayerLeftCallback;
                if (cb != null) cb.accept(playerLeft.getPlayerId());
            }

            case Pong pong -> {
                latencyMs = System.currentTimeMillis() - pong.getClientTimestamp();
                System.out.println("[client] Pong received, latency=" + latencyMs + "ms");
            }

            default -> { /* other message types — ignore */ }
        }
    }

    private void handleDisconnected() {
        System.out.println("[client] disconnected from server");
        Runnable cb = this.onDisconnectedCallback;
        if (cb != null) cb.run();
    }
}