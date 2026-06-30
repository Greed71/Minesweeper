package com.minesweeper.auth.repository;

import com.minesweeper.auth.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByMail(String mail);
}
