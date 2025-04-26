package com;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClickPosition {
    private int row;
    private int col;

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
}
