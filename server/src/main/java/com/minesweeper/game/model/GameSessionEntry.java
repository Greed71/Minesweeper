package com.minesweeper.game.model;

import com.minesweeper.game.engine.Minesweeper;

import java.time.Instant;

/**
 * Entry di sessione gioco: contiene l'istanza Minesweeper e il timestamp ultimo accesso.
 * Separata dal repository per chiarezza.
 */
public class GameSessionEntry {
    private final Minesweeper game;
    private volatile Instant lastAccess;

    public GameSessionEntry(Minesweeper game, Instant lastAccess) {
        this.game = game;
        this.lastAccess = lastAccess;
    }

    public Minesweeper getGame() { return game; }
    public Instant getLastAccess() { return lastAccess; }
    public void setLastAccess(Instant lastAccess) { this.lastAccess = lastAccess; }
}
