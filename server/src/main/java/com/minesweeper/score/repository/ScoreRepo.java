package com.minesweeper.score.repository;

import com.minesweeper.auth.model.User;
import com.minesweeper.score.model.Score;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreRepo extends JpaRepository<Score, Long> {
    List<Score> findByUser(User user);
    List<Score> findTop10ByOrderByPointsDesc();
    List<Score> findTop10ByDifficultyOrderByPointsAsc(String difficulty);
    List<Score> findByUserOrderByPointsDesc(User user);
}