import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import "./i18n/i18n.js";
import "./index.css";
import "./App.css";
import AppRouter from "./Router";

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AppRouter />
  </StrictMode>,
)
