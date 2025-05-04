package com;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepo userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepo userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username già in uso");
        }
        if (userRepository.findByMail(user.getMail()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Mail già in uso");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        saved.setPassword(null); // sicurezza
        return ResponseEntity.ok(saved); // ✅ ritorna l'utente registrato
    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestBody PasswordChangeRequest req) {
        User found = userRepository.findByUsername(req.getUsername());
        if (found != null) {
            found.setPassword(encoder.encode(req.getNewPassword()));
            userRepository.save(found);
            return "Password cambiata con successo";
        }
        return "Utente non trovato";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        System.out.println("Tentativo login per: " + user.getMail());

        User found = userRepository.findByMail(user.getMail());
        if (found == null) {
            System.out.println("Utente non trovato");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali errate");
        }

        if (encoder.matches(user.getPassword(), found.getPassword())) {
            found.setPassword(null);
            return ResponseEntity.ok(found);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali errate");
    }

}