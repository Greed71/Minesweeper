package com;

import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {

    private final MinesweeperService minesweeper;

    public Controller(MinesweeperService minesweeperService) {
        this.minesweeper = minesweeperService;
    }

    @PostMapping("/genera")
    @CrossOrigin(origins = "http://localhost:5173")
    public void generate(@RequestBody GameConfig entity) {
        System.out.println("Received rows: " + entity.getRows());
        System.out.println("Received columns: " + entity.getCols());
        System.out.println("Received mines: " + entity.getMines());

        minesweeper.prepareEmptyBoard(entity.getRows(), entity.getCols(), entity.getMines());
    }

    @PostMapping("/clic")
    @CrossOrigin(origins = "http://localhost:5173")
    public Integer[][] firstClick(@RequestBody ClickPosition click) {
        return minesweeper.firstClick(click.getRow(), click.getCol());
    }
}
