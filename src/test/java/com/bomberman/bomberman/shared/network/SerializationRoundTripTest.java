package com.bomberman.bomberman.shared.network;

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

    /**
     * Builds a GameState with one player whose stats are deliberately set to
     * non-default values. This way a serialization bug that silently re-applied
     * defaults on the receiving end would fail the round-trip tests instead of
     * sneaking past with "default == default".
     */
    private GameState freshGameState() {
        GameState state = new GameState();
        Player player = new Player(0, 1, 1);
        player.setPixelPosition(123.5, 456.25);  // off the default tile-aligned spawn
        player.addBombCapacity(2);                // max bombs = 3
        player.addExplosionRange(3);              // range = 4
        player.addSpeed(1.5);                     // speed = 4.5
        state.addPlayer(player);
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