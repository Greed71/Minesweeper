package com;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MinesweeperController {

    private final Minesweeper minesweeper;

    public MinesweeperController(Minesweeper minesweeper) {
        this.minesweeper = minesweeper;
    }

    @PostMapping("/genera")
    @CrossOrigin(origins = "http://localhost:5173")
    public Map<String, Object> generate(@RequestBody GameConfig config) {
        minesweeper.prepareEmptyBoard(config.getRows(), config.getCols(), config.getMines());
        System.out.println("Generato un nuovo campo di gioco con " + config.getRows() + " righe, " + config.getCols()
                + " colonne e " + config.getMines() + " mine.");

        // Risposta che include la board e un messaggio di game over (inizialmente
        // falso)
        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        response.put("gameOver", false); // Inizialmente il gioco non è finito
        response.put("message", "Inizia a giocare!"); // Messaggio di avvio
        return response;
    }

    @PostMapping("/clic")
@CrossOrigin(origins = "http://localhost:5173")
public Map<String, Object> firstClick(@RequestBody ClickPosition click) {
    minesweeper.revealCell(click.getRow(), click.getCol());

    // Se il gioco è finito, lo segnaliamo
    boolean gameWon = minesweeper.isGameWon();

    // Rispondiamo con la board aggiornata, gameOver e il messaggio
    Map<String, Object> response = new HashMap<>();
    response.put("board", minesweeper.getVisibleBoard());
    response.put("gameWon", gameWon);

    // Se il gioco è vinto, inviamo il messaggio di vittoria
    if (gameWon) {
        response.put("message", "Congratulazioni! Hai vinto!");
    }

    return response;
}

    @PostMapping("/reveal")
    @CrossOrigin(origins = "http://localhost:5173")
    public Map<String, Object> reveal(@RequestBody ClickPosition click) {
        minesweeper.revealCell(click.getRow(), click.getCol());

        // Se il gioco è finito, lo segnaliamo
        boolean gameOver = minesweeper.isGameOver();
        boolean gameWon = minesweeper.isGameWon(); // Controlla se il gioco è vinto

        // Rispondiamo con la board aggiornata e lo stato di gameOver
        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        response.put("gameOver", gameOver);
        response.put("gameWon", gameWon); // Aggiunto per il gioco vinto

        // Messaggio di game over o vittoria
        if (gameOver) {
            response.put("message", "Game Over!");
        }

        if (gameWon) {
            response.put("message", "Congratulazioni! Hai vinto!");
        }

        return response;
    }

    @PostMapping("/flag")
    @CrossOrigin(origins = "http://localhost:5173")
    public Map<String, Object> flag(@RequestBody ClickPosition click) {
        minesweeper.flagCell(click.getRow(), click.getCol());
        
        // Rispondi con la board aggiornata
        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        return response;
    }

}
