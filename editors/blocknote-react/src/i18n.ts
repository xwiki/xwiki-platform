import translations from "./translations";
import i18n from "i18next";
import { initReactI18next } from "react-i18next";

i18n.use(initReactI18next).init({
  // This will be overwritten by the App component
  lng: "en",

  // The resources object requires languages to be in the form of "{ en: { translation: { ...translation keys... } }"
  // so we transform them automatically here
  resources: Object.fromEntries(
    Object.entries(translations).map(([lang, translations]) => [
      lang,
      { translation: translations },
    ]),
  ),
});

export default i18n;
