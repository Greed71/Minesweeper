/**
 * In produzione la console resta pulita; in sviluppo aiuta a capire cosa non ha funzionato.
 */
export function devWarn(message, err) {
  if (!import.meta.env.DEV) return;
  if (err !== undefined) {
    console.warn(message, err);
  } else {
    console.warn(message);
  }
}
