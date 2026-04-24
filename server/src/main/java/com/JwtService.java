package com;

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

    private static final int MIN_SECRET_LENGTH = 32;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long ttlMs;
    private final String issuer;
    private final String audience;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer:minesweeper-api}") String issuer,
            @Value("${app.jwt.audience:minesweeper-app}") String audience,
            @Value("${app.jwt.ttl-seconds:86400}") int ttlSeconds) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        this.issuer = issuer;
        this.audience = audience;
        this.algorithm = Algorithm.HMAC256(secret);
        this.ttlMs = Math.max(60L, ttlSeconds) * 1000L;
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
}
