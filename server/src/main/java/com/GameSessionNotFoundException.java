package com;

/** Sessione scaduta o id sconosciuto. */
public class GameSessionNotFoundException extends RuntimeException {

    public GameSessionNotFoundException(String sessionId) {
        super("Partita non trovata: " + sessionId);
    }
}
