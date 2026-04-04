package com.bomberman.bomberman.server.logic;

import com.bomberman.bomberman.shared.entity.*;
import com.bomberman.bomberman.shared.model.GameMap;
import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.model.Tile;
import com.bomberman.bomberman.shared.util.Constants;
import com.bomberman.bomberman.shared.util.Direction;

/**
 * The game brain. Runs all game logic against a GameState:
 *   - Game setup (player spawning)
 *   - Player actions (movement, bomb placement)
 *   - Update orchestration (tick entities, handle explosions)
 *   - Collision detection (TODO)
 *   - Win/lose conditions (TODO)
 */
public class GameManager {

    // Game setup

    /**
     * Sets up the initial game state with players in spawn positions.
     */
    public void initializeGame(GameState state) {
        state.addPlayer(new Player(0, 1, 1));
        // Future players:
        // state.addPlayer(new Player(1, 1, Constants.GRID_COLS - 2));
        // state.addPlayer(new Player(2, Constants.GRID_ROWS - 2, 1));
        // state.addPlayer(new Player(3, Constants.GRID_ROWS - 2, Constants.GRID_COLS - 2));
    }

    // Player actions

    /**
     * Attempts to move a player one tile in the given direction.
     */
    public void movePlayer(GameState state, Player player, Direction direction) {
        if (!player.isAlive()) return;

        int targetRow = player.getRow() + direction.rowDelta;
        int targetCol = player.getCol() + direction.colDelta;

        if (!CollisionDetector.isTileWalkable(state, targetRow, targetCol)) return;

        player.move(direction);
    }

    /**
     * Attempts to place a bomb at the player's current position.
     * Validates bomb limit and prevents stacking on the same tile.
     */
    public void placeBomb(GameState state, Player player) {
        if (!player.isAlive()) return;
        if (!player.canPlaceBomb()) return;

        int row = (int) Math.round(player.getPixelY() / Constants.TILE_SIZE);
        int col = (int) Math.round(player.getPixelX() / Constants.TILE_SIZE);

        if (CollisionDetector.getBombAt(state, row, col) != null) return;

        Bomb bomb = new Bomb(player.getPlayerId(), row, col, player.getExplosionRange() );
        state.queueBomb(bomb);
        player.bombPlaced();
    }

    // Frame update

    /**
     * Advances the game by one frame.
     * Call order matters:
     *   1. Update all entities (bombs count down, explosions fade, players move)
     *   2. Handle bomb explosions (spawn explosion entities, destroy bricks)
     *   3. Check collisions (explosions kill players, players pick up power-ups)
     *   4. Remove inactive entities
     *   5. Flush deferred add queues
     *   6. Check win/lose conditions
     */
    public void update(GameState state, double deltaTime) {
        if (state.isGameOver()) return;

        // 1. Update all entities
        state.getPlayers().forEach(p -> p.update(deltaTime));
        state.getBombs().forEach(b -> b.update(deltaTime));
        state.getExplosions().forEach(e -> e.update(deltaTime));

        // 2. Handle bombs that just became inactive this frame (exploded)
        for (Bomb bomb : state.getBombs()) {
            if (!bomb.isActive()) {
                spawnExplosions(state, bomb);

                for (Player player : state.getPlayers()) {
                    if (player.getPlayerId() == bomb.getOwnerPlayerId()) {
                        player.bombExploded();
                        break;
                    }
                }
            }
        }

        // 3. Collision checks
        // TODO: explosion vs player (kill)
        // TODO: explosion vs bomb (chain reaction)
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

    // Explosion logic

    /**
     * Spawns explosion entities from the bomb center outward in 4 directions.
     * Stops at walls, destroys bricks (and stops), may reveal power-ups.
     */
    private void spawnExplosions(GameState state, Bomb bomb) {
        int row = bomb.getRow();
        int col = bomb.getCol();
        int range = bomb.getRange();
        GameMap map = state.getGameMap();

        // Center explosion
        state.queueExplosion(new Explosion(row, col));

        // Spread in each direction
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