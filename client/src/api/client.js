import axios from "axios";
import { getBackendUrl } from "../config.js";

// Client condiviso: intercetta 401 e prova refresh automatico.
// Se il refresh fallisce, pulisce localStorage e reindirizza al login.
const client = axios.create();

let isRefreshing = false;
let pendingRequests = [];

function getRefreshToken() {
  try {
    return localStorage.getItem("refreshToken");
  } catch {
    return null;
  }
}

function clearAuth() {
  try { localStorage.removeItem("token"); } catch { /* noop */ }
  try { localStorage.removeItem("refreshToken"); } catch { /* noop */ }
  try { localStorage.removeItem("loggedUser"); } catch { /* noop */ }
}

function onRefreshSuccess(newToken, newRefreshToken) {
  try { localStorage.setItem("token", newToken); } catch { /* noop */ }
  try { localStorage.setItem("refreshToken", newRefreshToken); } catch { /* noop */ }
  pendingRequests.forEach((cb) => cb(newToken));
  pendingRequests = [];
}

function onRefreshFailure() {
  pendingRequests.forEach((cb) => cb(null));
  pendingRequests = [];
  clearAuth();
  if (globalThis.location?.pathname !== "/register") {
    globalThis.location?.assign("/register");
  }
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }
    if (!error.config?.headers?.Authorization) {
      return Promise.reject(error);
    }
    const url = error.config?.url ?? "";
    if (url.includes("/auth/refresh") ||
        url.includes("/auth/login") ||
        url.includes("/auth/register") ||
        url.includes("/auth/change-password")) {
      return Promise.reject(error);
    }

    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      clearAuth();
      if (!url.includes("/auth/login") && !url.includes("/auth/register")) {
        globalThis.location?.assign("/register");
      }
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve) => {
        pendingRequests.push((newToken) => {
          if (newToken) {
            error.config.headers.Authorization = `Bearer ${newToken}`;
            resolve(client(error.config));
          } else {
            resolve(Promise.reject(error));
          }
        });
      });
    }

    isRefreshing = true;
    try {
      const backendUrl = getBackendUrl();
      const res = await axios.post(`${backendUrl}/auth/refresh`, {
        refreshToken,
      });
      const { token: newToken, refreshToken: newRefreshToken } = res.data;
      onRefreshSuccess(newToken, newRefreshToken);
      error.config.headers.Authorization = `Bearer ${newToken}`;
      isRefreshing = false;
      return client(error.config);
    } catch {
      isRefreshing = false;
      onRefreshFailure();
      return Promise.reject(error);
    }
  }
);

export default client;
