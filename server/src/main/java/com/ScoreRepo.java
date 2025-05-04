package com;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreRepo extends JpaRepository<Score, Long> {
    List<Score> findByUser(User user);
    List<Score> findTop10ByOrderByPointsDesc();
    List<Score> findTop10ByDifficultyOrderByPointsAsc(String difficulty);
    List<Score> findByUserOrderByPointsDesc(User user);
}