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
import type {
  TranslationQuery,
  Translations,
} from "@xwiki/platform-livedata-api";
import type { I18n } from "vue-i18n";

function buildRequest(
  translationsURL: string,
  locale: string,
  prefix: string,
  keys: string[],
) {
  const usp = new URLSearchParams({
    locale: locale,
    prefix: prefix,
  });
  for (const key of keys) {
    usp.append("key", key);
  }
  return `${translationsURL}?${usp.toString()}`;
}

async function getTranslations(
  locale: string,
  prefix: string,
  keys: string[],
): Promise<Translations> {
  const translationsURL = `${XWiki.contextPath}/rest/wikis/${encodeURIComponent(
    XWiki.currentWiki,
  )}/localization/translations`;
  const input = buildRequest(translationsURL, locale, prefix, keys);

  const res = await fetch(input, {
    headers: {
      Accept: "application/json",
    },
  });

  const translations = (await res.json()).translations;
  const resMap: { [key: string]: string } = {};
  for (const value of translations) {
    resMap[value.key] = value.rawSource;
  }
  return resMap;
}

/**
 * Build the translation resolver.
 * @param locale - the current locale
 * @param i18n - the i18n instance to populate
 * @since 18.0.0RC1
 * @beta
 */
function buildTranslations(locale: string, i18n: I18n) {
  return async function resolveTranslations(
    query: TranslationQuery,
  ): Promise<Translations> {
    const translations = await getTranslations(
      locale,
      query.prefix,
      query.keys,
    );
    i18n.global.setLocaleMessage(locale, translations);
    return translations;
  };
}

export { buildTranslations };
