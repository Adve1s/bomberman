package com.bomberman.bomberman.server.logic;

import com.bomberman.bomberman.shared.entity.Bomb;
import com.bomberman.bomberman.shared.entity.Player;
import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.util.Constants;

import java.util.List;

/**
 * Static helper for spatial queries: "what's at this position?"
 */
public final class CollisionDetector {

    private CollisionDetector() {} // static utility, no instantiation

    // Pixel-based collision

    /**
     * Checks if a player-sized hitbox at the given pixel position
     * would overlap any non-walkable tile (wall or box).
     */
    public static boolean collidesWithTerrain(GameState state, double pixelX, double pixelY) {
        int offset = Constants.PLAYER_HITBOX_OFFSET;
        int size = Constants.PLAYER_HITBOX_SIZE;

        int leftCol = (int) ((pixelX + offset) / Constants.TILE_SIZE);
        int rightCol = (int) ((pixelX + offset + size - 1) / Constants.TILE_SIZE);
        int topRow = (int) ((pixelY + offset) / Constants.TILE_SIZE);
        int bottomRow = (int) ((pixelY + offset + size - 1) / Constants.TILE_SIZE);

        for (int row = topRow; row <= bottomRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                if (!state.getGameMap().isWalkable(row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if moving from current to new position would newly overlap a bomb tile.
     */
    public static boolean collidesWithBombs(GameState state, double newX, double newY, double currentX, double currentY) {
        int offset = Constants.PLAYER_HITBOX_OFFSET;
        int size = Constants.PLAYER_HITBOX_SIZE;

        // Tiles the NEW position overlaps
        int newLeftCol = (int) ((newX + offset) / Constants.TILE_SIZE);
        int newRightCol = (int) ((newX + offset + size - 1) / Constants.TILE_SIZE);
        int newTopRow = (int) ((newY + offset) / Constants.TILE_SIZE);
        int newBottomRow = (int) ((newY + offset + size - 1) / Constants.TILE_SIZE);

        // Tiles the CURRENT position overlaps
        int curLeftCol = (int) ((currentX + offset) / Constants.TILE_SIZE);
        int curRightCol = (int) ((currentX + offset + size - 1) / Constants.TILE_SIZE);
        int curTopRow = (int) ((currentY + offset) / Constants.TILE_SIZE);
        int curBottomRow = (int) ((currentY + offset + size - 1) / Constants.TILE_SIZE);

        for (int row = newTopRow; row <= newBottomRow; row++) {
            for (int col = newLeftCol; col <= newRightCol; col++) {
                if (getBombAt(state, row, col) != null) {
                    // Is this a NEW overlap? (tile not covered by current position)
                    boolean alreadyOverlapping = row >= curTopRow && row <= curBottomRow
                            && col >= curLeftCol && col <= curRightCol;

                    if (!alreadyOverlapping) {
                        return true; // entering a bomb tile we weren't on before
                    }
                }
            }
        }
        return false;
    }

    // Tile-based queries

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

    /**
     * Returns all alive players whose hitbox overlaps the given tile.
     */
    public static List<Player> getPlayersOverlappingTile(
            GameState state,
            int row,
            int col
    ) {
        double tileX = col * Constants.TILE_SIZE;
        double tileY = row * Constants.TILE_SIZE;

        double tileRight = tileX + Constants.TILE_SIZE;
        double tileBottom = tileY + Constants.TILE_SIZE;

        return state.getPlayers().stream().filter(Player::isAlive).filter(player -> {

                    double playerLeft =
                            player.getPixelX() + Constants.PLAYER_HITBOX_OFFSET;

                    double playerTop =
                            player.getPixelY() + Constants.PLAYER_HITBOX_OFFSET;

                    double playerRight =
                            playerLeft + Constants.PLAYER_HITBOX_SIZE;

                    double playerBottom =
                            playerTop + Constants.PLAYER_HITBOX_SIZE;

                    return playerRight > tileX &&
                            playerLeft < tileRight &&
                            playerBottom > tileY &&
                            playerTop < tileBottom;
                })
                .toList();
    }
}