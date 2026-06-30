package com.minesweeper.game.repository;

import com.minesweeper.game.engine.Minesweeper;
import com.minesweeper.game.model.GameSessionEntry;

import java.time.Duration;
import java.util.Optional;

/**
 * Repository astratto per le sessioni di gioco.
 * Disaccoppia GameSessionService dallo storage concreto (RAM, Redis, DB).
 */
public interface GameSessionRepository {

    void save(String sessionId, Minesweeper game);

    Optional<GameSessionEntry> findById(String sessionId);

    void touch(String sessionId);

    void evictExpired(Duration ttl);
}
