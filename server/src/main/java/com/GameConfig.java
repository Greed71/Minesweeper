package com;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GameConfig {
    private int row;
    private int columns;
    private int mines;
    private String sessionId;

    public GameConfig() {
    }

    // Getter e Setter
    @JsonProperty("row") // Mappa il nome 'row' del JSON al campo 'row' nella classe
    public int getRows() {
        return row;
    }

    @JsonProperty("row")
    public void setRows(int row) {
        this.row = row;
    }

    @JsonProperty("col") // Mappa il nome 'columns' del JSON al campo 'columns' nella classe
    public int getCols() {
        return columns;
    }

    @JsonProperty("col")
    public void setCols(int columns) {
        this.columns = columns;
    }

    @JsonProperty("mines") // Mappa il nome 'columns' del JSON al campo 'columns' nella classe
    public int getMines() {
        return mines;
    }

    @JsonProperty("mines")
    public void setMines(int mines) {
        this.mines = mines;
    }

    @JsonProperty("sessionId") // Mappa il nome 'columns' del JSON al campo 'columns' nella classe
    public String getSessionId() {
        return sessionId;
    }

    @JsonProperty("sessionId")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}