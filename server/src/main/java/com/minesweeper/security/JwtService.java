package com.minesweeper.security;

import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

/** Crea e verifica i JWT (HMAC, issuer e audience obbligatori). */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String DEFAULT_SECRET = "change-me-in-production-use-openssl-rand-hex-32";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long ttlMs;
    private final long refreshTtlSeconds;
    private final String issuer;
    private final String audience;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer:minesweeper-api}") String issuer,
            @Value("${app.jwt.audience:minesweeper-app}") String audience,
            @Value("${app.jwt.ttl-seconds:900}") int ttlSeconds,
            @Value("${app.jwt.refresh-ttl-seconds:604800}") int refreshTtlSeconds) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        if (DEFAULT_SECRET.equals(secret)) {
            log.error("============================================================");
            log.error("  WARNING: JWT secret is still the DEFAULT value!");
            log.error("  Set JWT_SECRET env var to a random 64-char hex string.");
            log.error("  Generate: openssl rand -hex 32");
            log.error("============================================================");
        }
        this.issuer = issuer;
        this.audience = audience;
        this.algorithm = Algorithm.HMAC256(secret);
        this.ttlMs = Math.max(60L, ttlSeconds) * 1000L;
        this.refreshTtlSeconds = Math.max(60L, refreshTtlSeconds);
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build();
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return JWT.create()
                .withSubject(username)
                .withIssuer(issuer)
                .withAudience(audience)
                .withIssuedAt(new java.util.Date(now))
                .withExpiresAt(new java.util.Date(now + ttlMs))
                .sign(algorithm);
    }

    public String validateAndGetSubject(String token) throws JWTVerificationException {
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getSubject();
    }

    /** Genera un refresh token (stringa casuale Base64url, 256 bit). */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Durata refresh token in secondi (per calcolare la scadenza nel DB). */
    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }
}
