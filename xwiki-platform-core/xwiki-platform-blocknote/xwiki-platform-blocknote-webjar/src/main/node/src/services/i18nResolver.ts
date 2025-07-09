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
import { I18n } from "vue-i18n";

declare const define: (moduleName: string, moduleDefinition: unknown) => void;
declare const require: (modules: string[], onLoad: (...args: any[]) => void, onError?: (error: Error) => void) => void;

type Config = {
  locale: string;
  prefix: string;
  keys: string[];
};

type Translation = {
  config: Config;
  messages: Record<string, string>;
};

async function fetchTranslation(): Promise<Translation> {
  define("xwiki-blocknote-translation-keys", {
    locale: document.documentElement.getAttribute("lang"),
    prefix: "blocknote.",
    keys: [],
  });
  const translation = await new Promise<Translation>((resolve, reject) => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    require(["xwiki-blocknote-translation-keys", "xwiki-l10n!xwiki-blocknote-translation-keys"], (config, messages) => {
      resolve({ config, messages });
    }, reject);
  });
  // Add back the prefix to the keys.
  const messages: Record<string, string> = {};
  for (const [key, value] of Object.entries(translation.messages)) {
    if (typeof value === "string") {
      messages[`${translation.config.prefix}${key}`] = value;
    }
  }
  return { config: translation.config, messages };
}

export async function i18nResolver(i18n: I18n): Promise<I18n> {
  try {
    const translation = await fetchTranslation();
    i18n.global.setLocaleMessage(translation.config.locale, translation.messages);
  } catch (error) {
    console.error("Failed to load translations: ", error);
  }

  return i18n;
}
