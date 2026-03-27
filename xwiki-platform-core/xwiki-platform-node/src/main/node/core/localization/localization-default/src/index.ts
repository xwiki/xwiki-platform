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
  TranslationsWithMissed,
  Translator,
} from "@xwiki/platform-localization-api";

/**
 * Combines a chain of translators into a full localizations resolvers. Translators are called in a chain. Only
 * requested translations unresolved by the first translator are passed to the next one. The chaining stops either
 * when all requested translation keys, or when the chain is exhausted. Unresolved keys at the end of the chain are
 * returned as missed.
 *
 * @param translators - the list of translators to combine to build a full resolver.
 * @since 18.3.0RC1
 * @beta
 */
export function initialize(...translators: Translator[]): Resolver {
  return {
    async resolve(query: Query): Promise<TranslationsWithMissed> {
      let remainingTranslations = Array.isArray(query)
        ? query
        : query.keys.map((key) => (query.prefix ?? "") + key);

      let resolved = {};
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
}
