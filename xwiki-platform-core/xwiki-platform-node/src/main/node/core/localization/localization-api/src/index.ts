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

/**
 * A query expresses the set of translations to be resolved. It can be either an array of full translation keys, or
 * an array of translation key suffixes plus a separate shared prefix and an optional locale.
 *
 * @since 18.3.0RC1
 * @beta
 */
type Query =
  | string[]
  | {
      // the prefix can be undefined for legacy support
      prefix?: string | undefined;
      keys: string[];
      locale?: string;
    };

/**
 * A translator, take a query and returns translations. Translators are expected to be chained, the next translations
 * received a query where keys resolved by the previous translator are removed. A translator only return the
 * translations it can find, leaving the next translator of the chain resolve the unresolved ones.
 * @since 18.3.0RC1
 * @beta
 */
type Translator = {
  resolve(query: Query): Promise<Translations>;
};

/**
 * A resolved query, in the form of a map with the translation keys and their associated translation values.
 * @since 18.3.0RC1
 * @beta
 */
type Translations = { [key: string]: string };

/**
 * An object holding the translations but also the requested keys that failed to be resolved.
 * @since 18.3.0RC1
 * @beta
 */
type TranslationsWithMissed = {
  translations: Translations;
  missed?: string[];
};

/**
 * The results of a full translation resolution. Including missed translations.
 * @since 18.3.0RC1
 * @beta
 */
type Resolver = {
  resolve(query: Query): Promise<TranslationsWithMissed>;
};

export type {
  Query,
  Resolver,
  Translations,
  TranslationsWithMissed,
  Translator,
};
