import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import client from "../api/client.js";
import { devWarn } from "../devLog.js";

function Account() {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [scores, setScores] = useState([]);
  const [message, setMessage] = useState("");
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const backendUrl = import.meta.env.VITE_APP_BACKEND_URL;
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
      <div style={{ marginTop: "100px", textAlign: "center" }}>
        <h2>Non sei loggato.</h2>
        <p><a href="/register">Vai alla pagina di login</a></p>
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginTop: "100px" }}>
      <h1 style={{ fontSize: "2rem", marginBottom: "20px" }}>Account</h1>
      <p><strong>Email:</strong> {user.mail}</p>
      <p><strong>Username:</strong> {user.username}</p>

      <div style={{ marginTop: "30px", width: "300px" }}>
        <button
          onClick={() => setShowPasswordForm(!showPasswordForm)}
          style={{ padding: "10px", width: "60%", backgroundColor: "#000", color: "#fff", border: "none", marginBottom: "10px" }}
        >
          {showPasswordForm ? "Back" : "Change password"}
        </button>

        {showPasswordForm && (
          <>
            <input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="Current password"
              style={{ padding: "10px", width: "90%", marginBottom: "10px" }}
            />
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="New password"
              style={{ padding: "10px", width: "90%", marginBottom: "10px" }}
            />
            <button
              onClick={handlePasswordChange}
              style={{ padding: "10px", width: "100%", backgroundColor: "#000", color: "#fff", border: "none" }}
            >
              Change Password
            </button>
          </>
        )}

        {message && <p style={{ marginTop: "10px", color: "green" }}>{message}</p>}
      </div>

      <button
        onClick={handleLogout}
        style={{ marginTop: "40px", padding: "10px", fontSize: "16px", backgroundColor: "#000", color: "#fff", border: "none" }}
      >
        Logout
      </button>
    </div>
  );
}

export default Account;
