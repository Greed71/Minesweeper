package com;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class Controller {

    private final Minesweeper minesweeper;

    public Controller(Minesweeper minesweeper) {
        this.minesweeper = minesweeper;
    }

    @PostMapping("/genera")
    @CrossOrigin(origins = "http://localhost:5173")
    public Integer[][] generate(@RequestBody GameConfig config) {
        minesweeper.prepareEmptyBoard(config.getRows(), config.getCols(), config.getMines());
        System.out.println("Generato un nuovo campo di gioco con " + config.getRows() + " righe, " + config.getCols() + " colonne e " + config.getMines() + " mine.");
        return minesweeper.getVisibleBoard();
    }

    @PostMapping("/clic")
    @CrossOrigin(origins = "http://localhost:5173")
    public Integer[][] firstClick(@RequestBody ClickPosition click) {
        minesweeper.revealCell(click.getRow(), click.getCol());
        return minesweeper.getVisibleBoard();
    }

    @PostMapping("/reveal")
    @CrossOrigin(origins = "http://localhost:5173")
    public Integer[][] reveal(@RequestBody ClickPosition click) {
        minesweeper.revealCell(click.getRow(), click.getCol());
        return minesweeper.getVisibleBoard();
    }
}
