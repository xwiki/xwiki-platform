/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
import translations from "./translations";
import { default as i18n } from "i18next";
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
