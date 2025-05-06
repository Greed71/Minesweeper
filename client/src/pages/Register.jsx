import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

function Register() {
  const navigate = useNavigate();
  const backendUrl = process.env.REACT_APP_BACKEND_URL;
  const [form, setForm] = useState({
    mail: "",
    username: "",
    password: "",
    confirmPassword: ""
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
        // Login
        const response = await axios.post(`${backendUrl}/auth/login`, {
          mail: form.mail,
          password: form.password
        }, { withCredentials: true });
        localStorage.setItem("loggedUser", JSON.stringify(response.data));
        navigate("/");
      } else {
        // Registration
        if (form.password !== form.confirmPassword) {
          setError("The passwords don't match.");
          return;
        }
        const response = await axios.post(`${backendUrl}/auth/register`, {
          mail: form.mail,
          username: form.username,
          password: form.password
        }, { withCredentials: true });
        localStorage.setItem("loggedUser", JSON.stringify(response.data));
        setMessage("Registration successful!");
        navigate("/");
      }
    } catch (error) {
      console.error("Error during authentication:", error);
      setError("Invalid credentials or network error.");
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginTop: "100px" }}>
      <h1>{isLogin ? "Sign in" : "Sign up"}</h1>
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
            placeholder="Confirm Password"
            value={form.confirmPassword}
            onChange={handleChange}
            required
            style={{ padding: "10px", fontSize: "16px" }}
          />
        )}
        {error && <div style={{ color: "red", fontSize: "14px" }}>{error}</div>}
        {message && <div style={{ color: "green", fontSize: "14px" }}>{message}</div>}
        <button
          type="submit"
          style={{ padding: "10px", fontSize: "16px", backgroundColor: "#000", color: "#fff", border: "none" }}
          disabled={!form.mail || !form.password || (!isLogin && (!form.username || !form.confirmPassword))}
        >
          {isLogin ? "Sign in" : "Sign up"}
        </button>
      </form>
      <button
        onClick={() => setIsLogin(!isLogin)}
        style={{ marginTop: "20px", background: "none", border: "none", color: "blue", textDecoration: "underline", cursor: "pointer" }}
      >
        {isLogin ? "Don't have an account? Register" : "Do you already have an account? Sign in"}
      </button>
    </div>
  );
}

export default Register;
