package com.minesweeper.auth.controller;

import com.minesweeper.auth.dto.LoginRequest;
import com.minesweeper.auth.dto.PasswordChangeRequest;
import com.minesweeper.auth.dto.RegisterRequest;
import com.minesweeper.auth.model.User;
import com.minesweeper.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di sicurezza per AuthController:
 * - Anti-enumerazione utenti in registrazione (messaggio generico)
 * - Caratteri pericolosi bloccati in registrazione e cambio password
 * - Response identiche per login fallito (utente inesistente vs password errata)
 */
class AuthControllerSecurityTest {

    private AuthService authService;
    private PasswordEncoder encoder;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        encoder = mock(PasswordEncoder.class);
        controller = new AuthController(authService, encoder);
    }

    @Test
    void registerUsernameExistsReturnsGenericMessage() {
        when(authService.usernameExists("existingUser")).thenReturn(true);
        when(authService.mailExists(anyString())).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("existingUser");
        req.setPassword("validPassword123");
        req.setMail("new@test.com");

        ResponseEntity<?> response = controller.register(req);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username o mail già in uso", response.getBody());
        // Verifica che il messaggio NON riveli se è username o mail il conflitto
        verify(authService, atLeastOnce()).usernameExists(anyString());
    }

    @Test
    void registerMailExistsReturnsSameGenericMessage() {
        when(authService.usernameExists(anyString())).thenReturn(false);
        when(authService.mailExists("existing@test.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("newUser");
        req.setPassword("validPassword123");
        req.setMail("existing@test.com");

        ResponseEntity<?> response = controller.register(req);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username o mail già in uso", response.getBody());
    }

    @Test
    void registerBothUsernameAndMailExistReturnsSameGenericMessage() {
        when(authService.usernameExists("existingUser")).thenReturn(true);
        when(authService.mailExists("existing@test.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("existingUser");
        req.setPassword("validPassword123");
        req.setMail("existing@test.com");

        ResponseEntity<?> response = controller.register(req);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username o mail già in uso", response.getBody());
    }

    @Test
    void registerBlocksInvalidCharacters() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("user\"name");
        req.setPassword("validPassword123");
        req.setMail("test@test.com");

        ResponseEntity<?> response = controller.register(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid characters in input.", response.getBody());
    }

    @Test
    void changePasswordBlocksInvalidCharactersInNewPassword() {
        var req = new PasswordChangeRequest("old", "new'pass");
        ResponseEntity<String> response = controller.changePassword("user", req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid characters in new password.", response.getBody());
    }

    @Test
    void loginFailsWithSameMessageRegardlessOfReason() {
        // Caso 1: utente inesistente
        when(authService.login(eq("nonexistent@test.com"), anyString(), any()))
                .thenReturn(null);
        ResponseEntity<?> r1 = controller.login(createLoginRequest("nonexistent@test.com", "pwd"));

        // Caso 2: password errata
        when(authService.login(eq("exists@test.com"), anyString(), any()))
                .thenReturn(null);
        ResponseEntity<?> r2 = controller.login(createLoginRequest("exists@test.com", "wrong"));

        // Entrambi restituiscono 401 con lo stesso messaggio
        assertEquals(HttpStatus.UNAUTHORIZED, r1.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, r2.getStatusCode());
        assertEquals("Credenziali errate", r1.getBody());
        assertEquals("Credenziali errate", r2.getBody());
    }

    private LoginRequest createLoginRequest(String mail, String password) {
        LoginRequest req = new LoginRequest();
        req.setMail(mail);
        req.setPassword(password);
        return req;
    }
}
