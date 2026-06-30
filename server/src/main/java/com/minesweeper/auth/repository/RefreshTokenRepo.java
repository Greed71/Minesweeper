package com.minesweeper.auth.repository;

import com.minesweeper.auth.model.RefreshToken;
import com.minesweeper.auth.model.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    RefreshToken findByToken(String token);
    List<RefreshToken> findByUser(User user);
    void deleteByUser(User user);
}
