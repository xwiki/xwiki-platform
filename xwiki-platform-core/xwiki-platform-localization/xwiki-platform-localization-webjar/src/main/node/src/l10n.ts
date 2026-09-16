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

import { resolver } from "./index";
import { transformTranslation } from "./internal/transformTranslation";
import type {
  Query,
  Translations,
  TranslationsWithMissed,
} from "@xwiki/platform-localization-api";

/**
 * The expected signature of the define callback with a load as it is missing from the requirejs type definitions.
 */
export type DefineWithLoad = () => {
  load: (
    name: string,
    parentRequire: (names: string[], callback: (specs: Query) => void) => void,
    onLoad: (callback: unknown) => void,
  ) => void;
};

define("xwiki-l10n", (() => {
  return {
    load(name, parentRequire, onLoad) {
      parentRequire([name], (query) => {
        const queryPrefix: string =
          (Array.isArray(query) ? undefined : query.prefix) ?? "";
        const resolvedKeys: string[] = Array.isArray(query)
          ? query
          : query.keys.map((k) => queryPrefix + k);
        /**
         * Build the module returned to the dependent modules.
         * @param translations - the resolved translations, keyed by full translation key
         */
        function buildModule(translations: Translations) {
          // Remove the prefix when returning the translations for the current query.
          const normalizedTranslations = Object.entries(translations).reduce<{
            [key: string]: string;
          }>((acc, [k, v]) => {
            // We only return the keys from the query.
            if (resolvedKeys.includes(k)) {
              acc[k.substring(queryPrefix.length)] = v;
            }
            return acc;
          }, {});
          return {
            ...normalizedTranslations,
            get(key: string, ...args: string[]): string | null {
              return transformTranslation(
                normalizedTranslations[key] ?? null,
                ...args,
              );
            },
          };
        }

        resolver
          .resolve(query)
          .then(
            (resolvedTranslations: TranslationsWithMissed) =>
              buildModule(resolvedTranslations.translations),
            (err: unknown) => {
              console.error(
                `An issue occurred during the resolution of localization query ${JSON.stringify(query)}`,
                err,
              );
              // Degrade to the untranslated keys so that the dependent modules still initialize and merely display
              // the keys instead of their translations.
              return buildModule(
                Object.fromEntries(
                  resolvedKeys.map((key) => [
                    key,
                    key.substring(queryPrefix.length),
                  ]),
                ),
              );
            },
          )
          // onLoad is called outside of the handlers above so that an error thrown by a dependent module is not
          // reported as a translation resolution failure.
          .then(onLoad)
          .catch((err: unknown) => {
            console.error(
              `An issue occurred while initializing a module depending on the localization query ${JSON.stringify(query)}`,
              err,
            );
          });
      });
    },
  };
}) satisfies DefineWithLoad);
