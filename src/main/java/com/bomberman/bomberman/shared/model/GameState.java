package com.bomberman.bomberman.shared.model;

import com.bomberman.bomberman.shared.entity.*;
import com.bomberman.bomberman.shared.util.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Central game state: owns the map and all active entities.
 *
 * Entity lists are separated by type for:
 *   - Type safety (no casting)
 *   - Efficient iteration (e.g. check only bombs for chain explosions)
 *   - Controlled render order (powerups → bombs → explosions → players)
 *
 * Uses deferred add/remove queues so lists aren't modified during iteration.
 *
 * Implements GameStateView to provide read-only access for the Renderer.
 * Server-side code uses the full class directly (addPlayer, update, etc.).
 */
public class GameState implements GameStateView {

    private final GameMap gameMap;

    // Typed entity lists
    private final List<Player> players;
    private final List<Bomb> bombs;
    private final List<Explosion> explosions;
    private final List<PowerUp> powerUps;

    // Deferred add queues (populated during update, applied after)
    private final List<Bomb> bombsToAdd;
    private final List<Explosion> explosionsToAdd;
    private final List<PowerUp> powerUpsToAdd;

    private boolean gameOver;

    public GameState() {
        this.gameMap = new GameMap();
        this.players = new ArrayList<>();
        this.bombs = new ArrayList<>();
        this.explosions = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.bombsToAdd = new ArrayList<>();
        this.explosionsToAdd = new ArrayList<>();
        this.powerUpsToAdd = new ArrayList<>();
        this.gameOver = false;
    }

    // GameStateView (read-only, used by Renderer)

    @Override
    public GameMapView getGameMapView() {
        return gameMap;
    }

    @Override
    public List<? extends PlayerView> getPlayerViews() {
        return players;
    }

    @Override
    public List<? extends BombView> getBombViews() {
        return bombs;
    }

    @Override
    public List<? extends ExplosionView> getExplosionViews() {
        return explosions;
    }

    @Override
    public List<? extends PowerUpView> getPowerUpViews() {
        return powerUps;
    }

    @Override
    public boolean isGameOver() {
        return gameOver;
    }

    // ── Full access (server-side only) ──

    public GameMap getGameMap() {
        return gameMap;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Bomb> getBombs() {
        return bombs;
    }

    public List<Explosion> getExplosions() {
        return explosions;
    }

    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    // ── Entity registration ──

    public void addPlayer(Player player) {
        players.add(player);
    }

    /**
     * Queue a bomb for addition (safe to call during update loop).
     */
    public void queueBomb(Bomb bomb) {
        bombsToAdd.add(bomb);
    }

    /**
     * Queue an explosion tile for addition.
     */
    public void queueExplosion(Explosion explosion) {
        explosionsToAdd.add(explosion);
    }

    /**
     * Queue a power-up for addition.
     */
    public void queuePowerUp(PowerUp powerUp) {
        powerUpsToAdd.add(powerUp);
    }

    // Main update loop

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
    public void update(double deltaTime) {
        if (gameOver) return;

        // 1. Update all entities
        players.forEach(p -> p.update(deltaTime));
        bombs.forEach(b -> b.update(deltaTime));
        explosions.forEach(e -> e.update(deltaTime));

        // 2. Handle bombs that just became inactive this frame (exploded)
        for (Bomb bomb : bombs) {
            if (!bomb.isActive()) {
                spawnExplosions(bomb);

                // Notify the owner so they can place bombs again
                for (Player p : players) {
                    if (p.getPlayerId() == bomb.getOwnerPlayerId()) {
                        p.bombExploded();
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
        bombs.removeIf(b -> !b.isActive());
        explosions.removeIf(e -> !e.isActive());
        powerUps.removeIf(p -> !p.isActive());

        // 5. Flush deferred queues
        flushQueues();

        // 6. Win/lose check
        // TODO: check if only one player alive
    }

    /**
     * Spawns explosion entities from the bomb center outward in 4 directions.
     * Stops at walls, destroys bricks (and stops), may reveal power-ups.
     */
    private void spawnExplosions(Bomb bomb) {
        int row = bomb.getRow();
        int col = bomb.getCol();
        int range = bomb.getRange();

        // Center explosion
        queueExplosion(new Explosion(row, col));

        // Spread in each direction
        for (Direction dir : Direction.values()) {
            for (int i = 1; i <= range; i++) {
                int r = row + dir.rowDelta * i;
                int c = col + dir.colDelta * i;

                Tile tile = gameMap.getTile(r, c);

                if (tile == Tile.WALL) {
                    break; // wall stops explosion
                }

                if (tile == Tile.BOX) {
                    gameMap.setTile(r, c, Tile.FLOOR); // destroy brick
                    queueExplosion(new Explosion(r, c));
                    // TODO: random chance to spawn power-up here
                    break; // brick stops explosion (but is destroyed)
                }

                // Floor: explosion passes through
                queueExplosion(new Explosion(r, c));
            }
        }
    }

    private void flushQueues() {
        bombs.addAll(bombsToAdd);
        explosions.addAll(explosionsToAdd);
        powerUps.addAll(powerUpsToAdd);
        bombsToAdd.clear();
        explosionsToAdd.clear();
        powerUpsToAdd.clear();
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }
}