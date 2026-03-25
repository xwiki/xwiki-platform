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
  Query,
  Resolver,
  Translations,
} from "@xwiki/platform-localization-api";
import type { I18n } from "vue-i18n";

/**
 * @param resolver - the resolver to use when building the translation preloader
 * @since 18.3.0RC1
 * @beta
 */
function initTranslationsBuilder(
  resolver: Resolver,
): (local: string, i18n: I18n) => (query: Query) => Promise<Translations> {
  /**
   * Build the translation resolver.
   * @param locale - the current locale
   * @param i18n - the i18n instance to populate
   */
  return function buildTranslations(locale: string, i18n: I18n) {
    return async function resolveTranslations(
      query: Query,
    ): Promise<Translations> {
      const translations = (await resolver.resolve(query)).translations;
      i18n.global.setLocaleMessage(locale, translations);
      return translations;
    };
  };
}

export { initTranslationsBuilder };
