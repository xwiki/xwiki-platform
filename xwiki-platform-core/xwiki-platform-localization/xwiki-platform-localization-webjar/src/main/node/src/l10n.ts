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
import { initialize } from "@xwiki/platform-localization-default";
import { translatorFactory as translatorFactoryDom } from "@xwiki/platform-localization-resolver-dom";
import { translatorFactory as translatorFactoryXWikiRest } from "@xwiki/platform-localization-resolver-xwiki-rest";
import type {
  Query,
  TranslationsWithMissed,
} from "@xwiki/platform-localization-api";

XWiki.localization = XWiki.localization ?? {};

XWiki.localization.resolver = initialize(
  // TODO: introduce a mechanism to initialize translations in a hidden div element.
  translatorFactoryDom("translations"),
  translatorFactoryXWikiRest(
    `${XWiki.contextPath}/rest/wikis/${encodeURIComponent(XWiki.currentWiki)}/localization/translations`,
  ),
);

function transformTranslation(
  value: string | null,
  ...args: unknown[]
): string | null {
  if (typeof value === "string") {
    let transformedKey = value;
    // If there's an argument, ensure to unescape doubled single quotes.
    if (args.length > 0) {
      transformedKey = transformedKey.replaceAll("''", "'");
    }
    // Naive implementation for message parameter substitution that suits our current needs.
    for (let i = 0; i < args.length; i++) {
      transformedKey = transformedKey.replace(
        new RegExp(`\\{${i}\\}`, "g"),
        `${args[i]}`,
      );
    }
    return transformedKey;
  } else {
    return value;
  }
}

define("xwiki-l10n", ["module"], () => ({
  load(
    name: string,
    parentRequire: (names: string[], callback: (specs: Query) => void) => void,
    onLoad: (callback: unknown) => void,
  ) {
    parentRequire([name], (query: Query) => {
      const queryPrefix: string =
        (Array.isArray(query) ? undefined : query.prefix) ?? "";
      const resolvedKeys: string[] = Array.isArray(query)
        ? query
        : query.keys.map((k) => queryPrefix + k);
      XWiki.localization?.resolver
        ?.resolve(query)
        .then((resolvedTranslations: TranslationsWithMissed) => {
          const translations = resolvedTranslations.translations;
          onLoad({
            ...translations,
            get(key: string, ...args: string[]): string | null {
              return transformTranslation(
                translations[queryPrefix + key] ?? null,
                ...args,
              );
            },
          });
          // Remove the prefix when returning the translations for the current query.
          return Object.entries(translations).reduce<{
            [key: string]: string;
          }>((acc, [k, v]) => {
            // We only return the keys from the query.
            if (resolvedKeys.includes(k)) {
              acc[k.substring(queryPrefix.length)] = v;
            }
            return acc;
          }, {});
        });
    });
  },
}));
