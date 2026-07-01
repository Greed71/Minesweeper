package com.minesweeper.game.engine;

import com.minesweeper.game.engine.Minesweeper;

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

    /**
     * Il primo click deve SEMPRE aprire più di una cella: la cella cliccata
     * viene vincolata a valere 0, così il flood-fill rivela l'intera regione.
     */
    @Test
    void firstClickOpensAtLeastTwoCells() {
        Minesweeper g = new Minesweeper();
        g.setRandomForTest(new Random(123L));
        g.prepareEmptyBoard(8, 8, 10);
        g.revealCell(4, 4);
        assertFalse(g.isGameOver());

        int revealed = 0;
        Integer[][] visible = g.getVisibleBoard();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (visible[r][c] != null) {
                    revealed++;
                }
            }
        }
        // safeCell=0 ⇒ flood-fill ⇒ almeno la safe cell + i suoi vicini numerati (>=2).
        assertTrue(revealed >= 2,
            "Il primo click dovrebbe aprire più di una cella, ma ne ha aperte " + revealed);
    }

    /**
     * Copertura anche su board "hard": density alta, primo click in un angolo.
     * Vincolo safeCell=0 deve continuare a reggere.
     */
    @Test
    void firstClickOnCornerOnHardBoardStillOpensArea() {
        Minesweeper g = new Minesweeper();
        g.setRandomForTest(new Random(98765L));
        g.prepareEmptyBoard(16, 30, 99);
        g.revealCell(0, 0);
        assertFalse(g.isGameOver());

        int revealed = 0;
        Integer[][] visible = g.getVisibleBoard();
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 30; c++) {
                if (visible[r][c] != null) {
                    revealed++;
                }
            }
        }
        assertTrue(revealed >= 2);
    }
}
