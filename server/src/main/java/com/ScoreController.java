package com;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/score")
@CrossOrigin(origins = "http://localhost:5173")
public class ScoreController {

    private final ScoreRepo scoreRepo;
    private final UserRepo userRepo;

    public ScoreController(ScoreRepo scoreRepo, UserRepo userRepo) {
        this.scoreRepo = scoreRepo;
        this.userRepo = userRepo;
    }

    private String translateDifficulty(String input) {
        return switch (input.toLowerCase()) {
            case "easy" -> "facile";
            case "medium" -> "medio";
            case "hard" -> "difficile";
            default -> input;
        };
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveScore(@RequestBody ScoreRequest req) {
        User user = userRepo.findByUsername(req.getUsername());
        if (user == null) {
            return ResponseEntity.badRequest().body("Utente non trovato");
        }
        scoreRepo.save(new Score(req.getPoints(), req.getDifficulty(), user));
        return ResponseEntity.ok("Punteggio salvato!");
    }

    @GetMapping("/leaderboard")
    public List<Score> leaderboard(@RequestParam String difficulty) {
        difficulty = translateDifficulty(difficulty);
        return scoreRepo.findTop10ByDifficultyOrderByPointsAsc(difficulty);
    }

    @GetMapping("/user")
    @CrossOrigin(origins = "http://localhost:5173")
    public List<Score> getUserScores(@RequestParam String username) {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            return List.of(); // oppure puoi restituire 404
        }
        return scoreRepo.findByUserOrderByPointsDesc(user);
    }
}
