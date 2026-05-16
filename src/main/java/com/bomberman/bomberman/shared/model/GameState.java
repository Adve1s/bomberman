package com.bomberman.bomberman.shared.model;

import com.bomberman.bomberman.shared.entity.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure data container: owns the map and all active entities.
 * Implements GameStateView to provide read-only access for the Renderer.
 */
public class GameState implements GameStateView {

    private final GameMap gameMap;

    // Typed entity lists
    private final List<Player> players;
    private final List<Bomb> bombs;
    private final List<Explosion> explosions;
    private final List<PowerUp> powerUps;

    // Deferred add queues (populated during update, applied after).
    // Marked transient: server-only scratch space, never sent over the wire.
    // On the client these fields will be null after deserialization, which is
    // fine because the client never runs the update loop or calls queue*().
    private transient final List<Bomb> bombsToAdd;
    private transient final List<Explosion> explosionsToAdd;
    private transient final List<PowerUp> powerUpsToAdd;

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
    public boolean isGameOver() { return gameOver; }

    // Full access (server-side only)

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

    // Entity registration

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

    // Queue management

    /**
     * Moves entities from deferred queues into the main lists.
     */
    public void flushQueues() {
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