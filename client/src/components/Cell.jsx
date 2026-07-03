/**
 * Cell: componente puramente presentational per una cella della griglia.
 * Riceve valore, stato (covered / flagged / questionMark / revealed)
 * e callback via props.
 *
 * ROBUSTEZZA CLICK (bug storico, riapertura dopo il fix transform→filter):
 *   Il `<td>` È il CLICK TARGET secondario per i 1-2px di border-collapse
 *   border che stanno FUORI dal button interno. Il `<button>` È il
 *   target primario (mouse + keyboard via Enter/Space). Motivo del cambio
 *   storico:
 *     - `border-spacing: 1px` (versione precedente a `border-collapse:
 *       collapse`) lasciava una striscia di 1px NON coperta da alcun
 *       `<td>` (appartenente al background del `<table>`): click su quei
 *       pixel cadevano fuori dal button e non triggera nulla.
 *     - `overflow: hidden` su `<button>` (rimosso in App.css): in alcuni
 *       browser (Safari, WebKit mobile) causa hit-test subpixel in cui un
 *       mousedown a 1-2px dal bordo viene perso.
 *
 *   Architettura attuale:
 *     1. <button> è focusable (tabIndex={0} di default) e clickable.
 *        stopPropagation() evita doppio-fire quando il click bubbla al td.
 *     2. <td> ha un onClick fallback (target === currentTarget) che
 *        cattura i click sulla cornice 1px del border-collapse.
 *     3. <td> gestisce onContextMenu (right-click) per entrambi i
 *        sotto-elementi.
 *
 * A11Y: il <button> è il focusable element (Tab → Space/Enter reveals
 * come nel Minesweeper classico desktop). I <td> revealed con valore N
 * usano role="button" + tabIndex={0} + onKeyDown per gestire anche
 * chord via tastiera.
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
      <td
        // Keyboard-equivalent del double-click: Enter/Space sulla cella
        // numbered triggera il chord. tabIndex={0} la mette in tab-order.
        tabIndex={clickable ? 0 : -1}
        role={clickable ? "button" : undefined}
        aria-label={
          clickable
            ? `Cella ${value}, ${flagged ? "segnata" : "coperta"}, Invio o Spazio per espandere i vicini`
            : undefined
        }
        onDoubleClick={clickable ? onDoubleClick : undefined}
        onKeyDown={
          clickable
            ? (e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  onDoubleClick?.();
                }
              }
            : undefined
        }
        title={clickable ? "Doppio click per chord" : undefined}
      >
        <div className={`cell-revealed${numClass}`}>
          {value === -1 ? "💣" : value === 0 ? "" : value}
        </div>
      </td>
    );
  }

  return (
    <td
      // Click FALLBACK: cattura solo i click sulla cornice 1px del
      // border-collapse (target === currentTarget). Clic dentro il button
      // è gestito dal button stesso e fermato dalla stopPropagation().
      onClick={(e) => {
        if (e.target === e.currentTarget && !flagged) onClick?.();
      }}
      onContextMenu={(e) => {
        e.preventDefault();
        e.stopPropagation();
        onRightClick?.(e);
      }}
    >
      <button
        type="button"
        className="cell-btn"
        onClick={(e) => {
          e.stopPropagation(); // non re-triggera il fallback del <td>
          if (!flagged) onClick?.();
        }}
        // disabled solo sulla bandierina: la '?' accetta il left-click.
        disabled={!!flagged}
        tabIndex={0}
        aria-label={
          flagged
            ? "Cella segnata con bandierina"
            : questionMark
              ? "Cella marcata come dubbia, Invio o Spazio per scoprire"
              : "Cella coperta, Invio o Spazio per scoprire"
        }
      >
        {flagged ? "🏴" : questionMark ? "?" : ""}
      </button>
    </td>
  );
}

export default Cell;
