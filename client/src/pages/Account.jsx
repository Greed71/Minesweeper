import React, { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import client from "../api/client.js";
import { devWarn } from "../devLog.js";
import { getBackendUrl } from "../config.js";
import { useTranslation } from "react-i18next";

function Account() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [user, setUser] = useState(null);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [scores, setScores] = useState([]);
  const [message, setMessage] = useState("");
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const backendUrl = getBackendUrl();
  const token = localStorage.getItem("token");

  useEffect(() => {
    const storedUser = localStorage.getItem("loggedUser");
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  useEffect(() => {
    if (user && token) {
      client
        .get(`${backendUrl}/score/user`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        .then((response) => setScores(response.data))
        .catch((error) =>
          devWarn("I tuoi punteggi non sono arrivati, riprova.", error)
        );
    }
  }, [user, token, backendUrl]);

  const handleLogout = async () => {
    const token = localStorage.getItem("token");
    // Il backend revoca tutti i refresh token dell'utente lato DB quando riceve
    // un POST /auth/logout con bearer valido (vedi AuthService.logout). Qui basta
    // rimuoverli dal localStorage per evitare riusi dopo il re-login.
    if (token) {
      try {
        await client.post(`${backendUrl}/auth/logout`, null, {
          headers: { Authorization: `Bearer ${token}` },
        });
      } catch { /* logout best-effort: anche se la chiamata fallisce, puliamo locale */ }
    }
    try { localStorage.removeItem("loggedUser"); } catch { /* storage non disponibile */ }
    try { localStorage.removeItem("token"); } catch { /* storage non disponibile */ }
    try { localStorage.removeItem("refreshToken"); } catch { /* storage non disponibile */ }
    navigate("/");
    window.location.reload();
  };

  const handlePasswordChange = async () => {
    if (!token) {
      setMessage(t("account.notAuthenticated"));
      return;
    }

    try {
      await client.put(
        `${backendUrl}/auth/change-password`,
        {
          currentPassword: currentPassword,
          newPassword: newPassword,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      setMessage(t("account.passwordUpdated"));
      setCurrentPassword("");
      setNewPassword("");
    } catch (error) {
      devWarn("Cambio password bloccato (rete o dati errati).", error);
      setMessage(t("account.passwordUpdateError"));
    }
  };

  if (!user) {
    return (
      <main className="page page--auth">
        <div className="empty-state">
          <h2>{t("account.notLoggedIn")}</h2>
          <p>
            <Link to="/register">{t("account.goToLogin")}</Link>
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className="page page--auth">
      <div className="card account-block">
        <h1 className="card__title">{t("account.title")}</h1>
        <p className="card__hint">{t("account.subtitle")}</p>

        <div className="account-meta">
          <p>
            <strong>{t("account.email")}</strong> {user.mail}
          </p>
          <p>
            <strong>{t("account.username")}</strong> {user.username}
          </p>
        </div>

        {scores.length > 0 && (
          <div className="leaderboard" style={{ marginTop: "0.5rem" }}>
            <h2>{t("account.records")}</h2>
            <ul>
              {scores.map((s) => (
                <li key={s.id ?? `${s.difficulty}-${s.points}`}>
                  <span className="name">{t(`difficulty.${s.difficulty}`)}</span>
                  <span className="pts">{s.points}s</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div
          className="form-stack"
          style={{ marginTop: "1.25rem", width: "100%" }}
        >
          <button
            type="button"
            className="btn btn--secondary"
            onClick={() => setShowPasswordForm(!showPasswordForm)}
          >
            {showPasswordForm ? t("account.close") : t("account.changePassword")}
          </button>

          {showPasswordForm && (
            <>
              <input
                className="input"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder={t("account.currentPassword")}
                autoComplete="current-password"
              />
              <input
                className="input"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder={t("account.newPassword")}
                autoComplete="new-password"
              />
              <button
                type="button"
                className="btn btn--primary btn--block"
                onClick={handlePasswordChange}
              >
                {t("account.savePassword")}
              </button>
            </>
          )}

          {message && <p className="msg-success">{message}</p>}
        </div>

        <button
          type="button"
          className="btn btn--ghost btn--block"
          style={{ marginTop: "1.5rem" }}
          onClick={handleLogout}
        >
          {t("account.logout")}
        </button>
      </div>
    </main>
  );
}

export default Account;
