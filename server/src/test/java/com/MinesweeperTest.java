package com;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class MinesweeperTest {

    @Test
    void firstRevealPlacesExpectedMineCountAndStaysInPlay() {
        Minesweeper g = new Minesweeper();
        g.setRandomForTest(new Random(42L));
        g.prepareEmptyBoard(5, 5, 5);
        g.revealCell(0, 0);
        assertEquals(5, g.countMinesOnBoard());
        assertFalse(g.isGameOver());
    }

    @Test
    void hittingFirstMineFoundEndsGame() {
        Minesweeper g = new Minesweeper();
        g.setRandomForTest(new Random(42L));
        g.prepareEmptyBoard(4, 4, 3);
        g.revealCell(0, 0);
        outer:
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (g.cellValueAt(r, c) == -1) {
                    g.revealCell(r, c);
                    break outer;
                }
            }
        }
        assertTrue(g.isGameOver());
    }

    @Test
    void canWinSmallBoard() {
        Minesweeper g = new Minesweeper();
        g.setRandomForTest(new Random(7L));
        g.prepareEmptyBoard(2, 2, 1);
        g.revealCell(0, 0);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                if (g.cellValueAt(r, c) != -1) {
                    g.revealCell(r, c);
                }
            }
        }
        assertTrue(g.isGameWon());
    }

    @Test
    void outOfBoundsRevealIsNoOp() {
        Minesweeper g = new Minesweeper();
        g.setRandomForTest(new Random(1L));
        g.prepareEmptyBoard(3, 3, 1);
        g.revealCell(99, 99);
        g.revealCell(0, 0);
        assertNotEquals(0, g.countMinesOnBoard());
    }
}
