import { useState, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";
import client from "../api/client.js";
import { devWarn } from "../devLog.js";
import { getBackendUrl } from "../config.js";

/**
 * Custom hook: incapsula tutta la logica del gioco Minesweeper.
 * - Stato: board, timer, gameOver, gameWon, flags, message
 * - Azioni: startGame, revealCell, toggleFlag
 * - Leaderboard e salvataggio punteggio
 */
export function useMinesweeper() {
  const { t } = useTranslation();
  const backendUrl = getBackendUrl();
  const [board, setBoard] = useState([]);
  const [gameOver, setGameOver] = useState(false);
  const [gameWon, setGameWon] = useState(false);
  const [gameStarted, setGameStarted] = useState(false);
  const [timer, setTimer] = useState(0);
  const [flags, setFlags] = useState(0);
  const [message, setMessage] = useState("");
  const [buttonText, setButtonText] = useState([]);
  const [leaderboard, setLeaderboard] = useState([]);
  const [mode, setMode] = useState("");
  const [clicked, setClicked] = useState(false);
  const [finalTime, setFinalTime] = useState(null);
  const [row, setRow] = useState(0);
  const [col, setColumns] = useState(0);
  const [mines, setMines] = useState(0);

  // Punteggio in attesa di claim: un guest che vince NON può salvare
  // (non ha user). Lo parcheggiamo in localStorage + state così appena
  // l'utente si registra/logga, Home.jsx chiama claimPendingScore(user)
  // e il punteggio finisce in classifica. Senza di questo il guest vince,
  // si registra dopo, e il punteggio è perso.
  const [pendingScore, setPendingScore] = useState(() => {
    try {
      const raw = localStorage.getItem("minesweeper.pendingScore");
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  });

  const sessionId = useRef(crypto.randomUUID());
  const intervalId = useRef(null);
  const timerSnapshot = useRef(0);

  const startTimer = useCallback(() => {
    if (intervalId.current) return;
    const id = setInterval(() => {
      timerSnapshot.current += 1;
      setTimer(timerSnapshot.current);
    }, 1000);
    intervalId.current = id;
  }, []);

  const stopTimer = useCallback(() => {
    if (intervalId.current) {
      clearInterval(intervalId.current);
      setFinalTime(timerSnapshot.current);
      intervalId.current = null;
    }
  }, []);

  const saveIfHighscore = useCallback(async (time, user) => {
    try {
      const currentLeaderboard = leaderboard;
      const currentMode = mode;
      const isTop10 = currentLeaderboard.length < 10 ||
        time < (currentLeaderboard[currentLeaderboard.length - 1]?.points ?? Infinity);
      if (!isTop10) return;

      // Guest winner: park il punteggio per claim successivo.
      // Home.jsx userEffect su [user, pendingScore] lo rispedirà al server
      // non appena l'utente si registra o fa login.
      if (!user) {
        const pending = { points: time, difficulty: currentMode, finishedAt: Date.now() };
        try { localStorage.setItem("minesweeper.pendingScore", JSON.stringify(pending)); } catch { /* noop */ }
        setPendingScore(pending);
        return;
      }

      // Logged-in winner: salva subito.
      const token = localStorage.getItem("token");
      await client.post(
        `${backendUrl}/score/save`,
        { points: time, difficulty: currentMode },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const updated = await client.get(
        `${backendUrl}/score/leaderboard?difficulty=${currentMode}`
      );
      setLeaderboard(updated.data);
    } catch (err) {
      devWarn(t("game.scoreError"), err);
    }
  }, [backendUrl, leaderboard, mode, t]);

  const loadLeaderboard = useCallback(async (difficulty) => {
    try {
      const res = await client.get(`${backendUrl}/score/leaderboard?difficulty=${difficulty}`);
      setLeaderboard(res.data);
    } catch (err) {
      devWarn(t("game.leaderboardError"), err);
    }
  }, [backendUrl, t]);

  /** Claim un punteggio parcheggiato (parked come guest) dopo che
   *  l'utente si è loggato/registrato. Sicuro da chiamare + volte: se
   *  `pendingScore` è null o l'utente non c'è, è un no-op.
   *
   *  IMPORTANTE: questa callback cambia reference ogni volta che
   *  `pendingScore` o `mode` cambiano (deps del useCallback). Questo
   *  è INTENZIONALE: Home.jsx ha un useEffect su
   *  [user, pendingScore, claimPendingScore] che chiama claim quando
   *  `user && pendingScore`. Il guard nel chiamante evita loop infiniti:
   *  dopo un claim riuscito, `setPendingScore(null)` qui sotto cambia
   *  reference di claimPendingScore → useEffect si ri-fire → guard
   *  fallisce (pendingScore è null) → no-op. NON rimuovere il guard
   *  in Home.jsx senza aggiungere una protezione equivalente. */
  const claimPendingScore = useCallback(async (user) => {
    if (!user || !pendingScore) return;
    try {
      const token = localStorage.getItem("token");
      await client.post(
        `${backendUrl}/score/save`,
        { points: pendingScore.points, difficulty: pendingScore.difficulty },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      try { localStorage.removeItem("minesweeper.pendingScore"); } catch { /* noop */ }
      setPendingScore(null);
      // Aggiorna la leaderboard della difficoltà in questione (se è ancora selezionata).
      if (mode === pendingScore.difficulty) {
        await loadLeaderboard(pendingScore.difficulty);
      }
    } catch (err) {
      devWarn(t("game.scoreError"), err);
    }
  }, [backendUrl, pendingScore, mode, loadLeaderboard, t]);

  const startGame = useCallback(async (rows, cols, mineCount) => {
    try {
      const response = await client.post(`${backendUrl}/api/genera`, {
        row: rows,
        col: cols,
        mines: mineCount,
        sessionId: sessionId.current,
      });
      setButtonText(
        Array(rows).fill().map(() => Array(cols).fill(""))
      );
      setBoard(response.data.board);
      setGameOver(false);
      setGameWon(false);
      timerSnapshot.current = 0;
      setTimer(0);
      setFlags(0);
      setGameStarted(true);
      stopTimer();
      setMessage(response.data.message);
      setClicked(false);
    } catch (error) {
      devWarn(t("game.gameStartError"), error);
      setMessage(t("game.serverUnreachable"));
    }
  }, [backendUrl, stopTimer, t]);

  const revealCell = useCallback(async (rowIndex, colIndex, user) => {
    try {
      if (gameOver || gameWon) return;
      if (!intervalId.current) startTimer();

      const response = await client.post(`${backendUrl}/api/reveal`, {
        row: rowIndex,
        col: colIndex,
        sessionId: sessionId.current,
      });
      if (!clicked) setClicked(true);

      setBoard(response.data.board);
      setMessage(response.data.message);

      if (response.data.gameOver) {
        setGameOver(true);
        stopTimer();
      }
      if (response.data.gameWon) {
        setGameWon(true);
        stopTimer();
        saveIfHighscore(timerSnapshot.current, user);
      }
    } catch (error) {
      devWarn(t("game.moveError"), error);
    }
  }, [backendUrl, gameOver, gameWon, clicked, startTimer, stopTimer, saveIfHighscore, t]);

  const toggleFlag = useCallback((rowIndex, colIndex) => {
    if (gameOver || gameWon) return;
    // Deep copy a un livello: il pattern shallow `[...buttonText]` mutava
    // le inner-array dello stato precedente (vedasi review precedente).
    const newButtonText = buttonText.map((row) => [...row]);
    const current = newButtonText[rowIndex]?.[colIndex] ?? "";

    // Ciclo classico Minesweeper: vuoto → 🚩 → ? → vuoto.
    let next;
    if (current === "") {
      if (flags >= mines) return; // limite mine raggiunto: non bandierare
      next = "🏴";
      setFlags((f) => f + 1);
    } else if (current === "🏴") {
      next = "?";
      setFlags((f) => f - 1); // '?' non conta come bandierina "salda"
    } else {
      // "?" → torna vuoto. Nessuna variazione sul contatore flags.
      next = "";
    }

    newButtonText[rowIndex][colIndex] = next;
    setButtonText(newButtonText);
  }, [buttonText, gameOver, gameWon, flags, mines]);

  /** Chord-click: su una cella rivelata con valore N, se gli N vicini
   *  bandierati (🚩) combaciano, rivela tutti i vicini non-bandierati.
   *  Comportamento classico del Minesweeper desktop. */
  const chordCell = useCallback(async (rowIndex, colIndex, user) => {
    if (gameOver || gameWon) return;

    const cellValue = board[rowIndex]?.[colIndex];
    if (cellValue === null || cellValue === undefined || cellValue <= 0) return;

    const rows = board.length;
    const cols = board[0]?.length ?? 0;
    if (!rows || !cols) return;

    let flagCount = 0;
    const toReveal = [];
    for (let dr = -1; dr <= 1; dr++) {
      for (let dc = -1; dc <= 1; dc++) {
        if (dr === 0 && dc === 0) continue;
        const nr = rowIndex + dr;
        const nc = colIndex + dc;
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
        const marker = buttonText[nr]?.[nc] ?? "";
        if (marker === "🏴") {
          flagCount++;
        } else if (board[nr]?.[nc] === null && marker !== "🏴") {
          // Non rivelata e non bandierata (può essere '?' o vuota): chord la rivela.
          toReveal.push([nr, nc]);
        }
      }
    }

    if (flagCount !== cellValue) return; // il numero non combacia: niente chord

    // Fire tutte le rivelazioni "in parallelo ma isolato": allSettled (non Promise.all)
    // evita che un singolo errore di rete annulli le altre rivelazioni, perché la
    // perdita di una cella su 8 è fastidiosa in un chord classico.
    await Promise.allSettled(toReveal.map(([r, c]) => revealCell(r, c, user)));
  }, [board, buttonText, gameOver, gameWon, revealCell]);

  const resetGame = useCallback(() => {
    setGameStarted(false);
    setRow(0);
    setColumns(0);
    setMines(0);
    setBoard([]);
    setClicked(false);
    setMode("");
    setGameOver(false);
    setGameWon(false);
    timerSnapshot.current = 0;
    setTimer(0);
    setFinalTime(null);
    setButtonText([]);
    setFlags(0);
    setMessage("");
    stopTimer();
    // NB: NON azzerare pendingScore qui. Se il guest vince, si registra
    // DOPO aver chiuso la partita ("Esci") e PRIMA di iniziarne una nuova,
    // dobbiamo ancora poter fare claim del punteggio parcheggiato.
    // Se invece il guest vince e poi inizia una nuova partita (anche
    // guest), il nuovo win SOSTITUISCE il pending (è la scelta design:
    // salvare solo l'ultima performance guest è OK).
  }, [stopTimer]);

  // Riprova con stessa difficoltà ma nuovo seed: nuovo sessionId → layout mine diverso sul server.
  const restartWithFreshSeed = useCallback(async () => {
    if (!row || !col || !mines) return;
    sessionId.current = crypto.randomUUID();
    await startGame(row, col, mines);
  }, [row, col, mines, startGame]);

  return {
    board, gameOver, gameWon, gameStarted, timer, flags, message,
    buttonText, leaderboard, mode, row, col, mines, finalTime,
    pendingScore,
    setRow, setColumns, setMines, setMode,
    startGame, revealCell, toggleFlag, chordCell,
    claimPendingScore,
    resetGame, restartWithFreshSeed, loadLeaderboard, setLeaderboard,
  };
}
