package com;

import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class Minesweeper {

    private int rows;
    private int cols;
    private int mineCount;
    private int[][] board;
    private boolean[][] revealed;
    private boolean[][] flagged;
    private boolean minesPlaced = false;

    public void startNewGame(int rows, int cols, int mineCount) {
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.board = new int[rows][cols];
        this.revealed = new boolean[rows][cols];
        this.flagged = new boolean[rows][cols];
        initializeBoard();
        placeMines(-1, -1); // senza protezione
        calculateNumbers();
        minesPlaced = true;
    }

    public void prepareEmptyBoard(int rows, int cols, int mineCount) {
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.board = new int[rows][cols];
        this.revealed = new boolean[rows][cols];
        this.flagged = new boolean[rows][cols];
        initializeBoard();
        minesPlaced = false;
    }

    private void initializeBoard() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                board[row][col] = 0;
                revealed[row][col] = false;
                flagged[row][col] = false;
            }
        }
    }

    public void revealCell(int row, int col) {
        if (!isInBounds(row, col) || revealed[row][col]) return;
    
        if (!minesPlaced) {
            do {
                initializeBoard();
                placeMines(row, col);
                calculateNumbers();
            } while (board[row][col] != 0);
            minesPlaced = true;
        }
    
        revealed[row][col] = true;
    
        if (board[row][col] == 0) {
            for (int r = -1; r <= 1; r++) {
                for (int c = -1; c <= 1; c++) {
                    if (!(r == 0 && c == 0)) {
                        revealCell(row + r, col + c);
                    }
                }
            }
        }
    }
    

    public Integer[][] getVisibleBoard() {
        Integer[][] visible = new Integer[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (revealed[row][col]) {
                    visible[row][col] = board[row][col];
                } else {
                    visible[row][col] = null;
                }
            }
        }
        return visible;
    }

    private void placeMines(int safeRow, int safeCol) {
        Random rand = new Random();
        int placedMines = 0;
        while (placedMines < mineCount) {
            int row = rand.nextInt(rows);
            int col = rand.nextInt(cols);

            if ((row == safeRow && col == safeCol) || board[row][col] == -1) {
                continue;
            }

            board[row][col] = -1;
            placedMines++;
        }
    }

    private void calculateNumbers() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (board[row][col] == -1) continue;
                int count = 0;
                for (int r = -1; r <= 1; r++) {
                    for (int c = -1; c <= 1; c++) {
                        if (isInBounds(row + r, col + c) && board[row + r][col + c] == -1) {
                            count++;
                        }
                    }
                }
                board[row][col] = count;
            }
        }
    }

    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}
