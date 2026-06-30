package com.minesweeper.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limit dedicato per GET /api/ping (keep-alive del client).
 * Separa il traffico ping dalle richieste auth/game: un client che pinga
 * normalmente non deve consumare il budget di login/registrazione,
 * e un DDoS mirato al ping non intacca gli altri endpoint.
 */
@Component
public class PingRateLimitFilter extends OncePerRequestFilter {

    private final int max;
    private final long windowMs;
    private final boolean trustForwardedHeader;
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public PingRateLimitFilter(
            @Value("${app.security.ping-rate-max-per-minute:120}") int maxPerWindow,
            @Value("${app.security.ping-rate-window-seconds:60}") int windowSeconds,
            @Value("${app.security.trust-x-forwarded-for:false}") boolean trustForwardedHeader) {
        this.max = Math.max(1, maxPerWindow);
        this.windowMs = Math.max(1L, windowSeconds) * 1000L;
        this.trustForwardedHeader = trustForwardedHeader;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !("GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().endsWith("/api/ping"));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String ip = clientIp(request);
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = hits.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < now - windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= max) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                try (PrintWriter w = response.getWriter()) {
                    w.write("{\"error\":\"rate_limited\",\"message\":\"Troppi ping. Riprova più tardi.\"}");
                }
                return;
            }
            timestamps.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeader) {
            String h = request.getHeader("X-Forwarded-For");
            if (h != null && !h.isBlank()) {
                return h.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
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
