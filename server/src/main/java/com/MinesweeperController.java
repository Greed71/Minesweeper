package com;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://minesweeper-two-drab.vercel.app")
public class MinesweeperController {

    @PostMapping("/genera")
    public Map<String, Object> generate(@RequestBody GameConfig config, HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Minesweeper minesweeper = new Minesweeper();
        minesweeper.prepareEmptyBoard(config.getRows(), config.getCols(), config.getMines());
        session.setAttribute("game", minesweeper);

        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        response.put("gameOver", false);
        response.put("message", "Inizia a giocare!");
        return response;
    }

    @PostMapping("/clic")
    public Map<String, Object> firstClick(@RequestBody ClickPosition click, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Minesweeper minesweeper = (Minesweeper) session.getAttribute("game");

        if (minesweeper == null) {
            throw new IllegalStateException("Partita non trovata.");
        }

        minesweeper.revealCell(click.getRow(), click.getCol());

        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        response.put("gameOver", minesweeper.isGameOver());
        response.put("gameWon", minesweeper.isGameWon());
        response.put("message", minesweeper.isGameOver() ? "Hai perso!" : (minesweeper.isGameWon() ? "Hai vinto!" : "Continua a giocare"));
        return response;
    }

    @PostMapping("/reveal")
    public Map<String, Object> reveal(@RequestBody ClickPosition click, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Minesweeper minesweeper = (Minesweeper) session.getAttribute("game");

        if (minesweeper == null) {
            throw new IllegalStateException("Partita non trovata.");
        }

        minesweeper.revealCell(click.getRow(), click.getCol());

        Map<String, Object> response = new HashMap<>();
        response.put("board", minesweeper.getVisibleBoard());
        response.put("gameOver", minesweeper.isGameOver());
        response.put("gameWon", minesweeper.isGameWon());
        response.put("message", minesweeper.isGameOver() ? "Hai perso!" : (minesweeper.isGameWon() ? "Hai vinto!" : "Continua a giocare"));
        return response;
    }
}
