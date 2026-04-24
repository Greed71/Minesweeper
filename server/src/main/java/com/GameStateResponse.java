package com;

/** Stato partita: griglia, messaggio, fine gioco. */
public record GameStateResponse(
        Integer[][] board, boolean gameOver, boolean gameWon, String message) {
}
