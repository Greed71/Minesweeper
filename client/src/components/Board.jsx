import Cell from "./Cell.jsx";

/**
 * Board: componente puramente presentational per la griglia di gioco.
 * Riceve la matrice board, le bandiere e callback via props.
 */
function Board({ grid, buttonText, onCellClick, onCellRightClick, onCellDoubleClick }) {
  if (!grid || grid.length === 0) return null;

  return (
    <div
      className="game-board-outer"
      // Belt-and-suspenders: sopprime il menu contestuale del browser
      // anche nei pochi px di padding attorno alla tabella.
      onContextMenu={(e) => e.preventDefault()}
    >
      <table className="game-table">
        <tbody>
          {grid.map((rowArray, rowIndex) => (
            <tr key={rowIndex}>
              {rowArray.map((cellValue, colIndex) => {
                const marker = buttonText[rowIndex]?.[colIndex] ?? "";
                const isFlagged = marker === "🏴";
                const isQuestionMark = marker === "?";
                return (
                  <Cell
                    key={colIndex}
                    value={cellValue}
                    flagged={isFlagged}
                    questionMark={isQuestionMark}
                    revealed={cellValue !== null}
                    onClick={() => onCellClick(rowIndex, colIndex)}
                    onRightClick={(e) => {
                      e.preventDefault();
                      onCellRightClick(rowIndex, colIndex);
                    }}
                    onDoubleClick={() => onCellDoubleClick?.(rowIndex, colIndex)}
                  />
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Board;
