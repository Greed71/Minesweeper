package com.minesweeper.auth.service;

import com.minesweeper.auth.dto.AuthResponseBody;
import com.minesweeper.auth.model.RefreshToken;
import com.minesweeper.auth.model.User;
import com.minesweeper.auth.repository.RefreshTokenRepo;
import com.minesweeper.auth.repository.UserRepo;
import com.minesweeper.security.JwtService;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Service layer per logica di autenticazione: login, register, refresh, logout. */
@Service
public class AuthService {

    // Hash BCrypt valido generato staticamente per mitigare timing attack.
    // Non corrisponde a nessuna password reale.
    private static final String DUMMY_BCRYPT_HASH;
    static {
        DUMMY_BCRYPT_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12)
                .encode("dummy-timing-mitigation-hash");
    }

    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final JwtService jwtService;

    public AuthService(UserRepo userRepo, RefreshTokenRepo refreshTokenRepo, JwtService jwtService) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.jwtService = jwtService;
    }

    public boolean usernameExists(String username) {
        return userRepo.findByUsername(username) != null;
    }

    public boolean mailExists(String mail) {
        return userRepo.findByMail(mail) != null;
    }

    public AuthResponseBody register(String username, String rawPassword, String mail,
                                      org.springframework.security.crypto.password.PasswordEncoder encoder) {
        User user = new User(username, rawPassword, mail);
        user.setPassword(encoder.encode(user.getPassword()));
        User saved = userRepo.save(user);
        return buildAuthResponse(saved);
    }

    public AuthResponseBody login(String mail, String rawPassword,
                                   org.springframework.security.crypto.password.PasswordEncoder encoder) {
        User found = userRepo.findByMail(mail);
        if (found != null && encoder.matches(rawPassword, found.getPassword())) {
            return buildAuthResponse(found);
        }
        // Timing attack mitigation: esegui BCrypt su hash fittizio quando l'utente non esiste
        if (found == null) {
            encoder.matches(rawPassword, DUMMY_BCRYPT_HASH);
        }
        return null;
    }

    public String changePassword(String username, String currentPassword, String newPassword,
                                  org.springframework.security.crypto.password.PasswordEncoder encoder) {
        User found = userRepo.findByUsername(username);
        if (found == null) return "Utente non trovato";
        if (!encoder.matches(currentPassword, found.getPassword())) return "Password attuale errata";
        found.setPassword(encoder.encode(newPassword));
        userRepo.save(found);
        // Revoca tutti i refresh token: forzano il re-login dopo cambio password
        refreshTokenRepo.deleteByUser(found);
        return null; // success
    }

    public AuthResponseBody refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepo.findByToken(refreshTokenValue);
        if (stored == null || stored.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }
        refreshTokenRepo.delete(stored);
        return buildAuthResponse(stored.getUser());
    }

    public void logout(String username) {
        User user = userRepo.findByUsername(username);
        if (user != null) {
            refreshTokenRepo.deleteByUser(user);
        }
    }

    public User getCurrentUser(String username) {
        return userRepo.findByUsername(username);
    }

    private AuthResponseBody buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken();
        RefreshToken rt = new RefreshToken(
                refreshToken, user,
                Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()));
        refreshTokenRepo.save(rt);
        return new AuthResponseBody(accessToken, refreshToken, user);
    }
}
