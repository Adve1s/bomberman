package com.bomberman.bomberman.shared.network;

import com.bomberman.bomberman.shared.entity.*;
import com.bomberman.bomberman.shared.model.*;
import com.bomberman.bomberman.shared.util.Direction;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.util.ArrayList;

/**
 * Registers every class that travels over the wire.
 * Server and client both call this on their Kryo instance.
 * Order matters — do not reorder for no real reason.
 */
public final class NetworkRegistration {
    private NetworkRegistration() {
    }

    public static void register(Kryo kryo) {
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        // Model
        kryo.register(GameState.class);
        kryo.register(GameMap.class);

        // Entities
        kryo.register(Player.class);
        kryo.register(Bomb.class);
        kryo.register(Explosion.class);
        kryo.register(PowerUp.class);

        // Enums
        kryo.register(Tile.class);
        kryo.register(Direction.class);
        kryo.register(PowerUp.PowerUpType.class);

        // Array types (for GameMap.grid)
        kryo.register(Tile[].class);
        kryo.register(Tile[][].class);

        // Collection
        kryo.register(ArrayList.class);

        // Network messages — client → server
        kryo.register(JoinRequest.class);
        kryo.register(MoveCommand.class);
        kryo.register(PlaceBombCommand.class);
        kryo.register(ReadyCommand.class);
        kryo.register(StartGameCommand.class);
        kryo.register(Ping.class);

        // Network messages — server → client
        kryo.register(JoinAccepted.class);
        kryo.register(JoinRejected.class);
        kryo.register(LobbyState.class);
        kryo.register(GameStarted.class);
        kryo.register(StateSnapshot.class);
        kryo.register(PlayerLeft.class);
        kryo.register(Pong.class);
        kryo.register(GameOver.class);
    }
}