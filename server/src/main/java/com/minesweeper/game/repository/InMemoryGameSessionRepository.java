package com.minesweeper.game.repository;

import com.minesweeper.exception.GameSessionNotFoundException;
import com.minesweeper.game.engine.Minesweeper;
import com.minesweeper.game.model.GameSessionEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/** Implementazione in-memory del GameSessionRepository con ConcurrentHashMap e TTL. */
@Repository
public class InMemoryGameSessionRepository implements GameSessionRepository {

    private final Map<String, GameSessionEntry> sessions = new ConcurrentHashMap<>();
    private final int maxSessions;

    public InMemoryGameSessionRepository(
            @Value("${app.game.max-sessions:10000}") int maxSessions) {
        this.maxSessions = Math.max(1, maxSessions);
    }

    @Override
    public void save(String sessionId, Minesweeper game) {
        // Operazione atomica: previene race condition nel check del limite
        GameSessionEntry newEntry = new GameSessionEntry(game, Instant.now());
        GameSessionEntry existing = sessions.putIfAbsent(sessionId, newEntry);
        if (existing != null) {
            // Sovrascrivi se già esiste (update atomico)
            sessions.put(sessionId, newEntry);
            return;
        }
        // Se la put ha successo ma abbiamo superato il limite, rimuovi e lancia eccezione
        if (sessions.size() > maxSessions) {
            sessions.remove(sessionId);
            throw new GameSessionNotFoundException(
                    "Troppe partite attive. Riprova più tardi.");
        }
    }

    @Override
    public Optional<GameSessionEntry> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void touch(String sessionId) {
        GameSessionEntry entry = sessions.get(sessionId);
        if (entry != null) {
            entry.setLastAccess(Instant.now());
        }
    }

    @Override
    public void evictExpired(Duration ttl) {
        Instant limit = Instant.now().minus(ttl);
        sessions.entrySet().removeIf(e -> e.getValue().getLastAccess().isBefore(limit));
    }
}
