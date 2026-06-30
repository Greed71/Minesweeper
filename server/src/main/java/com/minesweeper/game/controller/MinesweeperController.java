package com.minesweeper.game.controller;

import com.minesweeper.game.dto.ClickPosition;
import com.minesweeper.game.dto.GameConfig;
import com.minesweeper.game.dto.GameStateResponse;
import com.minesweeper.game.service.GameSessionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class MinesweeperController {

    private final GameSessionService gameSessionService;

    public MinesweeperController(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @PostMapping("/genera")
    public GameStateResponse generate(@RequestBody @Valid GameConfig config) {
        return gameSessionService.startGame(config);
    }

    /** Prima mossa: le mine non possono finire sulla cella che hai scelto. */
    @PostMapping({"/reveal", "/clic"})
    public GameStateResponse reveal(@RequestBody @Valid ClickPosition click) {
        return gameSessionService.revealCell(click);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }
}
