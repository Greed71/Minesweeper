package com.minesweeper.score.controller;

import com.minesweeper.score.dto.ScoreRequest;
import com.minesweeper.score.model.Score;
import com.minesweeper.score.service.ScoreService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/** Controller sottile per punteggi: tutta la logica è in ScoreService. */
@RestController
@RequestMapping("/score")
@Validated
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveScore(
            @AuthenticationPrincipal @NonNull String username, @RequestBody @Valid ScoreRequest req) {
        Score saved = scoreService.saveScore(username, req.getPoints(), req.getDifficulty());
        if (saved == null) {
            return ResponseEntity.badRequest().body("Utente non trovato");
        }
        return ResponseEntity.ok("Punteggio salvato!");
    }

    @GetMapping("/leaderboard")
    public List<Score> leaderboard(@RequestParam @Pattern(regexp = "^(easy|medium|hard)$", message = "Invalid difficulty") String difficulty) {
        return scoreService.getLeaderboard(difficulty);
    }

    @GetMapping("/user")
    public List<Score> getUserScores(@AuthenticationPrincipal @NonNull String username) {
        return scoreService.getUserScores(username);
    }
}
