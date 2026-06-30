package com.minesweeper.score.service;

import com.minesweeper.auth.model.User;
import com.minesweeper.auth.repository.UserRepo;
import com.minesweeper.score.model.Score;
import com.minesweeper.score.repository.ScoreRepo;

import java.util.List;

import org.springframework.stereotype.Service;

/** Service layer per la logica dei punteggi. Separa business logic dal controller HTTP. */
@Service
public class ScoreService {

    private final ScoreRepo scoreRepo;
    private final UserRepo userRepo;

    public ScoreService(ScoreRepo scoreRepo, UserRepo userRepo) {
        this.scoreRepo = scoreRepo;
        this.userRepo = userRepo;
    }

    public Score saveScore(String username, int points, String difficulty) {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            return null;
        }
        return scoreRepo.save(new Score(points, difficulty, user));
    }

    public List<Score> getLeaderboard(String difficulty) {
        return scoreRepo.findTop10ByDifficultyOrderByPointsAsc(difficulty);
    }

    public List<Score> getUserScores(String username) {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            return List.of();
        }
        return scoreRepo.findByUserOrderByPointsDesc(user);
    }
}
