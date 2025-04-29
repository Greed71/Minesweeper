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
    private boolean gameOver = false;
    private boolean gameWon = false;

    // Inizia una nuova partita
    public void startNewGame(int rows, int cols, int mineCount) {
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.board = new int[rows][cols];
        this.revealed = new boolean[rows][cols];
        this.flagged = new boolean[rows][cols];
        initializeBoard();
        placeMines(-1, -1); // Posiziona le mine senza protezione iniziale
        calculateNumbers();
        minesPlaced = true;
    }

    // Prepara una griglia vuota senza mine
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
        gameWon = false; // Resetta stato di vittoria
    }

    // Inizializza la griglia (tutti i valori a 0)
    private void initializeBoard() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                board[row][col] = 0;
                revealed[row][col] = false;
                flagged[row][col] = false;
            }
        }
    }

    // Rivelazione di una cella
    public void revealCell(int row, int col) {
        if (!isInBounds(row, col) || revealed[row][col] || gameOver || flagged[row][col]) 
            return;

        // Posiziona le mine se non sono ancora state posizionate
        if (!minesPlaced) {
            do {
                initializeBoard();
                placeMines(row, col);
                calculateNumbers();
            } while (board[row][col] != 0);
            minesPlaced = true;
        }

        revealed[row][col] = true;

        // Se cliccata una mina, il gioco finisce
        if (board[row][col] == -1) {
            gameOver = true;
            System.out.println("Game Over! Hai colpito una mina.");
            return;
        }

        // Se la cella è vuota, scopri le celle limitrofe
        if (board[row][col] == 0) {
            for (int r = -1; r <= 1; r++) {
                for (int c = -1; c <= 1; c++) {
                    if (!(r == 0 && c == 0)) {
                        revealCell(row + r, col + c);
                    }
                }
            }
        }

        // Verifica se il gioco è stato vinto
        if (checkWin()) {
            gameWon = true;
            System.out.println("Congratulazioni! Hai vinto!");
        }
    }

    // Restituisce la griglia visibile (le celle rivelate)
    public Integer[][] getVisibleBoard() {
        Integer[][] visible = new Integer[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                visible[row][col] = revealed[row][col] ? board[row][col] : null;
            }
        }
        return visible;
    }

    // Posiziona le mine sulla griglia, evitando la cella iniziale
    private void placeMines(int safeRow, int safeCol) {
        Random rand = new Random();
        int placedMines = 0;
        while (placedMines < mineCount) {
            int row = rand.nextInt(rows);
            int col = rand.nextInt(cols);

            if ((row == safeRow && col == safeCol) || board[row][col] == -1) {
                continue;
            }

            board[row][col] = -1; // Posiziona una mina
            placedMines++;
        }
    }

    // Calcola i numeri delle celle in base al numero di mine adiacenti
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

    // Verifica se una cella è all'interno dei limiti della griglia
    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    // Verifica se il gioco è finito
    public boolean isGameOver() {
        return gameOver;
    }

    // Verifica se il gioco è stato vinto
    public boolean isGameWon() {
        return gameWon;
    }

    // Aggiungi o rimuovi una bandiera su una cella
    public void flagCell(int row, int col) {
        if (isInBounds(row, col) && !revealed[row][col]) {
            flagged[row][col] = !flagged[row][col];  // Toglia o aggiungi la bandiera
        }
    }

    // Controlla se il gioco è vinto (tutte le celle senza mine rivelate)
    public boolean checkWin() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (board[row][col] != -1 && !revealed[row][col]) {
                    return false; // Se una cella non è rivelata e non è una mina, il gioco non è vinto
                }
            }
        }
        return true; // Tutte le celle senza mine sono rivelate, gioco vinto
    }
}
