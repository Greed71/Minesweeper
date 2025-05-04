package com;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/save")
    public String saveScore(@RequestParam String username, @RequestParam int points) {
        User user = userRepo.findByUsername(username);
        if (user == null) return "Utente non trovato";

        scoreRepo.save(new Score(points, user));
        return "Punteggio salvato!";
    }

    @GetMapping("/user")
    public List<Score> getUserScores(@RequestParam String username) {
        User user = userRepo.findByUsername(username);
        if (user == null) return List.of();
        return scoreRepo.findByUser(user);
    }

    @GetMapping("/leaderboard")
    public List<Score> leaderboard() {
        return scoreRepo.findTop10ByOrderByPointsDesc();
    }
}
