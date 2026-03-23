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
        resolver
          .resolve(query)
          .then((resolvedTranslations: TranslationsWithMissed) => {
            const translations = resolvedTranslations.translations;
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
            onLoad({
              ...normalizedTranslations,
              get(key: string, ...args: string[]): string | null {
                return transformTranslation(
                  normalizedTranslations[key] ?? null,
                  ...args,
                );
              },
            });

            return normalizedTranslations;
          })
          .catch((err: unknown) => {
            console.error(
              `An issue occurred during the resolution of localization query ${query}`,
              err,
            );
          });
      });
    },
  };
}) satisfies DefineWithLoad);
