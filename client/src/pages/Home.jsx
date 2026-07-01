import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import client from "../api/client.js";
import { devWarn } from "../devLog.js";
import { getBackendUrl } from "../config.js";
import { useMinesweeper } from "../hooks/useMinesweeper.js";
import Board from "../components/Board.jsx";
import { useTranslation } from "react-i18next";

/** Strategy pattern: configurazione difficoltà in una mappa. Aggiungere "custom" è 1 riga. */
const DIFFICULTY_KEYS = ["easy", "medium", "hard"];

const DIFFICULTY_CONFIG = {
  easy:   { rows: 8,  cols: 8,  mines: 10 },
  medium: { rows: 16, cols: 16, mines: 40 },
  hard:   { rows: 16, cols: 30, mines: 99 },
};

function Home({ resetTrigger }) {
  const backendUrl = getBackendUrl();
  const [user, setUser] = useState(null);
  const { t } = useTranslation();

  const {
    board, gameOver, gameWon, gameStarted, timer, flags, message,
    buttonText, leaderboard, mode, row, col, mines,
    setRow, setColumns, setMines, setMode,
    startGame, revealCell, toggleFlag, chordCell, resetGame,
    restartWithFreshSeed, loadLeaderboard,
  } = useMinesweeper();

  // Ping keep-alive
  useEffect(() => {
    const pingInterval = setInterval(() => {
      client.get(`${backendUrl}/api/ping`).catch((err) => {
        devWarn(t("game.backendNotResponding"), err);
      });
    }, 60_000);
    return () => clearInterval(pingInterval);
  }, [backendUrl, t]);

  // Reset quando il trigger cambia (click logo)
  useEffect(() => {
    resetGame();
  }, [resetTrigger, resetGame]);

  // Carica leaderboard quando cambia la difficoltà
  useEffect(() => {
    if (mode) loadLeaderboard(mode);
  }, [mode, loadLeaderboard]);

  // Carica utente loggato
  useEffect(() => {
    const token = localStorage.getItem("token");
    const storedUser = localStorage.getItem("loggedUser");
    if (token) {
      client
        .get(`${backendUrl}/auth/user`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        .then((res) => {
          setUser(res.data);
          try { localStorage.setItem("loggedUser", JSON.stringify(res.data)); } catch { /* noop */ }
        })
        .catch(() => {
          setUser(null);
          try { localStorage.removeItem("token"); } catch { /* noop */ }
          try { localStorage.removeItem("loggedUser"); } catch { /* noop */ }
        });
    } else if (storedUser) {
      try { setUser(JSON.parse(storedUser)); } catch { setUser(null); }
    } else {
      setUser(null);
    }
  }, [backendUrl]);

  /** Strategy: seleziona difficoltà dalla mappa di configurazione. */
  const handleModeSelect = (modeKey) => {
    const cfg = DIFFICULTY_CONFIG[modeKey];
    if (!cfg) return;
    setMode(modeKey);
    setRow(cfg.rows);
    setColumns(cfg.cols);
    setMines(cfg.mines);
  };

  const handleCellClick = (r, c) => revealCell(r, c, user);

  const handleCellRightClick = (r, c) => toggleFlag(r, c);

  // Chord-click su una cella rivelata: rivela i vicini non-bandierati
  // quando il numero combacia col numero di bandierine adiacenti.
  const handleCellDoubleClick = (r, c) => chordCell(r, c, user);

  return (
    <main className="page home">
      {!gameStarted && (
        <div className="home__pre">
          <div className="home-hero">
            <h1>{t("home.heroTitle")}</h1>
            <p>{t("home.heroSubtitle")}</p>
          </div>

          <div className="mode-row" role="group" aria-label={t("home.heroTitle")}>
            {DIFFICULTY_KEYS.map((id) => (
              <button
                key={id}
                type="button"
                className={`mode-pill${mode === id ? " mode-pill--active" : ""}`}
                onClick={() => handleModeSelect(id)}
              >
                {t(`difficulty.${id}`)}
              </button>
            ))}
          </div>

          <button
            type="button"
            className="btn btn--primary"
            onClick={() => startGame(row, col, mines)}
            disabled={!mode || !row}
          >
            {t("home.play")}
          </button>

          {mode && leaderboard.length > 0 && (
            <div className="leaderboard">
              <h2>{t("home.leaderboard")} — {t(`difficulty.${mode}`)}</h2>
              <ul>
                {leaderboard.map((entry, i) => (
                  <li key={i}>
                    <span className="name">{entry.user.username}</span>
                    <span className="pts">{entry.points}s</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {gameStarted && (
        <div className="game-wrap">
          {board.length > 0 && !gameOver && !gameWon && (
            <div className="game-hud" aria-live="polite">
              <span>{t("game.time")} <strong>{timer}s</strong></span>
              <span>{t("game.flags")} <strong>{mines - flags}</strong> / {mines}</span>
            </div>
          )}

          <Board
            grid={board}
            buttonText={buttonText}
            onCellClick={handleCellClick}
            onCellRightClick={handleCellRightClick}
            onCellDoubleClick={handleCellDoubleClick}
            gameOver={gameOver}
            gameWon={gameWon}
          />

          {board.length > 0 && (gameOver || gameWon) && (
            <>
              <p
                className={`game-message ${gameWon ? "game-message--win" : "game-message--lose"}`}
                role="status"
              >
                {message}
              </p>
              {/* Vittoria: Esci / Riprova (nuovo seed) / Registrati (solo non loggato).
                  Sconfitta: Esci + Riprova (stesso seed nuovo). */}
              <div className={`post-game-actions${gameWon ? " post-game-actions--win" : ""}`}>
                <button
                  type="button"
                  className="btn btn--secondary"
                  onClick={resetGame}
                >
                  {t("game.menu")}
                </button>
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={restartWithFreshSeed}
                >
                  {t("game.retry")}
                </button>
                {gameWon && !user && (
                  <Link to="/register" className="btn btn--secondary">
                    {t("auth.registerButton")}
                  </Link>
                )}
              </div>
            </>
          )}
        </div>
      )}
    </main>
  );
}

export default Home;
