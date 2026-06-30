import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

import it from "./locales/it.json";
import fr from "./locales/fr.json";
import es from "./locales/es.json";
import de from "./locales/de.json";

const STORAGE_KEY = "minesweeper-lang";

function getStoredLang() {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function storeLang(lng) {
  try {
    localStorage.setItem(STORAGE_KEY, lng);
  } catch {
    /* noop */
  }
}

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      it: { translation: it },
      fr: { translation: fr },
      es: { translation: es },
      de: { translation: de },
    },
    fallbackLng: "it",
    lng: getStoredLang() ?? "it",
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ["localStorage", "navigator"],
      caches: [],
      lookupLocalStorage: STORAGE_KEY,
      lookupFromPathIndex: 0,
    },
  });

i18n.on("languageChanged", (lng) => {
  storeLang(lng);
  document.documentElement.lang = lng;
});

export default i18n;
