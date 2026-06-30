package com.minesweeper.score.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Punteggio: chi ha fatto la partita lo ricava dal token, non dal body. */
public class ScoreRequest {
    @Min(0)
    private int points;
    @NotBlank
    @Pattern(regexp = "^(easy|medium|hard)$", message = "difficulty must be easy, medium, or hard")
    @jakarta.validation.constraints.Size(max = 16)
    private String difficulty;

    public ScoreRequest() {}

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
