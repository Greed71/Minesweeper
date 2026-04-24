import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import client from "../api/client.js";
import { devWarn } from "../devLog.js";
import { getBackendUrl } from "../config.js";

function Register() {
  const navigate = useNavigate();
  const backendUrl = getBackendUrl();
  const [form, setForm] = useState({
    mail: "",
    username: "",
    password: "",
    confirmPassword: "",
  });
  const [isLogin, setIsLogin] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const handleChange = (e) => {
    const { name, value } = e.target;
    const disallowedChars = /[*'"`;\\]/g;
    const sanitizedValue =
      name === "password" || name === "confirmPassword"
        ? value.replace(disallowedChars, "")
        : value;
    setForm({ ...form, [name]: sanitizedValue });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");
    try {
      if (isLogin) {
        const response = await client.post(`${backendUrl}/auth/login`, {
          mail: form.mail,
          password: form.password,
        });
        const { token, user } = response.data;
        if (token) {
          localStorage.setItem("token", token);
        }
        if (user) {
          localStorage.setItem("loggedUser", JSON.stringify(user));
        }
        navigate("/");
      } else {
        if (form.password !== form.confirmPassword) {
          setError("The passwords don't match.");
          return;
        }
        const response = await client.post(`${backendUrl}/auth/register`, {
          mail: form.mail,
          username: form.username,
          password: form.password,
        });
        const { token, user } = response.data;
        if (token) {
          localStorage.setItem("token", token);
        }
        if (user) {
          localStorage.setItem("loggedUser", JSON.stringify(user));
        }
        setMessage("Registration successful!");
        navigate("/");
      }
    } catch (error) {
      devWarn(
        "Accesso non riuscito: controlla credenziali o connessione.",
        error
      );
      setError("Invalid credentials or network error.");
    }
  };

  return (
    <main className="page page--auth">
      <div className="card">
        <h1 className="card__title">
          {isLogin ? "Accedi" : "Crea un account"}
        </h1>
        <p className="card__hint">
          {isLogin
            ? "Bentornato: inserisci email e password."
            : "Registrati per salvare i punteggi in classifica."}
        </p>
        <form className="form-stack" onSubmit={handleSubmit}>
          <input
            className="input"
            type="email"
            name="mail"
            placeholder="Email"
            value={form.mail}
            onChange={handleChange}
            required
            autoComplete="email"
          />
          {!isLogin && (
            <input
              className="input"
              type="text"
              name="username"
              placeholder="Username"
              value={form.username}
              onChange={handleChange}
              required
              autoComplete="username"
            />
          )}
          <input
            className="input"
            type="password"
            name="password"
            placeholder="Password"
            value={form.password}
            onChange={handleChange}
            required
            autoComplete={isLogin ? "current-password" : "new-password"}
          />
          {!isLogin && (
            <input
              className="input"
              type="password"
              name="confirmPassword"
              placeholder="Conferma password"
              value={form.confirmPassword}
              onChange={handleChange}
              required
              autoComplete="new-password"
            />
          )}
          {error && <div className="msg-error">{error}</div>}
          {message && <div className="msg-success">{message}</div>}
          <button
            type="submit"
            className="btn btn--primary btn--block"
            disabled={
              !form.mail ||
              !form.password ||
              (!isLogin &&
                (!form.username || !form.confirmPassword))
            }
          >
            {isLogin ? "Entra" : "Registrati"}
          </button>
        </form>
        <button
          type="button"
          className="text-link"
          onClick={() => {
            setIsLogin(!isLogin);
            setError("");
            setMessage("");
          }}
        >
          {isLogin
            ? "Non hai un account? Registrati"
            : "Hai già un account? Accedi"}
        </button>
      </div>
    </main>
  );
}

export default Register;
