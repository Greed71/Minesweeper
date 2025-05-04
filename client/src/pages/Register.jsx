// pages/Register.jsx
import React, { useState } from "react";
import axios from "axios";

function Register() {
  const [form, setForm] = useState({
    mail: "",
    username: "",
    password: "",
    confirmPassword: ""
  });

  const [isLogin, setIsLogin] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      if (isLogin) {
        // Login
        const response = await axios.post("http://localhost:8080/auth/login", {
          mail: form.mail,
          password: form.password
        });
        localStorage.setItem("loggedUser", JSON.stringify(response.data));
        console.log("Login effettuato:", response.data);
        window.location.href = "/";
      } else {
        // Registrazione
        if (form.password !== form.confirmPassword) {
          setError("Le password non coincidono.");
          return;
        }
        const response = await axios.post("http://localhost:8080/auth/register", {
          mail: form.mail,
          username: form.username,
          password: form.password
        });
        localStorage.setItem("loggedUser", JSON.stringify(response.data));
        console.log("Registrazione completata:", response.data);
        window.location.href = "/";
      }
    } catch (error) {
      console.error("Errore durante l'autenticazione:", error);
      setError("Credenziali non valide o errore di rete.");
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginTop: "100px" }}>
      <h1 style={{ fontSize: "2rem", marginBottom: "20px" }}>{isLogin ? "Login" : "Registrazione"}</h1>
      <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "15px", width: "300px" }}>
        <input
          type="email"
          name="mail"
          placeholder="Email"
          value={form.mail}
          onChange={handleChange}
          required
          style={{ padding: "10px", fontSize: "16px" }}
        />
        {!isLogin && (
          <input
            type="text"
            name="username"
            placeholder="Username"
            value={form.username}
            onChange={handleChange}
            required
            style={{ padding: "10px", fontSize: "16px" }}
          />
        )}
        <input
          type="password"
          name="password"
          placeholder="Password"
          value={form.password}
          onChange={handleChange}
          required
          style={{ padding: "10px", fontSize: "16px" }}
        />
        {!isLogin && (
          <input
            type="password"
            name="confirmPassword"
            placeholder="Conferma Password"
            value={form.confirmPassword}
            onChange={handleChange}
            required
            style={{ padding: "10px", fontSize: "16px" }}
          />
        )}
        {error && <div style={{ color: "red", fontSize: "14px" }}>{error}</div>}
        <button type="submit" style={{ padding: "10px", fontSize: "16px", backgroundColor: "#000", color: "#fff", border: "none" }}>
          {isLogin ? "Accedi" : "Registrati"}
        </button>
      </form>
      <button
        onClick={() => setIsLogin(!isLogin)}
        style={{ marginTop: "20px", background: "none", border: "none", color: "blue", textDecoration: "underline", cursor: "pointer" }}
      >
        {isLogin ? "Non hai un account? Registrati" : "Hai già un account? Accedi"}
      </button>
    </div>
  );
}

export default Register;
