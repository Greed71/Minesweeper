/** Base URL API in sviluppo (Vite: `.env.development` / `.env.local`) o produzione. */
export function getBackendUrl() {
  return import.meta.env.VITE_APP_BACKEND_URL ?? "http://localhost:8080";
}
