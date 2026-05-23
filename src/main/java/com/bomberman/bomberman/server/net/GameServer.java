package com.bomberman.bomberman.server.net;

import com.bomberman.bomberman.server.logic.GameManager;
import com.bomberman.bomberman.shared.entity.Player;
import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.network.*;
import com.bomberman.bomberman.shared.util.Constants;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Authoritative game server.
 * Threading model:
 *   - KryoNet's Update thread fires connected / disconnected / received.
 *   - A single-threaded ScheduledExecutorService ("game-tick") runs the tick loop.
 *   - GameState is mutated ONLY on the tick thread. The network thread queues
 *     mutations (player joins) into pendingActions; the tick thread drains them
 *     at the top of each tick.
 *   - Per-session input state uses concurrent primitives (volatile, AtomicInteger)
 *     because it's written on the network thread and read on the tick thread.
 */
public class GameServer {

    // Config

    private static final int TICK_HZ = 30;
    private static final long TICK_MS = 1000 / TICK_HZ;
    private static final int MAX_PLAYERS = 4;

    // Spawn corners by playerId. GameMap.clearSpawnZones() keeps these reachable.
    private static final int[][] SPAWN_POSITIONS = {
            {1, 1},                                                       // P0 top-left
            {1, Constants.GRID_COLS - 2},                                 // P1 top-right
            {Constants.GRID_ROWS - 2, 1},                                 // P2 bottom-left
            {Constants.GRID_ROWS - 2, Constants.GRID_COLS - 2}            // P3 bottom-right
    };

    // Services

    private final Server kryoServer;
    private final ScheduledExecutorService tickExecutor;

    // State (tick thread only)

    private final GameState state;
    private final GameManager gameManager;
    private boolean gameStarted = false;
    private long previousTickNanos;

    // Cross-thread

    /** ConnectionId → session. Network thread inserts/removes; tick thread iterates. */
    private final Map<Integer, PlayerSession> sessionsByConnection = new ConcurrentHashMap<>();

    /** Network thread enqueues GameState mutations here for the tick thread to apply. */
    private final Queue<Runnable> pendingActions = new ConcurrentLinkedQueue<>();

    // Lifecycle

