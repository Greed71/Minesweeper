package com.minesweeper.auth.controller;

import com.minesweeper.auth.dto.AuthResponseBody;
import com.minesweeper.auth.dto.LoginRequest;
import com.minesweeper.auth.dto.PasswordChangeRequest;
import com.minesweeper.auth.dto.RefreshTokenRequest;
import com.minesweeper.auth.dto.RegisterRequest;
import com.minesweeper.auth.model.User;
import com.minesweeper.auth.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Controller sottile per autenticazione: logica di business delegata ad AuthService. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean containsInvalidChars(String input) {
        return input != null && input.matches(".*[\"'`;*\\\\].*");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        if (containsInvalidChars(request.getPassword()) || containsInvalidChars(request.getUsername())) {
            return ResponseEntity.badRequest().body("Invalid characters in input.");
        }
        if (authService.usernameExists(request.getUsername())
                || authService.mailExists(request.getMail())) {
            // Messaggio generico: non rivelare quale campo è già in uso (anti-enumerazione)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username o mail già in uso");
        }
        return ResponseEntity.ok(authService.register(
                request.getUsername(), request.getPassword(), request.getMail(), passwordEncoder));
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal @NonNull String username,
            @RequestBody @Valid PasswordChangeRequest req) {
        if (containsInvalidChars(req.getNewPassword())) {
            return ResponseEntity.badRequest().body("Invalid characters in new password.");
        }
        String error = authService.changePassword(username, req.getCurrentPassword(), req.getNewPassword(), passwordEncoder);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        return ResponseEntity.ok("Password aggiornata con successo");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        AuthResponseBody response = authService.login(request.getMail(), request.getPassword(), passwordEncoder);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali errate");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<User> getCurrentUser(
            @AuthenticationPrincipal @NonNull String username) {
        User user = authService.getCurrentUser(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody @Valid RefreshTokenRequest req) {
        AuthResponseBody response = authService.refresh(req.getRefreshToken());
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token non valido o scaduto.");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal @NonNull String username) {
        authService.logout(username);
        return ResponseEntity.ok("Logout effettuato.");
    }
}
