/**
 * Cell: componente puramente presentational per una cella della griglia.
 * Riceve valore, stato e callback via props.
 */
function Cell({ value, flagged, revealed, onClick, onRightClick }) {
  if (revealed) {
    const numClass = value > 0 && value <= 8 ? ` cell-num--${value}` : "";
    return (
      <td>
        <div className={`cell-revealed${numClass}`}>
          {value === -1 ? "💣" : value === 0 ? "" : value}
        </div>
      </td>
    );
  }

  return (
    <td onContextMenu={onRightClick}>
      <button
        type="button"
        className="cell-btn"
        onClick={onClick}
        style={{ fontSize: flagged ? "0.65rem" : undefined }}
        disabled={!!flagged}
      >
        {flagged ? "🏴" : ""}
      </button>
    </td>
  );
}

export default Cell;
