package com.minesweeper.game.service;

import com.minesweeper.exception.GameSessionNotFoundException;
import com.minesweeper.game.dto.ClickPosition;
import com.minesweeper.game.dto.GameConfig;
import com.minesweeper.game.dto.GameStateResponse;
import com.minesweeper.game.engine.Minesweeper;
import com.minesweeper.game.model.GameSessionEntry;
import com.minesweeper.game.repository.GameSessionRepository;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Servizio partite: logica di gioco delegata al repository per lo storage.
 * Disaccoppiato dall'implementazione concreta (RAM, Redis, DB).
 */
@Service
public class GameSessionService {

    private final GameSessionRepository repository;
    private final Duration sessionTtl;

    public GameSessionService(
            GameSessionRepository repository,
            @Value("${app.game.session-ttl-seconds:3600}") int sessionTtlSeconds) {
        this.repository = repository;
        this.sessionTtl = Duration.ofSeconds(Math.max(60, sessionTtlSeconds));
    }

    public GameStateResponse startGame(GameConfig config) {
        Minesweeper game = new Minesweeper();
        game.prepareEmptyBoard(config.getRows(), config.getCols(), config.getMines());
        repository.save(config.getSessionId(), game);
        return new GameStateResponse(
                game.getVisibleBoard(), false, false, "Inizia a giocare!");
    }

    public GameStateResponse revealCell(ClickPosition click) {
        GameSessionEntry entry = repository.findById(click.getSessionId())
                .orElseThrow(() -> new GameSessionNotFoundException(click.getSessionId()));
        repository.touch(click.getSessionId());
        entry.getGame().revealCell(click.getRow(), click.getCol());
        return toResponse(entry.getGame());
    }

    @Scheduled(
            initialDelayString = "${app.game.session-cleanup-initial-delay-ms:60000}",
            fixedRateString = "${app.game.session-cleanup-interval-ms:300000}")
    public void evictExpiredSessions() {
        repository.evictExpired(sessionTtl);
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
}
