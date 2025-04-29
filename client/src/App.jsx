import { useState, useEffect, useRef } from "react";
import "./App.css";
import axios from "axios";

function App() {
  // Stati principali per la gestione della griglia, del timer e dello stato del gioco
  const [row, setRow] = useState(0);
  const [col, setColumns] = useState(0);
  const [mines, setMines] = useState(0);
  const [board, setBoard] = useState([]);
  const [clicked, setClicked] = useState(false);
  const [mode, setMode] = useState("");
  const [gameOver, setGameOver] = useState(false);
  const [gameWon, setGameWon] = useState(false);
  const [timer, setTimer] = useState(0);
  const intervalId = useRef(0);  // Per memorizzare l'ID del timer
  const [message, setMessage] = useState("");
  const [finalTime, setFinalTime] = useState(null);
  const [buttonText, setButtonText] = useState([]);

  // Quando cambia il numero di righe o colonne, aggiorna il testo dei bottoni
  useEffect(() => {
    if (row > 0 && col > 0) {
      setButtonText(
        Array(row).fill().map(() => Array(col).fill("")) // Riempie l'array con stringhe vuote
      );
    }
  }, [row, col]);

  // Funzione per generare una nuova partita
  const handleSubmit = async () => {
    try {
      const response = await axios.post("http://localhost:8080/api/genera", {
        row: row,
        col: col,
        mines: mines,
      });
      setButtonText(Array(row).fill().map(() => Array(col).fill("")));
      setBoard(response.data.board);
      setGameOver(false);
      setGameWon(false);
      setTimer(0);
      stopTimer(intervalId.current);  // Ferma il timer
      intervalId.current = 0;  // Resetta l'ID del timer
      setMessage(response.data.message);
    } catch (error) {
      console.error(error);
    }
  };

  // Funzione per avviare il timer
  function startTimer() {
    if (intervalId.current) return; // Non avviare un altro timer se è già in corso
    const id = setInterval(() => {
      setTimer((prevTime) => prevTime + 1); // Incrementa il timer ogni secondo
    }, 1000);
    intervalId.current = id; // Memorizza l'ID del timer
  };

  // Funzione per fermare il timer
  function stopTimer(id) {
    if (intervalId.current) {
      clearInterval(id); // Ferma il timer
      setFinalTime(timer); // Salva il tempo finale
      intervalId.current = null; // Resetta l'ID del timer
    }
  };

  // Gestisce la selezione della modalità (facile, medio, difficile)
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
    }
  };

  // Gestisce il clic su una cella della griglia
  const handleCellClick = async (rowIndex, colIndex) => {
    try {
      if (gameOver || gameWon) return; // Non fare nulla se il gioco è finito
  
      if (!intervalId.current) startTimer(); // Avvia il timer se non è già in corso
  
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
      setMessage(response.data.message);
  
      // Se il gioco è finito o vinto, ferma il timer
      if (response.data.gameOver) {
        setGameOver(true);
        stopTimer(intervalId.current);
      }
      if (response.data.gameWon) {
        setGameWon(true);
        stopTimer(intervalId.current);
      }
    } catch (error) {
      console.error("Errore nel click:", error);
    }
  };

  // Gestisce il clic destro (per le bandiere)
  const handleCellRightClick = (rowIndex, colIndex, event) => {
    event.preventDefault(); // Impedisce il comportamento predefinito del clic destro
    const newButtonText = [...buttonText];
    if (gameOver || gameWon) return; // Non fare nulla se il gioco è finito
    if (newButtonText[rowIndex][colIndex] === "🏴") {
      newButtonText[rowIndex][colIndex] = ""; // Rimuove la bandiera
    } else {
      newButtonText[rowIndex][colIndex] = "🏴"; // Aggiunge la bandiera
    }
    setButtonText(newButtonText);
  };

  return (
    <div style={{ position: "relative", minHeight: "100vh" }}>
      {/* HEADER per la selezione della modalità */}
      <div style={{ position: "absolute", top: 0, left: 0, width: "100%", display: "flex", justifyContent: "center", gap: "20px", padding: "20px", zIndex: 10 }}>
        {["Facile", "Medio", "Difficile"].map((label) => (
          <button
            key={label}
            onClick={() => handleModeSelect(label.toLowerCase())}
            style={{ padding: "10px 20px", backgroundColor: mode === label.toLowerCase() ? "#aaa" : "#fff", border: "1px solid #000", borderRadius: "5px", cursor: "pointer" }}
          >
            {label}
          </button>
        ))}
      </div>

      {/* CONTENUTO */}
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", paddingTop: "120px" }}>
        <button
          onClick={handleSubmit}
          style={{ padding: "12px 42px", fontSize: "16px", border: "none", borderRadius: "5px", backgroundColor: "#ffffff", color: "#000000", cursor: "pointer", marginBottom: "30px" }}
        >
          Genera
        </button>

        {/* GRIGLIA di gioco */}
        {board.length > 0 && (
          <div style={{ display: "flex", justifyContent: "center", marginTop: "30px" }}>
            <table style={{ borderCollapse: "collapse" }}>
              <tbody>
                {board.map((rowArray, rowIndex) => (
                  <tr key={rowIndex}>
                    {rowArray.map((cell, colIndex) => (
                      <td
                        key={colIndex}
                        style={{ border: "1px solid black", width: "30px", height: "30px", textAlign: "center", verticalAlign: "middle" }}
                        onContextMenu={(e) => handleCellRightClick(rowIndex, colIndex, e)}
                      >
                        {cell === null ? (
                          <button
                            onClick={() => handleCellClick(rowIndex, colIndex)}
                            style={{ width: "100%", height: "100%", backgroundColor: "#ccc", border: "none", fontSize: buttonText[rowIndex]?.[colIndex] === "🏴" ? "10px" : "initial", cursor: "pointer" }}
                            disabled={gameOver || gameWon || buttonText[rowIndex]?.[colIndex]} // Disabilita il bottone se il gioco è finito o se è bandierato
                          >
                            {buttonText[rowIndex]?.[colIndex]}
                          </button>
                        ) : (
                          <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%", backgroundColor: "#eee", color: getColor(cell), fontSize: "18px" }}>
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

        {/* GAME OVER e GAME WON messaggi */}
        {gameOver && <div style={{ marginTop: "30px", fontSize: "24px" }}>{message}</div>}
        {gameWon && <div style={{ marginTop: "30px", fontSize: "24px" }}>{message}</div>}

        {/* TIMER */}
        {board.length > 0 && !gameOver && (
          <div style={{ marginTop: "30px", fontSize: "18px", fontWeight: "bold" }}>
            Timer: {timer}s
          </div>
        )}
      </div>
    </div>
  );
}

// Funzione per determinare il colore in base al numero
function getColor(number) {
  switch (number) {
    case 1: return "blue";
    case 2: return "green";
    case 3: return "red";
    case 4: return "darkblue";
    case 5: return "darkred";
    case 6: return "turquoise";
    case 7: return "black";
    case 8: return "gray";
    default: return "black";
  }
}

export default App;
