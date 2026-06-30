import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import client from "../api/client.js";
import { devWarn } from "../devLog.js";
import { getBackendUrl } from "../config.js";
import { useTranslation } from "react-i18next";

function Register() {
  const navigate = useNavigate();
  const backendUrl = getBackendUrl();
  const { t } = useTranslation();
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
    const disallowedChars = /[*'\"`;\\]/g;
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
        const { token, refreshToken, user } = response.data;
        if (token) {
          try { localStorage.setItem("token", token); } catch { /* storage non disponibile */ }
        }
        if (refreshToken) {
          try { localStorage.setItem("refreshToken", refreshToken); } catch { /* noop */ }
        }
        if (user) {
          try { localStorage.setItem("loggedUser", JSON.stringify(user)); } catch { /* storage non disponibile */ }
        }
        navigate("/");
      } else {
        if (form.password !== form.confirmPassword) {
          setError(t("auth.passwordMismatch"));
          return;
        }
        const response = await client.post(`${backendUrl}/auth/register`, {
          mail: form.mail,
          username: form.username,
          password: form.password,
        });
        const { token, refreshToken, user } = response.data;
        if (token) {
          try { localStorage.setItem("token", token); } catch { /* storage non disponibile */ }
        }
        if (refreshToken) {
          try { localStorage.setItem("refreshToken", refreshToken); } catch { /* noop */ }
        }
        if (user) {
          try { localStorage.setItem("loggedUser", JSON.stringify(user)); } catch { /* storage non disponibile */ }
        }
        setMessage(t("auth.registrationSuccess"));
        navigate("/");
      }
    } catch (error) {
      devWarn(
        "Accesso non riuscito: controlla credenziali o connessione.",
        error
      );
      setError(t("auth.credentialsError"));
    }
  };

  return (
    <main className="page page--auth">
      <div className="card">
        <h1 className="card__title">
          {isLogin ? t("auth.login") : t("auth.register")}
        </h1>
        <p className="card__hint">
          {isLogin ? t("auth.loginHint") : t("auth.registerHint")}
        </p>
        <form className="form-stack" onSubmit={handleSubmit}>
          <input
            className="input"
            type="email"
            name="mail"
            placeholder={t("auth.email")}
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
              placeholder={t("auth.username")}
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
            placeholder={t("auth.password")}
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
              placeholder={t("auth.confirmPassword")}
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
            {isLogin ? t("auth.loginButton") : t("auth.registerButton")}
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
            ? t("auth.switchToRegister")
            : t("auth.switchToLogin")}
        </button>
      </div>
    </main>
  );
}

export default Register;
