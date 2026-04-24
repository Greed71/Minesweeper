package com;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Campo minato: una istanza per partita. Non condividere la stessa istanza tra
 * più sessioni utente in parallelo.
 */
public class Minesweeper {

    private static final Logger log = LoggerFactory.getLogger(Minesweeper.class);
    private Random random = new Random();
    private int rows;
    private int cols;
    private int mineCount;
    private int[][] board;
    private boolean[][] revealed;
    private boolean[][] flagged;
    private boolean minesPlaced = false;
    private boolean gameOver = false;
    private boolean gameWon = false;

    public void prepareEmptyBoard(int rows, int cols, int mineCount) {
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.board = new int[rows][cols];
        this.revealed = new boolean[rows][cols];
        this.flagged = new boolean[rows][cols];
        initializeBoard();
        minesPlaced = false;
        gameOver = false;
        gameWon = false;
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
        if (!isInBounds(row, col) || revealed[row][col] || gameOver || flagged[row][col]) {
            return;
        }

        if (!minesPlaced) {
            do {
                initializeBoard();
                placeMines(row, col);
                calculateNumbers();
            } while (board[row][col] != 0);
            minesPlaced = true;
        }

        revealed[row][col] = true;

        if (board[row][col] == -1) {
            gameOver = true;
            log.debug("Mina colpita: row={} col={}", row, col);
            return;
        }

        if (board[row][col] == 0) {
            for (int r = -1; r <= 1; r++) {
                for (int c = -1; c <= 1; c++) {
                    if (!(r == 0 && c == 0)) {
                        revealCell(row + r, col + c);
                    }
                }
            }
        }

        if (checkWin()) {
            gameWon = true;
            log.debug("Partita vinta");
        }
    }

    public Integer[][] getVisibleBoard() {
        Integer[][] visible = new Integer[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                visible[row][col] = revealed[row][col] ? board[row][col] : null;
            }
        }
        return visible;
    }

    private void placeMines(int safeRow, int safeCol) {
        int placedMines = 0;
        while (placedMines < mineCount) {
            int row = random.nextInt(rows);
            int col = random.nextInt(cols);

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
                if (board[row][col] == -1) {
                    continue;
                }
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

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public void flagCell(int row, int col) {
        if (isInBounds(row, col) && !revealed[row][col]) {
            flagged[row][col] = !flagged[row][col];
        }
    }

    public boolean checkWin() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (board[row][col] != -1 && !revealed[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Solo test: generatore fissato per replicare le partite. */
    void setRandomForTest(Random random) {
        this.random = random;
    }

    int countMinesOnBoard() {
        int n = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (board[row][col] == -1) {
                    n++;
                }
            }
        }
        return n;
    }

    int cellValueAt(int row, int col) {
        return board[row][col];
    }
}
