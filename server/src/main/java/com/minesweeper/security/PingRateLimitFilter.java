package com.minesweeper.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * Bypass opzionale: se la property {@code app.security.ping-bypass-token}
 * è valorizzata, una richiesta con header {@code X-Ping-Token} (o, in
 * fallback, query param {@code token}) corrispondente — confronto constant-
 * time via {@link MessageDigest#isEqual(byte[], byte[])} — bypassa
 * completamente il rate limit, senza incrementare il bucket. Se la property
 * è vuota/non settata il bypass è disabilitato (fail-safe).
 */
@Component
public class PingRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PingRateLimitFilter.class);

    private final int max;
    private final long windowMs;
    private final boolean trustForwardedHeader;
    /** Bytes del segreto di bypass, oppure {@code null} se non configurato (bypass disabilitato). */
    private final byte[] bypassBytes;
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public PingRateLimitFilter(
            @Value("${app.security.ping-rate-max-per-minute:600}") int maxPerWindow,
            @Value("${app.security.ping-rate-window-seconds:60}") int windowSeconds,
            @Value("${app.security.trust-x-forwarded-for:false}") boolean trustForwardedHeader,
            @Value("${app.security.ping-bypass-token:}") String bypassToken) {
        this.max = Math.max(1, maxPerWindow);
        this.windowMs = Math.max(1L, windowSeconds) * 1000L;
        this.trustForwardedHeader = trustForwardedHeader;
        this.bypassBytes = (bypassToken != null && !bypassToken.isBlank())
                ? bypassToken.getBytes(StandardCharsets.UTF_8)
                : null;
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
        if (isBypassValid(request)) {
            if (log.isDebugEnabled()) {
                log.debug("Ping bypass granted for IP {}", ip);
            }
            filterChain.doFilter(request, response);
            return;
        }
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

    /**
     * Verifica se la richiesta presenta un token di bypass valido.
     * Cerca prima l'header {@code X-Ping-Token} (preferito, non finisce nei
     * log di accesso), poi fallback sul parametro di query {@code token}
     * (compatibilità con cron job che non supportano header custom).
     * Confronto constant-time; ritorna {@code false} anche se il bypass è
     * disabilitato (property vuota).
     */
    private boolean isBypassValid(@NonNull HttpServletRequest request) {
        if (bypassBytes == null) {
            return false;
        }
        String provided = request.getHeader("X-Ping-Token");
        if (provided == null || provided.isBlank()) {
            provided = request.getParameter("token");
        }
        if (provided == null || provided.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8), bypassBytes);
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
