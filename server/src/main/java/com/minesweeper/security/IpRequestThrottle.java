package com.minesweeper.security;

import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Quanti tentativi per IP su login/reg/game (solo questo server, non un WAF). */
@Component
public class IpRequestThrottle {

    private final int max;
    private final long windowMs;
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public IpRequestThrottle(
            @Value("${app.security.auth-rate-max-per-minute:30}") int maxPerWindow,
            @Value("${app.security.auth-rate-window-seconds:60}") int windowSeconds) {
        this.max = Math.max(1, maxPerWindow);
        this.windowMs = Math.max(1L, windowSeconds) * 1000L;
    }

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < now - windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= max) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    /** Pulisce le entry scadute periodicamente per evitare memory leak. */
    @Scheduled(fixedRate = 300_000)
    public void evictExpiredEntries() {
        long cutoff = System.currentTimeMillis() - Math.max(windowMs, 300_000L);
        Iterator<Map.Entry<String, Deque<Long>>> it = hits.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<Long>> entry = it.next();
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                    deque.pollFirst();
                }
                if (deque.isEmpty()) {
                    it.remove();
                }
            }
        }
    }
}
