package com.minesweeper.game.engine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Campo minato: una istanza per partita. Non condividere la stessa istanza tra
 * più sessioni utente in parallelo.
 */
public class Minesweeper {

    private static final Logger log = LoggerFactory.getLogger(Minesweeper.class);
    private static final int MAX_PLACEMENT_ATTEMPTS = 1_000_000;
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
            if (!placeMinesAndNumbers(row, col)) {
                gameOver = true;
                log.error("Impossibile piazzare le mine: griglia {}x{} mine={}", rows, cols, mineCount);
                return;
            }
            minesPlaced = true;
        }

        if (board[row][col] == -1) {
            revealed[row][col] = true;
            gameOver = true;
            log.debug("Mina colpita: row={} col={}", row, col);
            return;
        }

        floodFillIterative(row, col);

        if (checkWin()) {
            gameWon = true;
            log.debug("Partita vinta");
        }
    }

    /** Flood-fill iterativo con coda: evita StackOverflow su griglie grandi.
     *  Non rivela le mine durante l'espansione (comportamento standard). */
    private void floodFillIterative(int startRow, int startCol) {
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});
        revealed[startRow][startCol] = true;

        while (!queue.isEmpty()) {
            int[] cell = queue.pollFirst();
            int r = cell[0];
            int c = cell[1];

            if (board[r][c] != 0) {
                continue;
            }

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = r + dr;
                    int nc = c + dc;
                    if (!isInBounds(nr, nc) || revealed[nr][nc] || flagged[nr][nc] || board[nr][nc] == -1)
                        continue;
                    revealed[nr][nc] = true;
                    if (board[nr][nc] == 0) {
                        queue.addLast(new int[]{nr, nc});
                    }
                }
            }
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
        int attempts = 0;
        int maxPlaceable = rows * cols - 1;

        if (mineCount > maxPlaceable) {
            mineCount = maxPlaceable;
            log.warn("mineCount ridotto a {} (max piazzabili su {}x{})", maxPlaceable, rows, cols);
        }

        while (placedMines < mineCount && attempts < MAX_PLACEMENT_ATTEMPTS) {
            int row = random.nextInt(rows);
            int col = random.nextInt(cols);
            attempts++;

            if ((row == safeRow && col == safeCol) || board[row][col] == -1) {
                continue;
            }

            board[row][col] = -1;
            placedMines++;
        }

        if (placedMines < mineCount) {
            log.warn("Piazzate solo {}/{} mine dopo {} tentativi", placedMines, mineCount, attempts);
        }
    }

    /**
     * Posiziona mine e numeri garantendo che il PRIMO click dell'utente apra sempre
     * più di una cella. Regole in ordine:
     *   1. La cella su cui si clicca non deve contenere una mina.
     *   2. Deve valere 0 (zero mine adiacenti): il flood-fill in revealCell
     *      parte solo da celle 0 e rivela l'intera regione connessa, garantendo
     *      UX soddisfacente (primo click sempre esplosivo invece di una singola '1').
     * Su board patologicamente piccole/dense (es. 2x2 con 1 mine: nessuna cella 0
     * può esistere) si fa fallback all'ultimo tentativo che ha safeCell != -1.
     */
    private boolean placeMinesAndNumbers(int safeRow, int safeCol) {
        int maxAttempts = 100;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            initializeBoard();
            placeMines(safeRow, safeCol);
            calculateNumbers();
            int safeCell = board[safeRow][safeCol];
            if (safeCell == -1) {
                continue; // safe cell è una mina: riprova
            }
            if (safeCell == 0) {
                return true; // safe cell è una '0': il flood-fill rivelerà la regione
            }
            // safeCell è 1..8: riprova finché non cade su uno zero.
        }
        log.warn(
            "placeMinesAndNumbers: impossibile trovare safeCell=0 dopo {} tentativi ({}x{}, mine={}). "
            + "Fallback: il primo click aprirà una sola cella.",
            maxAttempts, rows, cols, mineCount
        );
        return true; // fallback: l'ultimo tentativo aveva safeCell = numero, non mina
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
