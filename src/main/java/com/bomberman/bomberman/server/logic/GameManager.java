package com.bomberman.bomberman.server.logic;

import com.bomberman.bomberman.shared.entity.*;
import com.bomberman.bomberman.shared.model.GameMap;
import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.model.Tile;
import com.bomberman.bomberman.shared.util.Constants;
import com.bomberman.bomberman.shared.util.Direction;

/**
 * The game brain. Runs all game logic against a GameState.
 */
public class GameManager {

    // Game setup

    public void initializeGame(GameState state) {
        state.addPlayer(new Player(0, 1, 1));
    }

    // Player actions

    /**
     * Moves the player by pixels in the given direction.
     */
    public void movePlayer(GameState state, Player player, Direction direction, double deltaTime) {
        if (!player.isAlive()) return;

        double speed = player.getSpeed() * Constants.TILE_SIZE; // pixels per second
        double step = speed * deltaTime;

        double currentX = player.getPixelX();
        double currentY = player.getPixelY();

        double newX = currentX + direction.colDelta * step;
        double newY = currentY + direction.rowDelta * step;

            if (!CollisionDetector.collidesWithTerrain(state, newX, newY)
                    && !CollisionDetector.collidesWithBombs(state, newX, newY, currentX, currentY)){
            player.setPixelPosition(newX, newY);
        }
    }

    /**
     * Attempts to place a bomb at the nearest tile to the player's position.
     */
    public void placeBomb(GameState state, Player player) {
        if (!player.isAlive()) return;
        if (!player.canPlaceBomb()) return;

        int row = (int) Math.round(player.getPixelY() / (double) Constants.TILE_SIZE);
        int col = (int) Math.round(player.getPixelX() / (double) Constants.TILE_SIZE);

        if (CollisionDetector.getBombAt(state, row, col) != null) return;

        Bomb bomb = new Bomb(player.getPlayerId(), row, col, player.getExplosionRange() );
        state.queueBomb(bomb);
        player.bombPlaced();
    }

    // Frame update

    /**
     * Advances the game by one frame.
     */
    public void update(GameState state, double deltaTime) {
        if (state.isGameOver()) return;

        // 1. Update all entities (player syncs grid pos, bombs tick, explosions fade)
        state.getPlayers().forEach(p -> p.update(deltaTime));
        state.getBombs().forEach(b -> b.update(deltaTime));
        state.getExplosions().forEach(e -> e.update(deltaTime));

        // 2. Handle bombs that just became inactive this frame (exploded)
        for (Bomb bomb : state.getBombs()) {
            if (!bomb.isActive()) {
                detonateBomb(state, bomb);
            }
        }

        // 3. Collision checks
        checkExplosionCollisions(state);
        // TODO: player vs power-up (pickup)

        // 4. Remove inactive entities
        state.getBombs().removeIf(b -> !b.isActive());
        state.getExplosions().removeIf(e -> !e.isActive());
        state.getPowerUps().removeIf(p -> !p.isActive());

        // 5. Flush deferred queues
        state.flushQueues();

        // 6. Win/lose check
        // TODO: check if only one player alive
    }

    // Collision checks

    private void checkExplosionCollisions(GameState state) {
        for (Explosion explosion : state.getExplosions()) {
            if (!explosion.isActive()) continue;

            int row = explosion.getRow();
            int col = explosion.getCol();

            for (Player player : CollisionDetector.getPlayersAt(state, row, col)) {
                player.kill();
            }

            Bomb bomb = CollisionDetector.getBombAt(state, row, col);
            if (bomb != null) {
                detonateBomb(state, bomb);
            }
        }
    }

    // Explosion logic

    private void detonateBomb(GameState state, Bomb bomb) {
        bomb.explode();
        spawnExplosions(state, bomb);

        for (Player player : state.getPlayers()) {
            if (player.getPlayerId() == bomb.getOwnerPlayerId()) {
                player.bombExploded();
                break;
            }
        }
    }

    private void spawnExplosions(GameState state, Bomb bomb) {
        int row = bomb.getRow();
        int col = bomb.getCol();
        int range = bomb.getRange();
        GameMap map = state.getGameMap();

        state.queueExplosion(new Explosion(row, col));

        for (Direction dir : Direction.values()) {
            for (int i = 1; i <= range; i++) {
                int r = row + dir.rowDelta * i;
                int c = col + dir.colDelta * i;

                Tile tile = map.getTile(r, c);

                if (tile == Tile.WALL) {
                    break;
                }

                if (tile == Tile.BOX) {
                    map.setTile(r, c, Tile.FLOOR);
                    state.queueExplosion(new Explosion(r, c));
                    // TODO: random chance to spawn power-up here
                    break;
                }

                state.queueExplosion(new Explosion(r, c));
            }
        }
    }
}