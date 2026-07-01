/**
 * Cell: componente puramente presentational per una cella della griglia.
 * Riceve valore, stato (covered / flagged / questionMark / revealed)
 * e callback via props.
 */
function Cell({
  value,
  flagged,
  questionMark,
  revealed,
  onClick,
  onRightClick,
  onDoubleClick,
}) {
  if (revealed) {
    // Cella già scoperta: doppio click sinistro → chord (rivela i vicini
    // se il numero combacia col numero di bandierine adiacenti).
    const clickable = value > 0 && value <= 8;
    const numClass = clickable ? ` cell-num--${value}` : "";
    return (
      <td>
        <div
          className={`cell-revealed${numClass}`}
          onDoubleClick={clickable ? onDoubleClick : undefined}
          title={clickable ? "Doppio click per chord" : undefined}
        >
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
        // La bandierina 'ferma' la cella: un click sinistro qui rivelerebbe
        // la mina segnalata. La cella con '?' invece ACCETTA il left-click
        // (= rivela, come nel Minesweeper classico), quindi non è disabled.
        disabled={!!flagged}
      >
        {flagged ? "🏴" : questionMark ? "?" : ""}
      </button>
    </td>
  );
}

export default Cell;
