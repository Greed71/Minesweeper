package com;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClickPosition {
    @Min(0)
    @Max(99)
    private int row;
    @Min(0)
    @Max(99)
    private int col;
    @NotBlank
    @Size(max = 128)
    private String sessionId;

    public ClickPosition() {}

    @JsonProperty("row")
    public int getRow() {
        return row;
    }

    @JsonProperty("row")
    public void setRow(int row) {
        this.row = row;
    }

    @JsonProperty("col")
    public int getCol() {
        return col;
    }

    @JsonProperty("col")
    public void setCol(int col) {
        this.col = col;
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
