package com;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Troppe richieste a login o registrazione: stesso indirizzo IP, finestra fissa. */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final IpRequestThrottle throttle;

    public AuthRateLimitFilter(IpRequestThrottle throttle) {
        this.throttle = throttle;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.endsWith("/auth/login") && !path.endsWith("/auth/register");
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

    private static String clientIp(HttpServletRequest request) {
        String h = request.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) {
            return h.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
