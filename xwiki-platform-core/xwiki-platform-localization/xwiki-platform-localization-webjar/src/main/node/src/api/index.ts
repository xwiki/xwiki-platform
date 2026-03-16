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
type Query =
  | string[]
  | {
      // the prefix can be undefined for legacy support
      prefix: string | undefined;
      keys: string[];
    };
type Translator = {
  resolve(query: Query): Promise<Translations>;
};

type Translations = { [key: string]: string };
type TranslationsWithMissed = {
  translations: Translations;
  missed?: string[];
};

type Resolver = {
  resolve(query: Query): Promise<TranslationsWithMissed>;
};

// Global resolver loaded on first call to initialize.
// To be replaced with a component based approach.s
let resolver: undefined | Resolver = undefined;

function initialize(...translators: Translator[]): Resolver {
  const newResolver = {
    async resolve(query: Query): Promise<TranslationsWithMissed> {
      let remainingTranslations = Array.isArray(query)
        ? query
        : query.keys.map((key) => (query.prefix ?? "") + key);

      let resolved = {};
      // TODO: find out how to handle cache (inside each translator)
      for (const translator of translators) {
        resolved = Object.assign(
          resolved,
          await translator.resolve(remainingTranslations),
        );
        remainingTranslations = remainingTranslations.filter(
          (translation) => !Object.keys(resolved).includes(translation),
        );
        if (remainingTranslations.length == 0) {
          break;
        }
      }

      return {
        translations: resolved,
        missed: remainingTranslations,
      };
    },
  };
  if (!resolver) {
    resolver = newResolver;
  }
  return resolver;
}

export { initialize, resolver };

export type {
  Query,
  Resolver,
  Translations,
  TranslationsWithMissed,
  Translator,
};
