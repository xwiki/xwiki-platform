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
import { resolver } from "../api";
import type { Query } from "../api";

function transformTranslation(
  key: string | null,
  ...args: unknown[]
): string | null {
  if (typeof key === "string") {
    let transformedKey = key;
    // If there's an argument, ensure to unescape doubled single quotes.
    if (args.length > 0) {
      transformedKey = transformedKey.replaceAll("''", "'");
    }
    // Naive implementation for message parameter substitution that suits our current needs.
    for (let i = 0; i < args.length; i++) {
      transformedKey = transformedKey.replace(
        new RegExp("\\{" + (i - 1) + "\\}", "g"),
        `${args[i]}`,
      );
    }
    return transformedKey;
  } else {
    return key;
  }
}

const toTranslationsMap = function (prefix, responseJSON) {
  const translationsMap = {};
  responseJSON.translations?.forEach(
    // Remove the prefix when adding the translation key to the translations map.
    (translation) =>
      (translationsMap[translation.key.substring(prefix.length)] =
        translation.rawSource),
  );
  return translationsMap;
};

define("xwiki-l10n", ["module"], function () {
  return {
    load(
      name: string,
      parentRequire: (
        names: string[],
        callback: (specs: Query) => void,
      ) => void,
      onLoad: (callback: unknown) => void,
    ) {
      parentRequire([name], function (specs: Query) {
        resolver?.resolve(specs).then((resolvedTranslations) => {
          // TODO: see how to integration "toTranslationsMap" and in particular
          // - the handling of raw source
          // - the handling of removing the prefix from the response (module consumers use the short version of the
          // keys when accessing them).
          onLoad({
            ...resolvedTranslations.translations,
            get(key: string, ...args: string[]): string | null {
              return transformTranslation(
                resolvedTranslations?.translations[key] ?? null,
                ...args,
              );
            },
          });
          return resolvedTranslations;
        });
      });
    },
  };
});
