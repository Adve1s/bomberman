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

    // Player actions

    /**
     * Moves the player by pixels in the given direction.
     */
    public void movePlayer(GameState state, Player player, int dx, int dy, double deltaTime) {
        if (!player.isAlive()) return;
        if (dx == 0 && dy == 0) return;

        double speed = player.getSpeed() * Constants.TILE_SIZE;
        double step = speed * deltaTime;

        // No normalization — pressing UP+RIGHT moves at √2x cardinal speed.
        double stepX = dx * step;
        double stepY = dy * step;

        double currentX = player.getPixelX();
        double currentY = player.getPixelY();

        // X-axis with slide-to-wall
        currentX = slideAxis(state, currentX, currentY, stepX, true, currentX, currentY);
        // Y-axis with slide-to-wall — uses updated X so wall-sliding around corners works
        currentY = slideAxis(state, currentX, currentY, stepY, false, currentX, currentY);

        player.setPixelPosition(currentX, currentY);
    }

    /**
     * Moves on a single axis. If the full step is clear, takes it. If blocked,
     * binary-searches for the largest sub-step that doesn't collide, so the
     * player ends up flush against the wall instead of stopping short.
     */
    private double slideAxis(GameState state, double currentX, double currentY,
                             double axisStep, boolean isX, double fromX, double fromY) {
        if (axisStep == 0) return isX ? currentX : currentY;

        double tryX = isX ? currentX + axisStep : currentX;
        double tryY = isX ? currentY : currentY + axisStep;
        if (canMoveTo(state, tryX, tryY, fromX, fromY)) {
            return isX ? tryX : tryY;
        }

        // Blocked — binary search for max valid sub-step.
        double low = 0, high = Math.abs(axisStep);
        double sign = Math.signum(axisStep);
        for (int i = 0; i < 8; i++) { // 8 iterations → < 0.02px precision for step ≤ 5px
            double mid = (low + high) / 2;
            double midX = isX ? currentX + sign * mid : currentX;
            double midY = isX ? currentY : currentY + sign * mid;
            if (canMoveTo(state, midX, midY, fromX, fromY)) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return (isX ? currentX : currentY) + sign * low;
    }

    /**
     * Checks if player can move there.
     */
    private boolean canMoveTo(GameState state, double tryX, double tryY, double fromX, double fromY) {
        return !CollisionDetector.collidesWithTerrain(state, tryX, tryY)
                && !CollisionDetector.collidesWithBombs(state, tryX, tryY, fromX, fromY);
    }

    /**
     * Attempts to place a bomb at the nearest tile to the player's position.
     */
    public void placeBomb(GameState state, Player player) {
        if (!player.isAlive()) return;
        if (!player.canPlaceBomb()) return;

        int row = player.getRow();
        int col = player.getCol();

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
        // TODO (teammate C): player vs power-up (pickup)

        // 4. Game progression

        // TODO (teammate C): shrinking map / sudden death / round timer.
        // Typical Bomberman progression: after ~60s, start marking border tiles
        // as WALL one per second spiraling inward, forcing players together.
        // Players standing on a newly-walled tile die.
        //
        // You'll need:
        //   - A round-elapsed-time field on GameState (incremented here each tick)
        //   - A "next-tile-to-claim" pointer or a spiral coordinate generator
        //   - Per-tick check: if elapsed time crossed the next threshold, mark
        //     the next tile as WALL via map.setTile(r, c, Tile.WALL) and kill
        //     any player(s) standing on it (CollisionDetector.getPlayersAt)
        //
        // Once this grows past a few lines, extract into updateGameProgression(state, deltaTime)
        // for readability — same pattern as spawnExplosions() and checkExplosionCollisions().

        // 5. Remove inactive entities
        state.getBombs().removeIf(b -> !b.isActive());
        state.getExplosions().removeIf(e -> !e.isActive());
        state.getPowerUps().removeIf(p -> !p.isActive());

        // 6. Flush deferred queues
        state.flushQueues();

        // 7. Win/lose check
        // TODO (teammate C): check if only one player alive
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
                    // TODO (teammate C): random chance to spawn power-up here
                    break;
                }

                state.queueExplosion(new Explosion(r, c));
            }
        }
    }
}