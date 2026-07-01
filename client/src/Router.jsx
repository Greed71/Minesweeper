// Barra in alto: "Minesweeper" azzera la partita, a destra login o profilo + selettore lingua.
import { useEffect, useState } from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Link,
  useLocation,
} from "react-router-dom";
import { useTranslation } from "react-i18next";
import Home from "./pages/Home";
import Register from "./pages/Register";
import Account from "./pages/Account";

const LANGUAGES = [
  { code: "it", label: "IT" },
  { code: "fr", label: "FR" },
  { code: "es", label: "ES" },
  { code: "de", label: "DE" },
];

function AppShell() {
  const { t, i18n } = useTranslation();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [resetCounter, setResetCounter] = useState(0);
  const location = useLocation();

  useEffect(() => {
    try {
      setIsLoggedIn(!!localStorage.getItem("loggedUser"));
    } catch {
      setIsLoggedIn(false);
    }
  }, [location.pathname]);

  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
  };

  return (
    <div className="app-shell">
      <header className="site-header">
        <Link
          to="/"
          className="brand"
          onClick={() => setResetCounter((c) => c + 1)}
        >
          <span className="brand__mark" aria-hidden>
            <img
              src="/mine.svg"
              alt=""
              className="brand__mark-img"
            />
          </span>
          Minesweeper
        </Link>

        <nav style={{ display: "flex", alignItems: "center", gap: "0.4rem" }}>
          <div className="lang-switcher" role="group" aria-label="Selettore lingua">
            {LANGUAGES.map(({ code, label }) => (
              <button
                key={code}
                type="button"
                className={`lang-btn${i18n.language === code ? " lang-btn--active" : ""}`}
                onClick={() => changeLanguage(code)}
                title={code}
              >
                {label}
              </button>
            ))}
          </div>

          {isLoggedIn ? (
            <Link to="/account" className="nav-link" title={t("nav.profile")}>
              {t("nav.profile")}
            </Link>
          ) : (
            <Link to="/register" className="nav-link">
              {t("nav.loginRegister")}
            </Link>
          )}
        </nav>
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
