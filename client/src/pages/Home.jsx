import { useState, useEffect, useRef } from "react";
import client from "../api/client.js";
import { devWarn } from "../devLog.js";
import { getBackendUrl } from "../config.js";

const DIFFICULTY_LABEL = {
  easy: "Facile",
  medium: "Medio",
  hard: "Difficile",
};

function Home({ resetTrigger }) {
  const [row, setRow] = useState(0);
  const [col, setColumns] = useState(0);
  const [mines, setMines] = useState(0);
  const [board, setBoard] = useState([]);
  const [clicked, setClicked] = useState(false);
  const [mode, setMode] = useState("");
  const [gameOver, setGameOver] = useState(false);
  const [gameWon, setGameWon] = useState(false);
  const [timer, setTimer] = useState(0);
  const intervalId = useRef(0);
  const [message, setMessage] = useState("");
  const [finalTime, setFinalTime] = useState(null);
  const [buttonText, setButtonText] = useState([]);
  const [flags, setFlags] = useState(0);
  const [leaderboard, setLeaderboard] = useState([]);
  const [showLeaderboard, setShowLeaderboard] = useState(true);
  const [user, setUser] = useState(null);
  const [gameStarted, setGameStarted] = useState(false);
  const [sessionId] = useState(() => crypto.randomUUID());
  const backendUrl = getBackendUrl();

  useEffect(() => {
    if (row > 0 && col > 0) {
      setButtonText(
        Array(row)
          .fill()
          .map(() => Array(col).fill(""))
      );
    }
  }, [row, col]);

  useEffect(() => {
    const pingInterval = setInterval(() => {
      client.get(`${backendUrl}/api/ping`).catch((err) => {
        devWarn("Il backend non risponde (controlla che sia avviato).", err);
      });
    }, 60_000);

    return () => clearInterval(pingInterval);
  }, [backendUrl]);
  useEffect(() => {
    setGameStarted(false);
    setRow(0);
    setColumns(0);
    setMines(0);
    setBoard([]);
    setClicked(false);
    setMode("");
    setGameOver(false);
    setGameWon(false);
    setTimer(0);
    setFinalTime(null);
    setButtonText([]);
    setFlags(0);
    setMessage("");
    stopTimer(intervalId.current);
    intervalId.current = 0;
  }, [resetTrigger]);

  useEffect(() => {
    if (mode) {
      client
        .get(`${backendUrl}/score/leaderboard?difficulty=${mode}`)
        .then((res) => setLeaderboard(res.data))
        .catch((err) =>
          devWarn("Classifica non caricata, riprova tra poco.", err)
        );
    }
  }, [mode]);

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
          localStorage.setItem("loggedUser", JSON.stringify(res.data));
        })
        .catch(() => {
          setUser(null);
          localStorage.removeItem("token");
          localStorage.removeItem("loggedUser");
        });
    } else if (storedUser) {
      setUser(JSON.parse(storedUser));
    } else {
      setUser(null);
    }
  }, []);

  const handleSubmit = async () => {
    try {
      const response = await client.post(`${backendUrl}/api/genera`, {
        row: row,
        col: col,
        mines: mines,
        sessionId: sessionId,
      });
      setButtonText(
        Array(row)
          .fill()
          .map(() => Array(col).fill(""))
      );
      setBoard(response.data.board);
      setGameOver(false);
      setGameWon(false);
      setTimer(0);
      setFlags(0);
      setGameStarted(true);
      stopTimer(intervalId.current);
      intervalId.current = 0;
      setMessage(response.data.message);
    } catch (error) {
      devWarn("Non è partita la partita, controlla la connessione.", error);
      setMessage(
        "Server non raggiungibile. Avvia Spring su porta 8080 (nella root: npm run dev) oppure controlla VITE_APP_BACKEND_URL."
      );
    }
  };
  const handleBack = () => {
    setGameStarted(false);
    setRow(0);
    setColumns(0);
    setMines(0);
    setBoard([]);
    setClicked(false);
    setMode("");
    setGameOver(false);
    setGameWon(false);
    setTimer(0);
    setFinalTime(null);
    setButtonText([]);
    setFlags(0);
    setMessage("");
    stopTimer(intervalId.current);
    intervalId.current = 0;
  };

  const saveIfHighscore = async () => {
    try {
      const isTop10 =
        leaderboard.length < 10 ||
        timer < leaderboard[leaderboard.length - 1].points;
      if (isTop10) {
        if (!user) {
          alert(
            "Devi effettuare l'accesso o registrarti per salvare il punteggio."
          );
          return;
        }
        const token = localStorage.getItem("token");
        await client.post(
          `${backendUrl}/score/save`,
          { points: timer, difficulty: mode },
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        const updated = await client.get(
          `${backendUrl}/score/leaderboard?difficulty=${mode}`
        );
        setLeaderboard(updated.data);
      }
    } catch (err) {
      devWarn("Punteggio non salvato.", err);
    }
  };

  function startTimer() {
    if (intervalId.current) return;
    const id = setInterval(() => {
      setTimer((prevTime) => prevTime + 1);
    }, 1000);
    intervalId.current = id;
  }

  function stopTimer(id) {
    if (intervalId.current) {
      clearInterval(id);
      setFinalTime(timer);
      intervalId.current = null;
    }
  }

  const handleModeSelect = (selectedMode) => {
    setMode(selectedMode);
    if (selectedMode === "easy") {
      setColumns(8);
      setRow(8);
      setMines(10);
    } else if (selectedMode === "medium") {
      setColumns(16);
      setRow(16);
      setMines(40);
    } else if (selectedMode === "hard") {
      setColumns(30);
      setRow(16);
      setMines(99);
    }
  };

  const handleCellClick = async (rowIndex, colIndex) => {
    try {
      if (gameOver || gameWon) return;
      if (!intervalId.current) startTimer();

      const response = await client.post(`${backendUrl}/api/reveal`, {
        row: rowIndex,
        col: colIndex,
        sessionId: sessionId,
      });
      if (!clicked) {
        setClicked(true);
      }

      setBoard(response.data.board);
      setMessage(response.data.message);

      if (response.data.gameOver) {
        setGameOver(true);
        stopTimer(intervalId.current);
      }
      if (response.data.gameWon) {
        setGameWon(true);
        saveIfHighscore();
        stopTimer(intervalId.current);
      }
    } catch (error) {
      devWarn("Questa mossa non è passata, riprova.", error);
    }
  };

  const handleCellRightClick = (rowIndex, colIndex, event) => {
    event.preventDefault();
    const newButtonText = [...buttonText];
    if (gameOver || gameWon) return;

    if (newButtonText[rowIndex][colIndex] === "🏴") {
      newButtonText[rowIndex][colIndex] = "";
      setFlags(flags - 1);
    } else if (flags < mines) {
      newButtonText[rowIndex][colIndex] = "🏴";
      setFlags(flags + 1);
    }
    setButtonText(newButtonText);
  };

  return (
    <main className="page home">
      {!gameStarted && (
        <div className="home__pre">
          <div className="home-hero">
            <h1>Pronti a giocare?</h1>
            <p>
              Scegli la difficoltà, controlla la classifica, poi inizia la
              partita.
            </p>
          </div>
          <div className="mode-row" role="group" aria-label="Difficoltà">
            {[
              ["easy", "Facile"],
              ["medium", "Medio"],
              ["hard", "Difficile"],
            ].map(([id, label]) => (
              <button
                key={id}
                type="button"
                className={`mode-pill${mode === id ? " mode-pill--active" : ""}`}
                onClick={() => handleModeSelect(id)}
              >
                {label}
              </button>
            ))}
          </div>
          <button
            type="button"
            className="btn btn--primary"
            onClick={() => {
              void handleSubmit();
            }}
            disabled={!mode || !row}
          >
            Gioca
          </button>

          {showLeaderboard && mode && (
            <div className="leaderboard">
              <h2>
                Classifica — {DIFFICULTY_LABEL[mode] ?? mode}
              </h2>
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
              <span>
                Tempo: <strong>{timer}s</strong>
              </span>
              <span>
                Bandiere: <strong>
                  {mines - flags}
                </strong>{" "}
                / {mines}
              </span>
            </div>
          )}

          {board.length > 0 && (
            <div className="game-board-outer">
              <table className="game-table">
                <tbody>
                  {board.map((rowArray, rowIndex) => (
                    <tr key={rowIndex}>
                      {rowArray.map((cell, colIndex) => (
                        <td
                          key={colIndex}
                          onContextMenu={(e) =>
                            handleCellRightClick(rowIndex, colIndex, e)
                          }
                        >
                          {cell === null ? (
                            <button
                              type="button"
                              className="cell-btn"
                              onClick={() => handleCellClick(rowIndex, colIndex)}
                              style={{
                                fontSize:
                                  buttonText[rowIndex]?.[colIndex] === "🏴"
                                    ? "0.65rem"
                                    : undefined,
                              }}
                              disabled={
                                gameOver ||
                                gameWon ||
                                !!buttonText[rowIndex]?.[colIndex]
                              }
                            >
                              {buttonText[rowIndex]?.[colIndex] ?? ""}
                            </button>
                          ) : (
                            <div
                              className={`cell-revealed${
                                cell > 0 && cell <= 8
                                  ? ` cell-num--${cell}`
                                  : ""
                              }`}
                            >
                              {cell === -1 ? "💣" : cell === 0 ? "" : cell}
                            </div>
                          )}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {board.length > 0 && (gameOver || gameWon) && (
            <>
              {gameOver && (
                <p className="game-message game-message--lose" role="status">
                  {message}
                </p>
              )}
              {gameWon && (
                <p className="game-message game-message--win" role="status">
                  {message}
                </p>
              )}
              <button
                type="button"
                className="btn btn--secondary"
                onClick={handleBack}
              >
                Menu
              </button>
            </>
          )}
        </div>
      )}
    </main>
  );
}

export default Home;
