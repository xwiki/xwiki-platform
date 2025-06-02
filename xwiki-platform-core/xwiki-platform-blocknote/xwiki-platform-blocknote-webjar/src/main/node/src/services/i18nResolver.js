/*
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
async function fetchTranslation() {
  define("xwiki-blocknote-translation-keys", {
    locale: document.documentElement.getAttribute("lang"),
    prefix: "blocknote.",
    keys: [],
  });
  const { config, l10n } = await new Promise((resolve, reject) => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    require(["xwiki-blocknote-translation-keys", "xwiki-l10n!xwiki-blocknote-translation-keys"], (config, l10n) => {
      resolve({ config, l10n });
    }, reject);
  });
  // Add back the prefix to the keys.
  const messages = {};
  for (const [key, value] of Object.entries(l10n)) {
    if (typeof value === "string") {
      messages[`${config.prefix}${key}`] = value;
    }
  }
  return { config, messages };
}

export async function i18nResolver(i18n) {
  const translation = await fetchTranslation();
  i18n.global.setLocaleMessage(translation.config.locale, translation.messages);
}
