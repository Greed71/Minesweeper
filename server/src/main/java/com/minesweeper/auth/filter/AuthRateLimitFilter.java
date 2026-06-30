package com.minesweeper.auth.filter;

import com.minesweeper.security.IpRequestThrottle;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Rate limiting su login, registrazione e endpoint di gioco (stesso IP, finestra fissa). */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final IpRequestThrottle throttle;
    private final boolean trustForwardedHeader;

    public AuthRateLimitFilter(IpRequestThrottle throttle,
            @Value("${app.security.trust-x-forwarded-for:false}") boolean trustForwardedHeader) {
        this.throttle = throttle;
        this.trustForwardedHeader = trustForwardedHeader;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        // GET /api/ping ha un rate limit dedicato in PingRateLimitFilter
        if (!"POST".equalsIgnoreCase(method)) {
            return true;
        }
        return !path.endsWith("/auth/login")
                && !path.endsWith("/auth/register")
                && !path.endsWith("/auth/refresh")
                && !path.endsWith("/api/genera")
                && !path.endsWith("/api/reveal")
                && !path.endsWith("/api/clic");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String ip = clientIp(request);
        if (!throttle.tryAcquire(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (PrintWriter w = response.getWriter()) {
                w.write("{\"error\":\"rate_limited\",\"message\":\"Troppi tentativi. Riprova più tardi.\"}");
            }
            return;
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
}
