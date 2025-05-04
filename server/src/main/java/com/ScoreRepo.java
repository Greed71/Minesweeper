package com;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreRepo extends JpaRepository<Score, Long> {

    List<Score> findByUser(User user);
    List<Score> findTop10ByOrderByPointsDesc(); // per la leaderboard

}
