package com.bomberman.bomberman.shared.network;

import com.bomberman.bomberman.server.logic.GameManager;
import com.bomberman.bomberman.shared.entity.Player;
import com.bomberman.bomberman.shared.model.GameState;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that GameState and its entities round-trip through Kryo
 * without losing data. If a new entity class is added but not registered
 * in NetworkRegistration, these tests fail loudly.
 */
class SerializationRoundTripTest {

    private Kryo kryo;

    @BeforeEach
    void setUp() {
        kryo = new Kryo();
        NetworkRegistration.register(kryo);
    }

    @Test
    void gameStateRoundTrips() {
        GameState original = freshGameState();

        GameState copy = roundTrip(original, GameState.class);

        assertNotNull(copy);
        assertEquals(original.getPlayers().size(), copy.getPlayers().size());
        assertEquals(original.getGameMap().getRows(), copy.getGameMap().getRows());
        assertEquals(original.getGameMap().getCols(), copy.getGameMap().getCols());
        assertEquals(original.isGameOver(), copy.isGameOver());
    }

    @Test
    void playerFieldsSurviveRoundTrip() {
        GameState original = freshGameState();
        Player originalPlayer = original.getPlayers().getFirst();

        GameState copy = roundTrip(original, GameState.class);
        Player copiedPlayer = copy.getPlayers().getFirst();

        assertEquals(originalPlayer.getPlayerId(),       copiedPlayer.getPlayerId());
        assertEquals(originalPlayer.getPixelX(),         copiedPlayer.getPixelX());
        assertEquals(originalPlayer.getPixelY(),         copiedPlayer.getPixelY());
        assertEquals(originalPlayer.isAlive(),           copiedPlayer.isAlive());
        assertEquals(originalPlayer.getSpeed(),          copiedPlayer.getSpeed());
        assertEquals(originalPlayer.getMaxBombs(),       copiedPlayer.getMaxBombs());
        assertEquals(originalPlayer.getExplosionRange(), copiedPlayer.getExplosionRange());
    }

    @Test
    void mapTilesSurviveRoundTrip() {
        GameState original = freshGameState();
        int rows = original.getGameMap().getRows();
        int cols = original.getGameMap().getCols();

        GameState copy = roundTrip(original, GameState.class);

        // Spot-check tiles at corners + interior
        for (int row : new int[]{0, 1, rows / 2, rows - 1}) {
            for (int col : new int[]{0, 1, cols / 2, cols - 1}) {
                assertEquals(
                        original.getGameMap().getTile(row, col),
                        copy.getGameMap().getTile(row, col),
                        "Tile mismatch at row=" + row + " col=" + col
                );
            }
        }
    }

    private GameState freshGameState() {
        GameState state = new GameState();
        new GameManager().initializeGame(state);
        return state;
    }

    private <T> T roundTrip(T obj, Class<T> type) {
        Output out = new Output(64 * 1024);
        kryo.writeObject(out, obj);
        out.close();
        byte[] bytes = out.toBytes();

        Input in = new Input(bytes);
        T copy = kryo.readObject(in, type);
        in.close();
        return copy;
    }
}