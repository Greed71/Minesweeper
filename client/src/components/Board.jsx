import Cell from "./Cell.jsx";

/**
 * Board: componente puramente presentational per la griglia di gioco.
 * Riceve la matrice board, le bandiere e callback via props.
 */
function Board({ grid, buttonText, onCellClick, onCellRightClick }) {
  if (!grid || grid.length === 0) return null;

  return (
    <div className="game-board-outer">
      <table className="game-table">
        <tbody>
          {grid.map((rowArray, rowIndex) => (
            <tr key={rowIndex}>
              {rowArray.map((cellValue, colIndex) => {
                const isFlagged = buttonText[rowIndex]?.[colIndex] === "🏴";
                return (
                  <Cell
                    key={colIndex}
                    value={cellValue}
                    flagged={isFlagged}
                    revealed={cellValue !== null}
                    onClick={() => onCellClick(rowIndex, colIndex)}
                    onRightClick={(e) => {
                      e.preventDefault();
                      onCellRightClick(rowIndex, colIndex);
                    }}
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
