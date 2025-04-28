import { useState, useEffect } from "react";
import "./App.css";
import axios from "axios";

function App() {
  const [row, setRow] = useState(0);
  const [col, setColumns] = useState(0);
  const [mines, setMines] = useState(0);
  const [board, setBoard] = useState([]);
  const [clicked, setClicked] = useState(false);
  const [mode, setMode] = useState("");
  const [gameOver, setGameOver] = useState(false); // Stato del gioco
  const [gameWon, setGameWon] = useState(false); // Stato del gioco
  const [timer, setTimer] = useState(0); // Timer in secondi
  const [intervalId, setIntervalId] = useState(null); // ID dell'intervallo per fermare il timer
  const [message, setMessage] = useState(""); // Messaggio da visualizzare
  const [finalTime, setFinalTime] = useState(null); // Stato per il tempo finale

  const handleSubmit = async () => {
    try {
      const response = await axios.post("http://localhost:8080/api/genera", {
        row: row,
        col: col,
        mines: mines,
      });
      setBoard(response.data.board);
      setGameOver(false); // Riavvia il gioco
      setTimer(0); // Resetta il timer
      startTimer(); // Avvia il timer
      setMessage(response.data.message); // Imposta il messaggio
    } catch (error) {
      console.error(error);
    }
  };

  const startTimer = () => {
    const id = setInterval(() => {
      setTimer((prevTime) => prevTime + 1); // Incrementa il timer ogni secondo
    }, 1000);
    setIntervalId(id); // Salva l'ID dell'intervallo per poterlo fermare più tardi
  };

  const stopTimer = () => {
    clearInterval(intervalId); // Ferma il timer
    setFinalTime(timer); // Salva il tempo finale
  };

  const handleModeSelect = (selectedMode) => {
    setMode(selectedMode);
    if (selectedMode === "facile") {
      setColumns(8);
      setRow(8);
      setMines(10);
    } else if (selectedMode === "medio") {
      setColumns(16);
      setRow(16);
      setMines(40);
    } else if (selectedMode === "difficile") {
      setColumns(30);
      setRow(16);
      setMines(99);
    } else if (selectedMode === "personalizzato") {
      setColumns(0);
      setRow(0);
      setMines(0);
    }
  };

  const handleCellClick = async (rowIndex, colIndex) => {
    try {
      let response;
      if (!clicked) {
        response = await axios.post("http://localhost:8080/api/clic", {
          row: rowIndex,
          col: colIndex,
        });
        setClicked(true);
      } else {
        response = await axios.post("http://localhost:8080/api/reveal", {
          row: rowIndex,
          col: colIndex,
        });
      }

      setBoard(response.data.board);
      setMessage(response.data.message); // Imposta il messaggio

      // Se il gioco è finito (vittoria o game over), ferma il timer
      if (response.data.gameOver) {
        setGameOver(true);
        stopTimer(); // Ferma il timer
      }

      if (response.data.gameWon) {
        setGameWon(true);
        stopTimer(); // Ferma il timer
      }
    } catch (error) {
      console.error("Errore nel click:", error);
    }
  };

  return (
    <>
      <div style={{ position: "relative", minHeight: "100vh" }}>
        {/* HEADER pulsanti modalità */}
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
            flexWrap: "nowrap",
          }}
        >
          {["Facile", "Medio", "Difficile", "Personalizzato"].map((label) => (
            <button
              key={label}
              onClick={() => handleModeSelect(label.toLowerCase())}
              style={{
                padding: "10px 20px",
                backgroundColor: mode === label.toLowerCase() ? "#aaa" : "#fff",
                border: "1px solid #000",
                borderRadius: "5px",
                cursor: "pointer",
                color: "#000",
              }}
            >
              {label}
            </button>
          ))}
        </div>

        {/* INPUTS per personalizzato */}
        {mode === "personalizzato" && (
          <div
            style={{
              position: "absolute",
              top: "80px",
              left: 0,
              width: "100%",
              display: "flex",
              justifyContent: "center",
              gap: "20px",
              padding: "10px",
              zIndex: 9,
            }}
          >
            <label style={{ color: "white" }}>
              Colonne:
              <input
                type="number"
                value={col}
                onChange={(e) => setColumns(Number(e.target.value))}
                style={{ padding: "5px", width: "80px", marginLeft: "5px" }}
              />
            </label>

            <label style={{ color: "white" }}>
              Righe:
              <input
                type="number"
                value={row}
                onChange={(e) => setRow(Number(e.target.value))}
                style={{ padding: "5px", width: "80px", marginLeft: "5px" }}
              />
            </label>

            <label style={{ color: "white" }}>
              Mine:
              <input
                type="number"
                value={mines}
                onChange={(e) => setMines(Number(e.target.value))}
                style={{ padding: "5px", width: "80px", marginLeft: "5px" }}
              />
            </label>
          </div>
        )}

        {/* CONTENUTO */}
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            paddingTop: mode === "personalizzato" ? "160px" : "120px",
          }}
        >
          <button
            onClick={handleSubmit}
            style={{
              padding: "12px 42px",
              fontSize: "16px",
              border: "none",
              borderRadius: "5px",
              backgroundColor: "#ffffff",
              color: "#000000",
              cursor: "pointer",
              marginBottom: "30px",
            }}
          >
            Genera
          </button>

          {/* GRIGLIA */}
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
                          }}
                        >
                          {cell === null ? (
                            <button
                              onClick={() =>
                                handleCellClick(rowIndex, colIndex)
                              }
                              style={{
                                width: "100%",
                                height: "100%",
                                backgroundColor: "#ccc",
                                border: "none",
                              }}
                              disabled={gameOver} // Disabilita il bottone se il gioco è finito
                            />
                          ) : (
                            <div
                              style={{
                                textAlign: "center",
                                lineHeight: "30px",
                                backgroundColor: "#eee",
                                color: getColor(cell),
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

          {/* GAME OVER */}
          {gameOver && (
            <div style={{ marginTop: "30px", fontSize: "24px" }}>
              {message} {/* Visualizza il messaggio di game over */}
            </div>
          )}
          {/* GAME WON */}
          {gameWon && (
            <div style={{ marginTop: "30px", fontSize: "24px" }}>
              {message} {/* Visualizza il messaggio di game won */}
            </div>
          )}

          {/* TIMER */}
          {board.length > 0 && !gameOver && (
            <div
              style={{
                marginTop: "30px",
                fontSize: "18px",
                fontWeight: "bold",
              }}
            >
              Timer: {timer}s
            </div>
          )}
        </div>
      </div>
    </>
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

export default App;
