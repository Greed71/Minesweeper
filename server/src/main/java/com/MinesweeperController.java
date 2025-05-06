package com;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://minesweeper-two-drab.vercel.app", allowCredentials = "true")
public class MinesweeperController {

    private final Map<String, Minesweeper> sessions = new HashMap<>();

    @PostMapping("/genera")
    public Map<String, Object> generate(@RequestBody GameConfig config) {
        Minesweeper minesweeper = new Minesweeper();
        minesweeper.prepareEmptyBoard(config.getRows(), config.getCols(), config.getMines());
        sessions.put(config.getSessionId(), minesweeper);

        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        response.put("gameOver", false);
        response.put("message", "Inizia a giocare!");
        return response;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Ping ricevuto!");
    }

    @PostMapping("/clic")
    public Map<String, Object> firstClick(@RequestBody ClickPosition click) {
        Minesweeper minesweeper = sessions.get(click.getSessionId());

        if (minesweeper == null) {
            throw new IllegalStateException("Partita non trovata.");
        }

        minesweeper.revealCell(click.getRow(), click.getCol());

        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        response.put("gameOver", minesweeper.isGameOver());
        response.put("gameWon", minesweeper.isGameWon());
        response.put("message", minesweeper.isGameOver() ? "Hai perso!"
                : (minesweeper.isGameWon() ? "Hai vinto!" : "Continua a giocare"));
        return response;
    }

    @PostMapping("/reveal")
    public Map<String, Object> reveal(@RequestBody ClickPosition click) {
        Minesweeper minesweeper = sessions.get(click.getSessionId());

        if (minesweeper == null) {
            throw new IllegalStateException("Partita non trovata.");
        }

        minesweeper.revealCell(click.getRow(), click.getCol());

        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        response.put("gameOver", minesweeper.isGameOver());
        response.put("gameWon", minesweeper.isGameWon());
        response.put("message", minesweeper.isGameOver() ? "Hai perso!"
                : (minesweeper.isGameWon() ? "Hai vinto!" : "Continua a giocare"));
        return response;
    }
}