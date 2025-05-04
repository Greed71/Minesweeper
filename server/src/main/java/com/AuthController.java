package com;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
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
    public ResponseEntity<?> login(@RequestBody User user, HttpServletRequest request) {
        System.out.println("Tentativo login per: " + user.getMail());

        User found = userRepository.findByMail(user.getMail());
        if (found == null) {
            System.out.println("Utente non trovato");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali errate");
        }

        if (encoder.matches(user.getPassword(), found.getPassword())) {
            request.getSession(true).setAttribute("user", found); // salva utente in sessione
            found.setPassword(null); // sicurezza
            return ResponseEntity.ok(found);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali errate");
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non autenticato");
        }
        User user = (User) session.getAttribute("user");
        return ResponseEntity.ok(user);
    }

}
