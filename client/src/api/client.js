import axios from "axios";

// Client condiviso: se il server risponde 401 con un token inviato, togliamo
// l’utente da localStorage e (se serve) lo mandiamo al login, senza pasticciare
// con password sbagliate al sign-in o alla vecchia password.
const client = axios.create();

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }
    if (!error.config?.headers?.Authorization) {
      return Promise.reject(error);
    }
    const url = error.config?.url ?? "";
    if (url.includes("/auth/change-password")) {
      return Promise.reject(error);
    }
    try {
      localStorage.removeItem("token");
      localStorage.removeItem("loggedUser");
    } catch {
      /* storage non disponibile, es. contesto raro */
    }
    if (!url.includes("/auth/login") && !url.includes("/auth/register")) {
      globalThis.location?.assign("/register");
    }
    return Promise.reject(error);
  }
);

export default client;
