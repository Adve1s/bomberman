package com.bomberman.bomberman.client.net;

import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.network.*;
import com.bomberman.bomberman.shared.util.Constants;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;

/**
 * Owns the network connection to a GameServer. Sends commands;
 * Threading:
 *   - KryoNet's Update thread fires received() / disconnected().
 *   - The FX thread polls getLatestSnapshot() / getMyPlayerId() / isGameStarted()
 *     once per frame from the AnimationTimer.
 *   - All "latest received" fields are volatile because they cross threads.
 *     Pattern: net thread writes, FX thread reads.
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

    public GameClient(String playerName) {
        this.playerName = playerName;
        this.kryoClient = new Client(Constants.NETWORK_WRITE_BUFFER_SIZE, Constants.NETWORK_OBJECT_BUFFER_SIZE);
        NetworkRegistration.register(kryoClient.getKryo());

        kryoClient.addListener(new Listener() {
            @Override public void received(Connection connection, Object object) {
                // KryoNet fires received() for its internal framework messages too
                // (keepalives, etc). Filter to our types only.
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

    // Queries polled by the FX thread each frame

    public GameState getLatestSnapshot() { return latestSnapshot; }
    public int       getMyPlayerId()     { return myPlayerId; }
    public boolean   isGameStarted()     { return gameStarted; }

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
                // TODO (teammate A): show rejection overlay with reason.
                System.err.println("[client] join rejected: " + joinRejected.getReason());
            }

            case GameStarted gameStarted -> {
                this.gameStarted = true;
                System.out.println("[client] game started");
            }

            case GameOver gameOver -> {
                // TODO (teammate A/C): show win/lose screen.
                if (gameOver.isDraw()) {
                    System.out.println("[client] game over: draw");
                } else {
                    System.out.println("[client] game over: player " + gameOver.getWinnerPlayerId() + " wins");
                }
            }

            case LobbyState lobbyState -> {
                // TODO (teammate A): render lobby UI from this.
                System.out.println("[client] LobbyState received (TODO teammate A)");
            }

            case PlayerLeft playerLeft -> System.out.println("[client] player " + playerLeft.getPlayerId() + " left");

            case Pong pong -> {
                // TODO (teammate B): latency = System.currentTimeMillis() - pong.getClientTimestamp();
                System.out.println("[client] Pong received (TODO teammate B)");
            }

            default -> { /* other message types — ignore */ }
        }
    }

    private void handleDisconnected() {
        // TODO (teammate B): show "disconnected" overlay, offer reconnect.
        System.out.println("[client] disconnected from server (TODO teammate B)");
    }
}