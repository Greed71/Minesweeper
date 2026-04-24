package com;

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

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepo userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    private boolean containsInvalidChars(String input) {
        return input != null && input.matches(".*[\"'`;*\\\\].*");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username già in uso");
        }
        if (containsInvalidChars(request.getPassword()) || containsInvalidChars(request.getUsername())) {
            return ResponseEntity.badRequest().body("Invalid characters in input.");
        }
        if (userRepository.findByMail(request.getMail()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Mail già in uso");
        }
        User user = new User(request.getUsername(), request.getPassword(), request.getMail());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getUsername());
        return ResponseEntity.ok(new AuthResponseBody(token, saved));
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal @NonNull String username,
            @RequestBody @Valid PasswordChangeRequest req) {
        User found = userRepository.findByUsername(username);
        if (found == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non trovato");
        }
        if (!passwordEncoder.matches(req.getCurrentPassword(), found.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password attuale errata");
        }
        found.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(found);
        return ResponseEntity.ok("Password aggiornata con successo");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        User found = userRepository.findByMail(request.getMail());
        if (found == null || !passwordEncoder.matches(request.getPassword(), found.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali errate");
        }
        String token = jwtService.generateToken(found.getUsername());
        return ResponseEntity.ok(new AuthResponseBody(token, found));
    }

    @GetMapping("/user")
    public ResponseEntity<User> getCurrentUser(
            @AuthenticationPrincipal @NonNull String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(user);
    }
}
