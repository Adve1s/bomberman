package com.bomberman.bomberman.server.logic;

import com.bomberman.bomberman.shared.entity.*;
import com.bomberman.bomberman.shared.model.GameMap;
import com.bomberman.bomberman.shared.model.GameState;
import com.bomberman.bomberman.shared.model.Tile;
import com.bomberman.bomberman.shared.entity.WarningTile;
import com.bomberman.bomberman.shared.util.Constants;
import com.bomberman.bomberman.shared.util.Direction;
import java.util.Iterator;

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
        state.addRoundTime(deltaTime);

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
        checkPowerUpCollisions(state);

        // 4. Game progression
        updateGameProgression(state);
        updateWarningTiles(state);

        // 5. Remove inactive entities
        state.getBombs().removeIf(b -> !b.isActive());
        state.getExplosions().removeIf(e -> !e.isActive());
        state.getPowerUps().removeIf(p -> !p.isActive());

        // 6. Flush deferred queues
        state.flushQueues();

        // 7. Win/lose check
        checkWinCondition(state);
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

    // Power up collision checks

    private void checkPowerUpCollisions(GameState state){
        for (PowerUp powerUp : state.getPowerUps()){
            if (!powerUp.isActive()) continue;

            int row = powerUp.getRow();
            int col = powerUp.getCol();

            for (Player player : CollisionDetector.getPlayersAt(state, row, col)){
                PowerUp.PowerUpType type = powerUp.getType();

                switch (type){
                    case EXTRA_RANGE -> player.addExplosionRange(Constants.RANGE_EFFECT);
                    case SPEED_BOOST -> player.addSpeed(Constants.SPEED_EFFECT);
                    case EXTRA_BOMB -> player.addBombCapacity(Constants.BOMB_EFFECT);
                }

                powerUp.destroy();
            }
        }
    }

    // Game progression

    private void updateGameProgression(GameState state){
        double now = state.getRoundTime();

        while (now - state.getLastShrinkTime() >= Constants.SHRINK_INTERVAL) {

            shrinkMap(state);

            state.setLastShrinkTime(state.getLastShrinkTime() + Constants.SHRINK_INTERVAL);
            state.increaseShrinkLayer();
        }
    }

    private void shrinkMap(GameState state) {
        GameMap map = state.getGameMap();

        int layer = state.getShrinkLayer() + 1;
        int rows = map.getRows();
        int cols = map.getCols();

        int maxLayer = Math.min(rows, cols) / 2;

        if (layer > maxLayer) {
            return;
        }

        // top & bottom
        for (int c = layer; c < cols - layer; c++) {
            markWarning(state, layer, c);
            markWarning(state, rows - 1 - layer, c);
        }

        // left & right
        for (int r = layer + 1; r < rows - layer - 1; r++) {
            markWarning(state, r, layer);
            markWarning(state, r, cols - 1 - layer);
        }
    }

    private void markWarning(GameState state, int r, int c) {
        state.addWarningTile(r, c, state.getRoundTime());
    }

    private void updateWarningTiles(GameState state) {
        double now = state.getRoundTime();

        Iterator<? extends WarningTileView> it = state.getWarningTiles().iterator();

        while (it.hasNext()) {
            WarningTileView tile = it.next();

            if (tile.getStartTime() == 0) continue; // safety (optional)

            if (tile.shouldBecomeWall(now, Constants.WARNING_WALL_DURATION)) {

                int row = tile.getRow();
                int col = tile.getCol();

                state.getGameMap().setTile(row, col, Tile.WALL);

                for (Player player : CollisionDetector.getPlayersOverlappingTile(state, row, col)) {
                    player.kill();
                }

                for (PowerUp powerUp : state.getPowerUps()) {
                    if (powerUp.getRow() == row && powerUp.getCol() == col) {
                        powerUp.destroy();
                    }
                }

                it.remove(); // OK because we're iterating the real list
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

                    double spawnChance = Constants.SPAWN_CHANCE;
                    double typeRoll = Math.random();

                    if (typeRoll <= spawnChance){
                        int type = (int)(Math.random() * 3);
                        PowerUp.PowerUpType powerUpType;

                        if (type == 0){
                            powerUpType = PowerUp.PowerUpType.EXTRA_BOMB;
                        } else if (type == 1){
                            powerUpType = PowerUp.PowerUpType.EXTRA_RANGE;
                        } else {
                            powerUpType = PowerUp.PowerUpType.SPEED_BOOST;
                        }

                        PowerUp powerUp = new PowerUp(powerUpType, r, c);
                        state.queuePowerUp(powerUp);
                    }

                    break;
                }

                state.queueExplosion(new Explosion(r, c));
            }
        }
    }

    private void checkWinCondition(GameState state){
        int alivePlayers = 0;
        int lastAliveId = -1;

        for (Player player : state.getPlayers()){
            if(player.isAlive()){
                alivePlayers++;
                lastAliveId = player.getPlayerId();
            }
        }

        if (alivePlayers <= 1){

            if (alivePlayers == 1){
                state.setWinner(lastAliveId);
            } else {
                state.setDraw();
            }

            state.setGameOver(true);
            System.out.println("GAME OVER!");
        }
    }
}