package com;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;
import io.github.cdimascio.dotenv.Dotenv;

public class JwtUtils {

    private static final String SECRET_KEY = Dotenv.load().get("SECRET_KEY");

    // Metodo per generare il JWT
    public static String generateToken(String username) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);  // Usa HMAC SHA256 per la firma
        return JWT.create()
                .withSubject(username)  // Imposta il nome utente come subject
                .withIssuedAt(new Date())  // Data di emissione
                .withExpiresAt(new Date(System.currentTimeMillis() + 86400000))  // Scadenza del token
                .sign(algorithm);  // Firma il token con l'algoritmo
    }

    // Metodo per validare e ottenere il nome utente dal JWT
    public static String validateTokenAndExtractUsername(String token) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);  // Usa la stessa chiave per validare
        return JWT.require(algorithm)
                .build()
                .verify(token)  // Verifica il token
                .getSubject();  // Estrai il nome utente dal claim "sub"
    }
}
