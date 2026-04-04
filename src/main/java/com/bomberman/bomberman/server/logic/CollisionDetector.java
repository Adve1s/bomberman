package com.bomberman.bomberman.server.logic;

import com.bomberman.bomberman.shared.entity.Bomb;
import com.bomberman.bomberman.shared.entity.Player;
import com.bomberman.bomberman.shared.model.GameState;

import java.util.List;

/**
 * Static helper for spatial queries: "what's at this tile?"
 */
public final class CollisionDetector {

    private CollisionDetector() {} // static utility, no instantiation

    /**
     * Whether a player can move onto this tile.
     */
    public static boolean isTileWalkable(GameState state, int row, int col) {
        if (!state.getGameMap().isWalkable(row, col)) return false;
        if (getBombAt(state, row, col) != null) return false;
        return true;
    }

    /**
     * Returns the active bomb at this position, or null if none.
     */
    public static Bomb getBombAt(GameState state, int row, int col) {
        for (Bomb bomb : state.getBombs()) {
            if (bomb.isActive() && bomb.getRow() == row && bomb.getCol() == col) {
                return bomb;
            }
        }
        return null;
    }

    /**
     * Returns all alive players standing on this tile.
     */
    public static List<Player> getPlayersAt(GameState state, int row, int col) {
        return state.getPlayers().stream()
                .filter(p -> p.isAlive() && p.getRow() == row && p.getCol() == col)
                .toList();
    }
}