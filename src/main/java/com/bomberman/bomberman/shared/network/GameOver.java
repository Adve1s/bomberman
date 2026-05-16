package com.bomberman.bomberman.shared.network;

/**
 * Broadcast when the game ends. winnerPlayerId is the surviving player,
 * or -1 if the game ended in a draw (e.g. everyone died on the same tick).
 */
public class GameOver implements NetworkMessage {
    private final int winnerPlayerId;
    private final boolean draw;

    public GameOver(int winnerPlayerId, boolean draw) {
        this.winnerPlayerId = winnerPlayerId;
        this.draw = draw;
    }

    public int getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public boolean isDraw() {
        return draw;
    }
}