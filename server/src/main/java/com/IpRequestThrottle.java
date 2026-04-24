package com;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Quanti tentativi per IP su login/reg (solo questo server, non un WAF). */
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
}
