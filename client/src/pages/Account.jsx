import React, { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import client from "../api/client.js";
import { devWarn } from "../devLog.js";
import { getBackendUrl } from "../config.js";

const diffLabel = (d) => {
  const m = { easy: "Facile", medium: "Medio", hard: "Difficile" };
  return m[d] ?? d;
};

function Account() {
  const navigate = useNavigate();
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

  const handleLogout = () => {
    localStorage.removeItem("loggedUser");
    localStorage.removeItem("token");
    navigate("/");
    window.location.reload();
  };

  const handlePasswordChange = async () => {
    if (!token) {
      setMessage("Non sei autenticato.");
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

      setMessage("Password aggiornata con successo.");
      setCurrentPassword("");
      setNewPassword("");
    } catch (error) {
      devWarn("Cambio password bloccato (rete o dati errati).", error);
      setMessage("Errore durante l'aggiornamento della password.");
    }
  };

  if (!user) {
    return (
      <main className="page page--auth">
        <div className="empty-state">
          <h2>Accesso non effettuato</h2>
          <p>
            <Link to="/register">Vai al login o alla registrazione</Link>
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className="page page--auth">
      <div className="card account-block">
        <h1 className="card__title">Profilo</h1>
        <p className="card__hint">Dati account e punteggi salvati</p>

        <div className="account-meta">
          <p>
            <strong>Email</strong> {user.mail}
          </p>
          <p>
            <strong>Username</strong> {user.username}
          </p>
        </div>

        {scores.length > 0 && (
          <div className="leaderboard" style={{ marginTop: "0.5rem" }}>
            <h2>I tuoi record</h2>
            <ul>
              {scores.map((s) => (
                <li key={s.id ?? `${s.difficulty}-${s.points}`}>
                  <span className="name">{diffLabel(s.difficulty)}</span>
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
            {showPasswordForm ? "Chiudi" : "Cambia password"}
          </button>

          {showPasswordForm && (
            <>
              <input
                className="input"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="Password attuale"
                autoComplete="current-password"
              />
              <input
                className="input"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Nuova password"
                autoComplete="new-password"
              />
              <button
                type="button"
                className="btn btn--primary btn--block"
                onClick={handlePasswordChange}
              >
                Salva nuova password
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
          Esci
        </button>
      </div>
    </main>
  );
}

export default Account;
