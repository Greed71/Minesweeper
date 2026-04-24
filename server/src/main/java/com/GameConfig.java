package com;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GameConfig {
    @Min(1)
    @Max(100)
    private int row;
    @Min(1)
    @Max(100)
    private int columns;
    @Min(1)
    @Max(10_000)
    private int mines;
    @NotBlank
    @Size(max = 128)
    private String sessionId;

    public GameConfig() {}

    @JsonProperty("row")
    public int getRows() {
        return row;
    }

    @JsonProperty("row")
    public void setRows(int row) {
        this.row = row;
    }

    @JsonProperty("col")
    public int getCols() {
        return columns;
    }

    @JsonProperty("col")
    public void setCols(int columns) {
        this.columns = columns;
    }

    @JsonProperty("mines")
    public int getMines() {
        return mines;
    }

    @JsonProperty("mines")
    public void setMines(int mines) {
        this.mines = mines;
    }

    @JsonProperty("sessionId")
    public String getSessionId() {
        return sessionId;
    }

    @JsonProperty("sessionId")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
