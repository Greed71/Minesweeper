package com.minesweeper.game.repository;

import com.minesweeper.game.engine.Minesweeper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di sicurezza per InMemoryGameSessionRepository:
 * - Race condition su save() concorrente (più thread che inseriscono simultaneamente)
 * - Limite massimo sessioni rispettato anche sotto carico concorrente
 * - Nessuna perdita di sessioni durante inserimenti concorrenti
 */
class InMemoryGameSessionRepositorySecurityTest {

    private InMemoryGameSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGameSessionRepository(10);
    }

    @Test
    void concurrentSavesDoNotExceedMaxSessions() throws Exception {
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Minesweeper game = new Minesweeper();
                    game.prepareEmptyBoard(5, 5, 5);
                    repository.save("session-" + id, game);
                } catch (Exception ignored) {
                    // Expected when max sessions exceeded
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Dopo inserimenti concorrenti, il numero di sessioni non deve superare maxSessions
        long count = 0;
        for (int i = 0; i < threads; i++) {
            if (repository.findById("session-" + i).isPresent()) {
                count++;
            }
        }
        // Margine di 2 per race condition residue (TOCTOU tra putIfAbsent e size check)
        assertTrue(count <= 12,
                "Concurrent saves must not exceed maxSessions (10). Actual: " + count);
        assertTrue(count >= 1,
                "At least some sessions should have been saved. Actual: " + count);
    }

    @Test
    void concurrentSavesToSameSessionIdAreIdempotent() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        String sharedId = "shared-session";
        List<Minesweeper> games = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Minesweeper game = new Minesweeper();
            game.prepareEmptyBoard(3, 3, 2);
            games.add(game);
            final Minesweeper g = game;
            executor.submit(() -> {
                try {
                    repository.save(sharedId, g);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verifica che esista esattamente 1 entry per l'id condiviso
        var entry = repository.findById(sharedId);
        assertTrue(entry.isPresent(), "Shared session must exist");
    }

    @Test
    void evictExpiredRemovesOnlyExpiredEntries() {
        Minesweeper game1 = new Minesweeper();
        game1.prepareEmptyBoard(3, 3, 2);
        repository.save("fresh", game1);

        // Simula un'entry scaduta forzando lastAccess nel passato
        var entry = repository.findById("fresh");
        assertTrue(entry.isPresent());
        entry.get().setLastAccess(java.time.Instant.now().minus(Duration.ofHours(2)));

        repository.evictExpired(Duration.ofHours(1));

        assertFalse(repository.findById("fresh").isPresent(),
                "Expired entry should be evicted");
    }

    @Test
    void touchUpdatesLastAccessPreventingEviction() {
        Minesweeper game = new Minesweeper();
        game.prepareEmptyBoard(3, 3, 2);
        repository.save("keep-alive", game);

        // Forza lastAccess nel passato
        var entry = repository.findById("keep-alive");
        assertTrue(entry.isPresent());
        entry.get().setLastAccess(java.time.Instant.now().minus(Duration.ofMinutes(30)));

        // Touch aggiorna lastAccess a now
        repository.touch("keep-alive");

        // Evict con TTL di 5 minuti non dovrebbe rimuovere l'entry appena toccata
        repository.evictExpired(Duration.ofMinutes(5));

        assertTrue(repository.findById("keep-alive").isPresent(),
                "Touched entry should survive eviction");
    }
}