    public GameServer() {
        this.state = new GameState();
        this.gameManager = new GameManager();
        this.kryoServer = new Server(Constants.NETWORK_WRITE_BUFFER_SIZE, Constants.NETWORK_OBJECT_BUFFER_SIZE);
        this.tickExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "game-tick");
            thread.setDaemon(false); // keep JVM alive
            return thread;
        });

        NetworkRegistration.register(kryoServer.getKryo());

        kryoServer.addListener(new Listener() {
            @Override public void connected(Connection connection)    { handleConnected(connection); }
            @Override public void disconnected(Connection connection) { handleDisconnected(connection); }
            @Override public void received(Connection connection, Object object) {
                // KryoNet fires received() for its internal framework messages too
                // (keepalives, etc). Filter to our types only.
                if (object instanceof NetworkMessage message) {
                    handleReceived(connection, message);
                }
            }
        });
    }

    public void start() throws IOException {
        kryoServer.start();
        try {
            kryoServer.bind(Constants.NETWORK_PORT);
        } catch (IOException e) {
            kryoServer.stop();
            throw e;
        }
        previousTickNanos = System.nanoTime();
        tickExecutor.scheduleAtFixedRate(this::tick, 0, TICK_MS, TimeUnit.MILLISECONDS);
        System.out.println("[server] listening on port " + Constants.NETWORK_PORT);
    }

    public void stop() {
        tickExecutor.shutdownNow();
        kryoServer.stop();
        System.out.println("[server] stopped");
    }

    // Network thread

    private void handleConnected(Connection connection) {
        // Don't allow late joiners once the match has started.
        if (gameStarted) {
            connection.sendTCP(new JoinRejected("Game already started"));
            connection.close();
            System.out.println("[server] rejected " + connection.getID() + " — game already started");
            return;
        }

        int playerId = nextAvailablePlayerId();
        if (playerId < 0) {
            connection.sendTCP(new JoinRejected("Server is full (" + MAX_PLAYERS + " max)"));
            connection.close();
            System.out.println("[server] rejected " + connection.getID() + " — server full");
            return;
        }

        PlayerSession session = new PlayerSession(playerId, connection);
        sessionsByConnection.put(connection.getID(), session);

        // GameState mutation deferred to the tick thread.
        pendingActions.add(() -> {
            int[] spawnPosition = SPAWN_POSITIONS[playerId];
            state.addPlayer(new Player(playerId, spawnPosition[0], spawnPosition[1]));
        });

        connection.sendTCP(new JoinAccepted(playerId));
        System.out.println("[server] player " + playerId + " connected from " + connection.getRemoteAddressTCP());
    }

    private void handleDisconnected(Connection connection) {
        // TODO (teammate B): remove the player from GameState (queue via pendingActions),
        // broadcast PlayerLeft, end the game if only one player remains.
        // Important to remove Player from GameState since we reuse PlayerId
        PlayerSession session = sessionsByConnection.remove(connection.getID());
        if (session != null) {
            System.out.println("[server] player " + session.playerId + " disconnected");

            // Refresh lobby for remaining clients after someone leaves
            broadcastLobbyState();
        }
    }

    private void handleReceived(Connection connection, NetworkMessage message) {
        PlayerSession session = sessionsByConnection.get(connection.getID());
        if (session == null) return; // rejected or already disconnected

        switch (message) {
            // Movement: overwrite-style. Latest intent wins, applied once per tick.
            case MoveCommand move -> session.pendingMove = new MoveCommand(
                    Integer.signum(move.getDx()),
                    Integer.signum(move.getDy())
            );

            // Bombs: count style. Every press should fire a placement attempt.
            case PlaceBombCommand placeBomb -> session.pendingBombPresses.incrementAndGet();

            case JoinRequest joinRequest -> {
                session.name = joinRequest.getPlayerName();
                System.out.println("[server] player " + session.playerId + " name: " + joinRequest.getPlayerName());
                broadcastLobbyState();
            }

            case Ping ping -> {
                // TODO (teammate B): connection.sendTCP(new Pong(ping.getClientTimestamp()));
            }

            case ReadyCommand ready -> {
                session.ready = ready.isReady();
                System.out.println("[server] player " + session.playerId + " ready: " + session.ready);
                broadcastLobbyState();
            }

            case StartGameCommand startGame -> {
                int hostPlayerId = getHostPlayerId();

                if (session.playerId != hostPlayerId) {
                    System.out.println("[server] player " + session.playerId + " tried to start game but is not host");
                    return;
                }

                if (sessionsByConnection.size() < 2) {
                    System.out.println("[server] cannot start: need at least 2 players");
                    return;
                }

                boolean everyoneReady = sessionsByConnection.values().stream()
                        .filter(s -> s.playerId != hostPlayerId)
                        .allMatch(s -> s.ready);

                if (!everyoneReady) {
                    System.out.println("[server] cannot start: not everyone is ready");
                    return;
                }

                gameStarted = true;
                kryoServer.sendToAllTCP(new GameStarted());
                System.out.println("[server] game started");
            }

            default -> { /* other message types — ignore */ }
        }
    }

    // Tick thread

    private void tick() {
        try {
            // Measured delta. scheduleAtFixedRate is approximate; a hard 1/TICK_HZ
            // would drift if a tick runs late. Cap at 100ms so a stall (GC pause,
            // debugger breakpoint, OS hiccup) doesn't produce a huge step that
            // teleports players through walls.
            long now = System.nanoTime();
            double delta = (now - previousTickNanos) / 1_000_000_000.0;
            previousTickNanos = now;
            if (delta > 0.1) delta = 0.1;

            // 1. Drain queued GameState mutations from the network thread.
            Runnable action;
            while ((action = pendingActions.poll()) != null) {
                action.run();
            }

            // 2. Apply each session's input to its player.
            for (PlayerSession session : sessionsByConnection.values()) {
                Player player = findPlayer(session.playerId);
                if (player == null) continue; // mutation queued but spawn hasn't run yet

                // Movement: read-and-clear the latest intent. If the client stopped
                // sending (key released), pendingMove stays null and the player
                // doesn't move — no explicit stop message needed.
                MoveCommand move = session.pendingMove;
                session.pendingMove = null;
                if (move != null) {
                    gameManager.movePlayer(state, player, move.getDx(), move.getDy(), delta);
                }

                // Bombs: drain the counter atomically.
                int presses = session.pendingBombPresses.getAndSet(0);
                for (int i = 0; i < presses; i++) {
                    gameManager.placeBomb(state, player);
                }
            }

            // 3. Advance the world.
            gameManager.update(state, delta);

            // 4. Broadcast snapshot.
            // TODO (teammate C): also broadcast GameOver when the win condition triggers.
            kryoServer.sendToAllTCP(new StateSnapshot(state));

        } catch (Exception e) {
            // scheduleAtFixedRate silently cancels the task if a tick throws.
            System.err.println("[server] tick error:");
            e.printStackTrace();
        }
    }

    // Helpers

    /**
     * Closes the current hosted session for all connected clients.
     * Sends a {@link SessionClosed} message with the provided reason so clients
     * can return to the main menu before the embedded server shuts down.
     */
    public void closeSession(String reason) {
        kryoServer.sendToAllTCP(new SessionClosed(reason));
    }

    private void broadcastLobbyState() {
        List<PlayerSession> sortedSessions = sessionsByConnection.values().stream()
                .sorted(Comparator.comparingInt(session -> session.playerId))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Integer> playerIds = sortedSessions.stream()
                .map(session -> session.playerId)
                .collect(Collectors.toCollection(ArrayList::new));

        List<String> playerNames = sortedSessions.stream()
                .map(session -> session.name)
                .collect(Collectors.toCollection(ArrayList::new));

        List<Boolean> readyStates = sortedSessions.stream()
                .map(session -> session.ready)
                .collect(Collectors.toCollection(ArrayList::new));

        int hostPlayerId = sortedSessions.isEmpty()
                ? -1
                : sortedSessions.get(0).playerId;

        kryoServer.sendToAllTCP(new LobbyState(playerIds, playerNames, readyStates, hostPlayerId));

        System.out.println("[server] lobby state broadcast: " + playerNames);
    }

    private int nextAvailablePlayerId() {
        Set<Integer> taken = sessionsByConnection.values().stream()
                .map(s -> s.playerId)
                .collect(Collectors.toSet());
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (!taken.contains(i)) return i;
        }
        return -1;
    }

    private Player findPlayer(int playerId) {
        for (Player p : state.getPlayers()) {
            if (p.getPlayerId() == playerId) return p;
        }
        return null;
    }

    private int getHostPlayerId() {
        return sessionsByConnection.values().stream()
                .mapToInt(session -> session.playerId)
                .min()
                .orElse(-1);
    }

    // Per-connection state

    private static class PlayerSession {
        final int playerId;
        final Connection connection;
        String name = "Player";
        volatile boolean ready = false;

        /** Latest movement intent. volatile: written on net thread, read on tick thread. */
        volatile MoveCommand pendingMove;

        /** Count of bomb presses since last tick. Atomic: net thread increments, tick drains. */
        final AtomicInteger pendingBombPresses = new AtomicInteger(0);

        PlayerSession(int playerId, Connection connection) {
            this.playerId = playerId;
            this.connection = connection;
        }
    }

    // Dedicated-server entry point

    public static void main(String[] args) throws IOException {
        GameServer server = new GameServer();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}