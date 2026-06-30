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
      if (!user) {
        alert(t("game.loginToSave"));
        return;
      }
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
    const newButtonText = [...buttonText];
    if (newButtonText[rowIndex]?.[colIndex] === "🏴") {
      newButtonText[rowIndex][colIndex] = "";
      setFlags((f) => f - 1);
    } else if (flags < mines) {
      newButtonText[rowIndex][colIndex] = "🏴";
      setFlags((f) => f + 1);
    }
    setButtonText(newButtonText);
  }, [buttonText, gameOver, gameWon, flags, mines]);

  const loadLeaderboard = useCallback(async (difficulty) => {
    try {
      const res = await client.get(`${backendUrl}/score/leaderboard?difficulty=${difficulty}`);
      setLeaderboard(res.data);
    } catch (err) {
      devWarn(t("game.leaderboardError"), err);
    }
  }, [backendUrl, t]);

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
  }, [stopTimer]);

  return {
    board, gameOver, gameWon, gameStarted, timer, flags, message,
    buttonText, leaderboard, mode, row, col, mines, finalTime,
    setRow, setColumns, setMines, setMode,
    startGame, revealCell, toggleFlag, resetGame, loadLeaderboard,
    setLeaderboard,
  };
}
