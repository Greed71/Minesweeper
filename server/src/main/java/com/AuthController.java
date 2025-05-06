package com;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.exceptions.JWTVerificationException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "${FRONT}", allowCredentials = "true")
public class AuthController {

    private final UserRepo userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepo userRepository) {
        this.userRepository = userRepository;
    }

    private boolean containsInvalidChars(String input) {
        return input != null && input.matches(".*[\"'`;*\\\\].*");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username già in uso");
        }
        if (containsInvalidChars(user.getPassword()) || containsInvalidChars(user.getUsername())) {
            return ResponseEntity.badRequest().body("Invalid characters in input.");
        }
        if (userRepository.findByMail(user.getMail()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Mail già in uso");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        saved.setPassword(null); // sicurezza
        return ResponseEntity.ok(saved); // ritorna l'utente registrato
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest req, HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido");
        }

        token = token.substring(7);  // Rimuovi "Bearer " dal token

        try {
            String username = JwtUtils.validateTokenAndExtractUsername(token);

            User found = userRepository.findByUsername(username);
            if (found != null) {
                // Verifica la password attuale
                if (!encoder.matches(req.getCurrentPassword(), found.getPassword())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password attuale errata");
                }

                // Hash della nuova password prima di salvarla
                found.setPassword(encoder.encode(req.getNewPassword()));  
                userRepository.save(found);
                return ResponseEntity.ok("Password aggiornata con successo");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non trovato");
            }
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        User found = userRepository.findByMail(user.getMail());
        if (found == null || !encoder.matches(user.getPassword(), found.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali errate");
        }

        // Genera un token JWT e restituiscilo
        String token = JwtUtils.generateToken(found.getUsername());
        return ResponseEntity.ok(Map.of("token", token));  // Restituisce il token
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido");
        }

        token = token.substring(7);  // Rimuovi "Bearer "
        String username = JwtUtils.validateTokenAndExtractUsername(token);

        User user = userRepository.findByUsername(username);
        if (user != null) {
            return ResponseEntity.ok(user);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non trovato");
    }
}
