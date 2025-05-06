// pages/Home.jsx
import { useState, useEffect, useRef } from "react";
import axios from "axios";
import "../App.css";

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
      axios
        .get("https://minesweeper-back.onrender.com/api/ping")
        .then((response) => {
          console.log("Ping inviato con successo:", response.data);
        })
        .catch((error) => {
          console.error("Errore nel ping:", error);
        });
    }, 60000); // Ping ogni 1 minuto (60.000 ms)

    // Pulizia dell'intervallo se il componente viene smontato
    return () => {
      clearInterval(pingInterval);
    };
  }, []);

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
      axios
        .get(`https://minesweeper-back.onrender.com/score/leaderboard?difficulty=${mode}`)
        .then((res) => setLeaderboard(res.data))
        .catch((err) => console.error("Errore nel recupero leaderboard:", err));
    }
  }, [mode]);

  useEffect(() => {
    const storedUser = localStorage.getItem("loggedUser");
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    } else {
      axios
        .get("https://minesweeper-back.onrender.com/auth/user", { withCredentials: true })
        .then((res) => {
          setUser(res.data);
          localStorage.setItem("loggedUser", JSON.stringify(res.data));
        })
        .catch(() => setUser(null));
    }
  }, []);

  const handleSubmit = async () => {
    try {
      const response = await axios.post("https://minesweeper-back.onrender.com/api/genera", {
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
      console.error(error);
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
        await axios.post("https://minesweeper-back.onrender.com/score/save", {
          username: user.username, // ora user è un oggetto
          points: timer,
          difficulty: mode,
        }, { withCredentials: true });
        const updated = await axios.get(
          `https://minesweeper-back.onrender.com/score/leaderboard?difficulty=${mode}`
        );
        setLeaderboard(updated.data);
      }
    } catch (err) {
      console.error("Errore nel salvataggio del punteggio:", err);
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

      let response;
      if (!clicked) {
        response = await axios.post("https://minesweeper-back.onrender.com/api/clic", {
          row: rowIndex,
          col: colIndex,
          sessionId: sessionId,
        });
        setClicked(true);
      } else {
        response = await axios.post("https://minesweeper-back.onrender.com/api/reveal", {
          row: rowIndex,
          col: colIndex,
          sessionId: sessionId,
        });
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
      console.error("Errore nel click:", error);
    }
  };

  const handleCellRightClick = (rowIndex, colIndex, event) => {
    event.preventDefault(); // Impedisce il comportamento predefinito del clic destro
    const newButtonText = [...buttonText];
    if (gameOver || gameWon) return;

    if (newButtonText[rowIndex][colIndex] === "🏴") {
      newButtonText[rowIndex][colIndex] = "";
      setFlags(flags - 1); // Rimuove una bandiera
    } else {
      if (flags < mines) {
        // Non superare il numero di mine
        newButtonText[rowIndex][colIndex] = "🏴";
        setFlags(flags + 1); // Aggiunge una bandiera
      }
    }

    setButtonText(newButtonText);
  };

  return (
    <div
      style={{
        position: "relative",
        minHeight: "100vh",
        WebkitUserSelect: "none",
        userSelect: "none",
      }}
    >
      {!gameStarted && (
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            width: "100%",
            display: "flex",
            justifyContent: "center",
            gap: "20px",
            padding: "20px",
            zIndex: 10,
          }}
        >
          {["Easy", "Medium", "Hard"].map((label) => (
            <button
              key={label}
              onClick={() => handleModeSelect(label.toLowerCase())}
              style={{
                padding: "10px 20px",
                backgroundColor: mode === label.toLowerCase() ? "#aaa" : "#fff",
                border: "1px solid #000",
                borderRadius: "5px",
                cursor: "pointer",
                color: "#000000",
              }}
            >
              {label}
            </button>
          ))}
        </div>
      )}

      <div
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          paddingTop: "120px",
        }}
      >
        {!gameStarted && (
          <button
            onClick={() => {
              handleSubmit();
              setGameStarted(true);
            }}
            style={{
              padding: "12px 40px",
              fontSize: "16px",
              border: "none",
              borderRadius: "5px",
              backgroundColor: "#ffffff",
              color: "#000000",
              cursor: "pointer",
              marginBottom: "30px",
              marginLeft: "20px",
            }}
          >
            Play
          </button>
        )}

        {showLeaderboard && mode && (
          <div className="leaderboard">
            <h2>Ranking - {mode}</h2>
            <ul>
              {leaderboard.map((entry, i) => (
                <li key={i}>
                  {entry.user.username} - {entry.points}s
                </li>
              ))}
            </ul>
          </div>
        )}

        {board.length > 0 && (
          <div
            style={{
              display: "flex",
              justifyContent: "center",
              marginTop: "30px",
            }}
          >
            <table style={{ borderCollapse: "collapse" }}>
              <tbody>
                {board.map((rowArray, rowIndex) => (
                  <tr key={rowIndex}>
                    {rowArray.map((cell, colIndex) => (
                      <td
                        key={colIndex}
                        style={{
                          border: "1px solid black",
                          width: "30px",
                          height: "30px",
                          textAlign: "center",
                          verticalAlign: "middle",
                        }}
                        onContextMenu={(e) =>
                          handleCellRightClick(rowIndex, colIndex, e)
                        }
                      >
                        {cell === null ? (
                          <button
                            onClick={() => handleCellClick(rowIndex, colIndex)}
                            style={{
                              width: "100%",
                              height: "100%",
                              backgroundColor: "#ccc",
                              border: "none",
                              fontSize:
                                buttonText[rowIndex]?.[colIndex] === "🏴"
                                  ? "10px"
                                  : "initial",
                              cursor: "pointer",
                            }}
                            disabled={
                              gameOver ||
                              gameWon ||
                              buttonText[rowIndex]?.[colIndex]
                            }
                          >
                            {buttonText[rowIndex]?.[colIndex]}
                          </button>
                        ) : (
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "center",
                              alignItems: "center",
                              height: "100%",
                              backgroundColor: "#eee",
                              color: getColor(cell),
                              fontSize: "18px",
                            }}
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

        {gameOver && (
          <div style={{ marginTop: "30px", fontSize: "24px" }}>{message}</div>
        )}
        {gameWon && (
          <div style={{ marginTop: "30px", fontSize: "24px" }}>{message}</div>
        )}

        {(gameOver || gameWon) && (
          <button
            onClick={handleBack}
            style={{
              marginTop: "30px",
              padding: "10px 20px",
              fontSize: "16px",
              borderRadius: "5px",
              border: "1px solid black",
              backgroundColor: "#f2f2f2",
              cursor: "pointer",
              color: "#000000",
            }}
          >
            Back
          </button>
        )}

        {board.length > 0 && !gameOver && (
          <div
            style={{ marginTop: "30px", fontSize: "18px", fontWeight: "bold" }}
          >
            Timer: {timer}s
          </div>
        )}
        {board.length > 0 && !gameOver && (
          <div
            style={{ marginTop: "10px", fontSize: "18px", fontWeight: "bold" }}
          >
            💣: {mines - flags}
          </div>
        )}
      </div>
    </div>
  );
}

function getColor(number) {
  switch (number) {
    case 1:
      return "blue";
    case 2:
      return "green";
    case 3:
      return "red";
    case 4:
      return "darkblue";
    case 5:
      return "darkred";
    case 6:
      return "turquoise";
    case 7:
      return "black";
    case 8:
      return "gray";
    default:
      return "black";
  }
}

export default Home;
