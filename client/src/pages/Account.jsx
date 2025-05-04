// pages/Account.jsx
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

function Account() {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [newPassword, setNewPassword] = useState("");
  const [scores, setScores] = useState([]);
  const [message, setMessage] = useState("");
  const [showPasswordForm, setShowPasswordForm] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem("loggedUser");
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  useEffect(() => {
    if (user) {
      axios.get(`http://localhost:8080/score/user?username=${user.username}`)
        .then(response => setScores(response.data))
        .catch(error => console.error("Errore nel recupero punteggi:", error));
    }
  }, [user]);

  const handleLogout = () => {
    localStorage.removeItem("loggedUser");
    navigate("/");
    window.location.reload();
  };

  const handlePasswordChange = async () => {
    try {
      await axios.put("http://localhost:8080/auth/change-password", {
        username: user.username,
        newPassword: newPassword
      });
      setMessage("Password aggiornata con successo.");
      setNewPassword("");
    } catch (error) {
      console.error("Errore durante l'aggiornamento della password:", error);
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
