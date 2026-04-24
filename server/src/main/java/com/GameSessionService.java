package com;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Partite in RAM, con scadenza se resti troppo inattivo. Un solo nodo: in
 * cluster serve un database o un cache comune.
 */
@Service
public class GameSessionService {

    private final Map<String, GameEntry> sessions = new ConcurrentHashMap<>();
    private final Duration sessionTtl;

    public GameSessionService(
            @Value("${app.game.session-ttl-seconds:3600}") int sessionTtlSeconds) {
        this.sessionTtl = Duration.ofSeconds(Math.max(60, sessionTtlSeconds));
    }

    public GameStateResponse startGame(GameConfig config) {
        Minesweeper game = new Minesweeper();
        game.prepareEmptyBoard(config.getRows(), config.getCols(), config.getMines());
        putSession(config.getSessionId(), game);
        return new GameStateResponse(
                game.getVisibleBoard(), false, false, "Inizia a giocare!");
    }

    public GameStateResponse revealCell(ClickPosition click) {
        GameEntry entry = sessions.get(click.getSessionId());
        if (entry == null) {
            throw new GameSessionNotFoundException(click.getSessionId());
        }
        entry.lastAccess = Instant.now();
        entry.game.revealCell(click.getRow(), click.getCol());
        return toResponse(entry.game);
    }

    @Scheduled(
            initialDelayString = "${app.game.session-cleanup-initial-delay-ms:60000}",
            fixedRateString = "${app.game.session-cleanup-interval-ms:300000}")
    public void evictExpiredSessions() {
        Instant limit = Instant.now().minus(sessionTtl);
        sessions.entrySet().removeIf(e -> e.getValue().lastAccess.isBefore(limit));
    }

    private void putSession(String sessionId, Minesweeper game) {
        sessions.put(sessionId, new GameEntry(game, Instant.now()));
    }

    private static GameStateResponse toResponse(Minesweeper game) {
        String message;
        if (game.isGameOver()) {
            message = "Hai perso!";
        } else if (game.isGameWon()) {
            message = "Hai vinto!";
        } else {
            message = "Continua a giocare";
        }
        return new GameStateResponse(
                game.getVisibleBoard(), game.isGameOver(), game.isGameWon(), message);
    }

    private static final class GameEntry {
        final Minesweeper game;
        volatile Instant lastAccess;

        GameEntry(Minesweeper game, Instant lastAccess) {
            this.game = game;
            this.lastAccess = lastAccess;
        }
    }
}
