package com;

import org.springframework.stereotype.Service;

@Service
public class MinesweeperService {

    private Minesweeper minesweeper;

    public void startNewGame(int rows, int cols, int mineCount) {
        this.minesweeper = new Minesweeper(rows, cols, mineCount, true);
    }

    public void prepareEmptyBoard(int rows, int cols, int mineCount) {
        this.minesweeper = new Minesweeper(rows, cols, mineCount, false);
    }

    public int[][] getFullBoard() {
        return minesweeper.getBoard();
    }

    public Integer[][] firstClick(int row, int col) {
        minesweeper.placeMines(row, col);        // piazza le mine evitando la cella cliccata
        minesweeper.revealCell(row, col);        // rivela la cella (ed espande se è 0)
        return minesweeper.getVisibleBoard();    // ritorna solo le celle scoperte
    }
}
