// Barra in alto: “Minesweeper” azzera la partita, a destra login o profilo.
import { useEffect, useState } from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Link,
  useLocation,
} from "react-router-dom";
import Home from "./pages/Home";
import Register from "./pages/Register";
import Account from "./pages/Account";

function AppShell() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [resetCounter, setResetCounter] = useState(0);
  const location = useLocation();

  useEffect(() => {
    setIsLoggedIn(!!localStorage.getItem("loggedUser"));
  }, [location.pathname]);

  return (
    <div className="app-shell">
      <header className="site-header">
        <Link
          to="/"
          className="brand"
          onClick={() => setResetCounter((c) => c + 1)}
        >
          <span className="brand__mark" aria-hidden>
            ⬚
          </span>
          Minesweeper
        </Link>
        {isLoggedIn ? (
          <Link to="/account" className="nav-link" title="Account">
            Profilo
          </Link>
        ) : (
          <Link to="/register" className="nav-link">
            Accedi / Registrati
          </Link>
        )}
      </header>
      <Routes>
        <Route path="/" element={<Home resetTrigger={resetCounter} />} />
        <Route path="/register" element={<Register />} />
        <Route path="/account" element={<Account />} />
      </Routes>
    </div>
  );
}

export default function AppRouter() {
  return (
    <Router>
      <AppShell />
    </Router>
  );
}
