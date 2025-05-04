import { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import Home from "./pages/Home";
import Register from "./pages/Register";
import Account from "./pages/Account";

export default function AppRouter() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    const user = localStorage.getItem("loggedUser");
    setIsLoggedIn(!!user);
  }, []);

  return (
    <Router>
      <div style={{ position: "fixed", top: 0, right: 0, padding: "20px", zIndex: 1000 }}>
        {isLoggedIn ? (
          <Link to="/account" style={{ textDecoration: "none", fontSize: "20px" }}>👤</Link>
        ) : (
          <Link to="/register" style={{ textDecoration: "none", fontWeight: "bold" }}>
            Signup / Login
          </Link>
        )}
      </div>
      <div style={{ position: "fixed", top: 0, left: 0, padding: "20px", zIndex: 1000 }}>
        <Link to="/" style={{ textDecoration: "none", fontWeight: "bold" }}>
          Minesweeper
        </Link>
      </div>

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/register" element={<Register />} />
        <Route path="/account" element={<Account />} />
      </Routes>
    </Router>
  );
}
