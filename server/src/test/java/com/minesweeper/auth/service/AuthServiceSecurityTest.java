package com.minesweeper.auth.service;

import com.minesweeper.auth.model.User;
import com.minesweeper.auth.repository.RefreshTokenRepo;
import com.minesweeper.auth.repository.UserRepo;
import com.minesweeper.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test di sicurezza deterministici per AuthService:
 * - Timing attack mitigation (BCrypt sempre eseguito anche per utenti inesistenti)
 * - Revoca refresh token dopo cambio password
 * - Anti-enumerazione: login fallito non distingue tra utente inesistente e password errata
 */
class AuthServiceSecurityTest {

    private UserRepo userRepo;
    private RefreshTokenRepo refreshTokenRepo;
    private JwtService jwtService;
    private PasswordEncoder encoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepo.class);
        refreshTokenRepo = mock(RefreshTokenRepo.class);
        jwtService = mock(JwtService.class);
        encoder = mock(PasswordEncoder.class);
        authService = new AuthService(userRepo, refreshTokenRepo, jwtService);
    }

    @Test
    void loginWithNonexistentUserStillCallsEncoder() {
        when(userRepo.findByMail("nonexistent@test.com")).thenReturn(null);

        var result = authService.login("nonexistent@test.com", "anyPassword", encoder);

        assertNull(result, "Login should fail for nonexistent user");
        // Verifica che encoder.matches() sia stato chiamato esattamente 1 volta
        // (sul dummy hash per mitigare timing attack)
        verify(encoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    void loginWithWrongPasswordCallsEncoderExactlyOnce() {
        User user = new User("test", "hashedPassword", "test@test.com");
        when(userRepo.findByMail("test@test.com")).thenReturn(user);
        when(encoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        var result = authService.login("test@test.com", "wrongPassword", encoder);

        assertNull(result, "Login should fail with wrong password");
        // Verifica che encoder sia chiamato esattamente 1 volta (per la password reale)
        verify(encoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    void loginSuccessCallsEncoderAndReturnsToken() {
        User user = new User("test", "hashedPassword", "test@test.com");
        when(userRepo.findByMail("test@test.com")).thenReturn(user);
        when(encoder.matches("correct", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken("test")).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtService.getRefreshTtlSeconds()).thenReturn(604800L);

        var result = authService.login("test@test.com", "correct", encoder);

        assertNotNull(result, "Login should succeed");
        assertEquals("access-token", result.getToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(user, result.getUser());
    }

    @Test
    void changePasswordRevokesRefreshTokens() {
        User user = new User("test", "hashedOldPassword", "test@test.com");
        when(userRepo.findByUsername("test")).thenReturn(user);
        when(encoder.matches(eq("oldPassword"), anyString())).thenReturn(true);
        when(encoder.encode("newPassword")).thenReturn("hashedNewPassword");
        when(userRepo.save(any())).thenReturn(user);

        String error = authService.changePassword("test", "oldPassword", "newPassword", encoder);

        assertNull(error, "Password change should succeed");
        verify(refreshTokenRepo, times(1)).deleteByUser(user);
    }

    @Test
    void changePasswordFailsWithWrongPasswordAndDoesNotRevokeTokens() {
        User user = new User("test", "hashedCorrectPassword", "test@test.com");
        when(userRepo.findByUsername("test")).thenReturn(user);
        when(encoder.matches(eq("wrongPassword"), anyString())).thenReturn(false);

        String error = authService.changePassword("test", "wrongPassword", "newPassword", encoder);

        assertEquals("Password attuale errata", error);
        verify(refreshTokenRepo, never()).deleteByUser(any());
    }

    @Test
    void loginWithNonexistentUserVsWrongPasswordIndistinguishableViaEncoderCalls() {
        // Utente inesistente
        when(userRepo.findByMail("nonexistent@test.com")).thenReturn(null);
        authService.login("nonexistent@test.com", "pwd", encoder);
        verify(encoder, atLeastOnce()).matches(anyString(), anyString());
        // Reset mock
        reset(encoder);

        // Password sbagliata
        User user = new User("test", "hashedPassword", "test@test.com");
        when(userRepo.findByMail("test@test.com")).thenReturn(user);
        when(encoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);
        authService.login("test@test.com", "wrongPassword", encoder);
        verify(encoder, atLeastOnce()).matches(anyString(), anyString());
        // Entrambi i casi chiamano encoder.matches() almeno 1 volta → timing indistinguibile
    }
}
